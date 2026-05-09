import os
import pickle
import numpy as np

_BASE_DIR = os.path.dirname(os.path.abspath(__file__))
from flask import Flask, request, jsonify
from flask_cors import CORS

app = Flask(__name__)
CORS(app)

# ── Load LSTM classifier (existing model 1) ───────────────────────────────────
lstm_model = None
try:
    from tensorflow import keras
    lstm_model = keras.models.load_model(os.path.join(_BASE_DIR, "model", "lstm_model.h5"))
    print("[startup] LSTM model loaded")
except Exception as e:
    print(f"[startup] LSTM model not loaded: {e}")

# ── Load NLP intent model (existing model 2) ─────────────────────────────────
nlp_vectorizer = None
nlp_classifier = None
try:
    with open(os.path.join(_BASE_DIR, "model", "nlp_vectorizer.pkl"), "rb") as f:
        nlp_vectorizer = pickle.load(f)
    with open(os.path.join(_BASE_DIR, "model", "nlp_classifier.pkl"), "rb") as f:
        nlp_classifier = pickle.load(f)
    print("[startup] NLP intent model loaded")
except Exception as e:
    print(f"[startup] NLP model not loaded: {e}")

# ── Load Adaptive Learning model (model 3 — ODD 9) ───────────────────────────
adaptive_model = None
adaptive_scaler = None
try:
    import joblib
    adaptive_model = joblib.load(os.path.join(_BASE_DIR, "model", "adaptive_model.pkl"))
    adaptive_scaler = joblib.load(os.path.join(_BASE_DIR, "model", "adaptive_scaler.pkl"))
    print("[startup] Adaptive model loaded")
except Exception as e:
    print(f"[startup] Adaptive model not loaded: {e}")


# ── Helpers ───────────────────────────────────────────────────────────────────

DIFFICULTY_LABELS = {0: "EASY", 1: "MEDIUM", 2: "HARD", 3: "BOSS"}


def _adaptive_default_response():
    return {
        "success_probability": 0.5,
        "recommended_action": "MAINTAIN",
        "timer_adjustment": 0,
        "show_hint": False,
        "difficulty_label": "MEDIUM",
        "player_level": "LEARNING",
    }


def _build_adaptive_response(prob, difficulty):
    label = DIFFICULTY_LABELS.get(int(difficulty), "MEDIUM")
    if prob < 0.4:
        return {
            "success_probability": round(prob, 4),
            "recommended_action": "ASSIST",
            "timer_adjustment": 15,
            "show_hint": True,
            "difficulty_label": label,
            "player_level": "STRUGGLING",
        }
    elif prob <= 0.7:
        return {
            "success_probability": round(prob, 4),
            "recommended_action": "MAINTAIN",
            "timer_adjustment": 0,
            "show_hint": False,
            "difficulty_label": label,
            "player_level": "LEARNING",
        }
    else:
        return {
            "success_probability": round(prob, 4),
            "recommended_action": "CHALLENGE",
            "timer_adjustment": -10,
            "show_hint": False,
            "difficulty_label": label,
            "player_level": "PROFICIENT",
        }


# ── Existing endpoints ────────────────────────────────────────────────────────

@app.route("/health", methods=["GET"])
def health():
    return jsonify({
        "status": "ok",
        "lstm_loaded": lstm_model is not None,
        "nlp_loaded": nlp_classifier is not None,
        "adaptive_loaded": adaptive_model is not None,
    })


@app.route("/predict", methods=["POST"])
def predict():
    """LSTM classifier endpoint."""
    data = request.get_json()
    if lstm_model is None:
        return jsonify({"error": "LSTM model not available"}), 503
    try:
        features = np.array(data["features"]).reshape(1, -1, 1)
        prob = float(lstm_model.predict(features)[0][0])
        return jsonify({"prediction": prob, "label": int(prob > 0.5)})
    except Exception as e:
        return jsonify({"error": str(e)}), 400


@app.route("/voice-to-command", methods=["POST"])
def voice_to_command():
    """NLP intent endpoint."""
    data = request.get_json()
    if nlp_classifier is None or nlp_vectorizer is None:
        return jsonify({"error": "NLP model not available"}), 503
    try:
        text = data.get("text", "")
        vec = nlp_vectorizer.transform([text])
        intent = nlp_classifier.predict(vec)[0]
        return jsonify({"intent": intent, "command": intent})
    except Exception as e:
        return jsonify({"error": str(e)}), 400


@app.route("/risk", methods=["POST"])
def risk():
    """Risk analysis endpoint."""
    data = request.get_json()
    score = data.get("score", 0)
    level = "LOW" if score < 30 else "MEDIUM" if score < 70 else "HIGH"
    return jsonify({"risk_level": level, "score": score})


# ── Adaptive Learning endpoint (ODD 9) ───────────────────────────────────────

@app.route("/adaptive-predict", methods=["POST"])
def adaptive_predict():
    data = request.get_json()
    if adaptive_model is None or adaptive_scaler is None:
        return jsonify(_adaptive_default_response())

    try:
        features = np.array([[
            float(data["success_rate"]),
            float(data["avg_attempts"]),
            float(data["avg_response_time"]),
            int(data["command_category"]),
            int(data["difficulty"]),
            int(data["streak"]),
        ]])
        scaled = adaptive_scaler.transform(features)
        prob = float(adaptive_model.predict_proba(scaled)[:, 1][0])
        return jsonify(_build_adaptive_response(prob, data["difficulty"]))
    except Exception as e:
        print(f"[adaptive-predict] error: {e}")
        return jsonify(_adaptive_default_response())


# ── Skill Intelligence Engine (ODD 9) ────────────────────────────────────────

skill_predictor    = None
skill_label_encoder = None
skill_benchmarks   = None

# Fallback benchmarks used when CSV is unavailable
_BENCHMARK_FALLBACK = {
    "RHCSA":           {"filesystem": 70, "network": 65, "process": 60, "security": 75, "disk": 70, "service": 65},
    "LPIC-1":          {"filesystem": 65, "network": 60, "process": 55, "security": 60, "disk": 65, "service": 55},
    "CompTIA Linux+":  {"filesystem": 60, "network": 55, "process": 50, "security": 65, "disk": 60, "service": 50},
}

try:
    _model_path      = os.path.join(_BASE_DIR, "model", "skill_predictor.pkl")
    _encoder_path    = os.path.join(_BASE_DIR, "model", "skill_label_encoder.pkl")
    _benchmarks_path = os.path.join(_BASE_DIR, "dataset", "skill_benchmarks.csv")
    skill_predictor     = joblib.load(_model_path)
    skill_label_encoder = joblib.load(_encoder_path)
    import pandas as _pd
    skill_benchmarks = _pd.read_csv(_benchmarks_path)
    print(f"[startup] Skill engine loaded. Benchmarks: {len(skill_benchmarks)} rows")
except Exception as e:
    print(f"[startup] Skill engine not fully loaded: {e}")
    print("[startup] Will use fallback benchmarks dict")


CATEGORIES = ["filesystem", "network", "process", "security", "disk", "service"]
MAX_MISSIONS_PER_CAT = 4   # approx missions per category
TIME_LIMIT = 60.0           # seconds reference

TITLE_THRESHOLDS = [
    (0,  25,  "Trainee",                "Junior Operator"),
    (26, 45,  "Junior Operator",        "Systems Administrator"),
    (46, 65,  "Systems Administrator",  "Senior Engineer"),
    (66, 80,  "Senior Engineer",        "Infrastructure Architect"),
    (81, 100, "Infrastructure Architect", None),
]

CATEGORY_HINTS = {
    "filesystem": {
        "chapter": "Chapter 1: System Breach",
        "missions": ["Recover the logs", "Find the script"],
    },
    "network": {
        "chapter": "Chapter 2: Web Server Incident",
        "missions": ["Service diagnostic", "Port check"],
    },
    "process": {
        "chapter": "Chapter 2 & 3",
        "missions": ["Kill the rogue process", "Monitor the daemon"],
    },
    "security": {
        "chapter": "Chapter 3: Intrusion Detected",
        "missions": ["Block the IP", "Eliminate the backdoor"],
    },
    "disk": {
        "chapter": "Chapter 4: Storage Crisis",
        "missions": ["Free the disk space", "Compress the archive"],
    },
    "service": {
        "chapter": "Chapter 2: Web Server Incident",
        "missions": ["Restart the service", "Check service status"],
    },
}


def _compute_skill_score(stat):
    completed     = stat.get("completed", 0)
    total_attempts = stat.get("total_attempts", 1) or 1
    total_stars    = stat.get("total_stars", 0)
    avg_time       = stat.get("avg_time", TIME_LIMIT)

    if completed == 0:
        return 0.0

    completion_ratio = min(completed / MAX_MISSIONS_PER_CAT, 1.0)
    avg_stars_val    = total_stars / max(completed, 1)
    speed_bonus      = max(0.0, 1.0 - (avg_time / TIME_LIMIT))

    score = completion_ratio * 40.0 + (avg_stars_val / 3.0) * 35.0 + speed_bonus * 25.0
    return round(min(max(score, 0.0), 100.0), 2)


def _get_title(overall):
    for lo, hi, title, next_t in TITLE_THRESHOLDS:
        if lo <= overall <= hi:
            progress = ((overall - lo) / max(hi - lo, 1)) * 100
            return title, next_t or title, round(progress, 1)
    return "Infrastructure Architect", "Infrastructure Architect", 100.0


def _cert_readiness_fallback(skill_profile):
    result = {}
    for cert_name, requirements in _BENCHMARK_FALLBACK.items():
        gaps, strengths = [], []
        total_weight = len(requirements)
        weighted_match = 0.0
        for cat, required in requirements.items():
            current = skill_profile.get(cat, 0.0)
            weighted_match += min(current / required, 1.0) * (1.0 / total_weight) * 100
            if current < required:
                gaps.append({"category": cat, "current": round(current, 1),
                              "required": required, "gap": round(current - required, 1)})
            else:
                strengths.append({"category": cat, "current": round(current, 1),
                                   "required": required, "surplus": round(current - required, 1)})
        gaps.sort(key=lambda x: x["gap"])
        result[cert_name] = {
            "ready":         len(gaps) == 0,
            "overall_match": round(weighted_match, 1),
            "gaps":          gaps,
            "strengths":     strengths,
        }
    return result


def _cert_readiness(skill_profile, benchmarks_df):
    result = {}
    for cert in benchmarks_df["certification"].unique():
        cert_rows = benchmarks_df[benchmarks_df["certification"] == cert]
        gaps, strengths = [], []
        weighted_match = 0.0
        for _, row in cert_rows.iterrows():
            cat      = row["category"]
            required = float(row["min_score"])
            weight   = float(row["weight"])
            current  = skill_profile.get(cat, 0.0)
            weighted_match += min(current / required, 1.0) * weight * 100
            if current < required:
                gaps.append({"category": cat, "current": round(current, 1),
                              "required": int(required), "gap": round(current - required, 1)})
            else:
                strengths.append({"category": cat, "current": round(current, 1),
                                   "required": int(required), "surplus": round(current - required, 1)})

        gaps.sort(key=lambda x: x["gap"])
        result[cert] = {
            "ready":         len(gaps) == 0,
            "overall_match": round(weighted_match, 1),
            "gaps":          gaps,
            "strengths":     strengths,
        }
    return result


@app.route("/skill-analyze", methods=["POST"])
def skill_analyze():
    data = request.get_json()
    category_stats = data.get("category_stats", {})

    skill_profile = {}
    for cat in CATEGORIES:
        stat = category_stats.get(cat, {})
        skill_profile[cat] = _compute_skill_score(stat)

    overall_score = round(sum(skill_profile.values()) / len(CATEGORIES), 2)
    player_title, next_title, progress = _get_title(overall_score)

    predicted_weakness = min(skill_profile, key=skill_profile.get)
    weakness_confidence = 0.5

    if skill_predictor is not None and skill_label_encoder is not None:
        try:
            features = np.array([[
                skill_profile.get("filesystem", 0),
                skill_profile.get("network",    0),
                skill_profile.get("process",    0),
                skill_profile.get("security",   0),
                skill_profile.get("disk",       0),
                skill_profile.get("service",    0),
                data.get("total_missions_completed", 0),
                data.get("avg_attempts", 1.0),
                data.get("avg_stars",    0.0),
            ]])
            proba = skill_predictor.predict_proba(features)[0]
            pred_idx = int(np.argmax(proba))
            predicted_weakness  = skill_label_encoder.inverse_transform([pred_idx])[0]
            weakness_confidence = round(float(proba[pred_idx]), 4)
        except Exception as e:
            print(f"[skill-analyze] ML prediction error: {e}")

    print(f"[skill-analyze] skill_profile: {skill_profile}")
    print(f"[skill-analyze] benchmarks loaded: {skill_benchmarks is not None}")

    if skill_benchmarks is not None:
        cert_readiness = _cert_readiness(skill_profile, skill_benchmarks)
    else:
        cert_readiness = _cert_readiness_fallback(skill_profile)

    print(f"[skill-analyze] certification_readiness: {cert_readiness}")

    sorted_cats = sorted(skill_profile.items(), key=lambda x: x[1])
    recommendations = []
    for priority, (cat, score) in enumerate(sorted_cats[:2], start=1):
        hint = CATEGORY_HINTS.get(cat, {})
        recommendations.append({
            "priority":           priority,
            "category":           cat,
            "message":            (f"Score {round(score, 0):.0f}% in {cat}. "
                                   f"Focus on {hint.get('chapter', cat)} to improve."),
            "suggested_missions": hint.get("missions", []),
        })

    return jsonify({
        "skill_profile":           skill_profile,
        "overall_score":           overall_score,
        "predicted_weakness":      predicted_weakness,
        "weakness_confidence":     weakness_confidence,
        "certification_readiness": cert_readiness,
        "recommendations":         recommendations,
        "player_title":            player_title,
        "next_title":              next_title,
        "progress_to_next_title":  progress,
    })


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=True)
