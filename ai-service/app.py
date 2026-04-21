"""
CodeArena AI Service — ML-Powered Version 2.0
==============================================
Models:
  eco_model.pkl          → TF-IDF + Random Forest (scikit-learn)
  recommend_model.json   → Cosine Similarity matrix (pure JSON, no pandas)

Run: python app.py
"""

from flask import Flask, request, jsonify
from flask_cors import CORS
import joblib
import json
import os

app = Flask(__name__)
CORS(app)

# ─────────────────────────────────────────────────────────────────────────────
# LOAD TRAINED MODELS ON STARTUP
# ─────────────────────────────────────────────────────────────────────────────

eco_model = None
recommend_artifacts = None

def load_models():
    global eco_model, recommend_artifacts

    if os.path.exists('eco_model.pkl'):
        eco_model = joblib.load('eco_model.pkl')
        print("✅ Loaded eco_model.pkl (TF-IDF + Random Forest)")
    else:
        print("⚠️  eco_model.pkl not found")

    if os.path.exists('recommend_model.json'):
        with open('recommend_model.json', 'r') as f:
            recommend_artifacts = json.load(f)
        print("✅ Loaded recommend_model.json (Cosine Similarity)")
    else:
        print("⚠️  recommend_model.json not found")

load_models()

# ─────────────────────────────────────────────────────────────────────────────
# ECO SCORE
# ─────────────────────────────────────────────────────────────────────────────

ECO_LABELS = {
    (9, 10): ("Excellent", "#10b981", "🌱"),
    (7, 8):  ("Good",      "#22c55e", "🌿"),
    (4, 6):  ("Average",   "#f59e0b", "♻️"),
    (1, 3):  ("Poor",      "#ef4444", "⚠️"),
}

ECO_REASONS = {
    "Excellent": "Minimal carbon footprint — sustainable materials aligned with SDG 12.",
    "Good":      "Low carbon lifecycle with good recycling efficiency.",
    "Average":   "Standard environmental impact for this product category.",
    "Poor":      "High carbon footprint — consider more sustainable alternatives.",
}

def score_to_label(score):
    for (low, high), (label, color, emoji) in ECO_LABELS.items():
        if low <= score <= high:
            return label, color, emoji
    return "Average", "#f59e0b", "♻️"

def predict_eco_score(product_name, category):
    if eco_model is None:
        return {"score": 5, "label": "Average", "reason": "Model not loaded.", "color": "#f59e0b", "emoji": "♻️"}

    score = int(eco_model.predict([f"{product_name} {category}"])[0])
    score = max(1, min(10, score))
    label, color, emoji = score_to_label(score)
    return {"score": score, "label": label, "reason": ECO_REASONS[label], "color": color, "emoji": emoji}

# ─────────────────────────────────────────────────────────────────────────────
# RECOMMENDATIONS
# ─────────────────────────────────────────────────────────────────────────────

def get_recommendations(user_orders, all_products, limit=4):
    if not all_products:
        return []

    if recommend_artifacts is None or not user_orders:
        sorted_p = sorted(all_products, key=lambda p: p.get("stock", 0), reverse=True)
        return [{"id": p["id"], "score": 0.5, "reason": "Popular item"} for p in sorted_p[:limit]]

    categories = recommend_artifacts["categories"]
    sim_matrix = recommend_artifacts["similarity_matrix"]  # plain list of lists

    category_weights = {}
    ordered_ids = set()

    for order in user_orders:
        cat = order.get("category", "OTHER")
        qty = order.get("quantity", 1)
        category_weights[cat] = category_weights.get(cat, 0) + qty
        ordered_ids.add(order.get("productId", ""))

    if not category_weights:
        return []

    # Expand weights using similarity matrix
    expanded = {}
    for cat, weight in category_weights.items():
        if cat in categories:
            idx = categories.index(cat)
            for j, sim_cat in enumerate(categories):
                sim_score = sim_matrix[idx][j]
                expanded[sim_cat] = expanded.get(sim_cat, 0) + weight * sim_score

    total = sum(expanded.values()) or 1
    cat_prefs = {cat: w / total for cat, w in expanded.items()}
    top_cat = max(category_weights, key=category_weights.get)

    scored = []
    for product in all_products:
        pid = product.get("id", "")
        if pid in ordered_ids or product.get("stock", 0) <= 0:
            continue
        cat = product.get("category", "OTHER")
        score = cat_prefs.get(cat, 0.05) + min(product.get("stock", 0) / 200, 0.1)
        scored.append({"id": pid, "score": round(score, 4), "reason": f"Based on your {top_cat} interest"})

    scored.sort(key=lambda x: x["score"], reverse=True)
    return scored[:limit]

# ─────────────────────────────────────────────────────────────────────────────
# ROUTES
# ─────────────────────────────────────────────────────────────────────────────

@app.route("/health", methods=["GET"])
def health():
    return jsonify({
        "status": "ok",
        "service": "CodeArena AI Service",
        "version": "2.0-ml",
        "models": {
            "eco_score":       "loaded ✅" if eco_model else "not loaded ⚠️",
            "recommendations": "loaded ✅" if recommend_artifacts else "not loaded ⚠️"
        }
    })

@app.route("/api/eco-score", methods=["POST"])
def eco_score():
    try:
        data = request.get_json()
        if not data or not data.get("productName"):
            return jsonify({"error": "productName required"}), 400
        result = predict_eco_score(data.get("productName", ""), data.get("category", "OTHER"))
        return jsonify({"success": True, "productId": data.get("productId", ""), **result})
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route("/api/eco-score/batch", methods=["POST"])
def eco_score_batch():
    try:
        data = request.get_json()
        products = data.get("products", []) if data else []
        scores = {}
        for p in products:
            if p.get("id") and p.get("name"):
                scores[p["id"]] = predict_eco_score(p["name"], p.get("category", "OTHER"))
        return jsonify({
            "success": True, "count": len(scores), "scores": scores,
            "model": "TF-IDF + Random Forest trained on Kaggle carbon footprint dataset"
        })
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route("/api/recommend", methods=["POST"])
def recommend():
    try:
        data = request.get_json()
        if not data:
            return jsonify({"error": "Request body required"}), 400
        recs = get_recommendations(
            data.get("userOrders", []),
            data.get("allProducts", []),
            int(data.get("limit", 4))
        )
        return jsonify({
            "success": True,
            "participantId": data.get("participantId", ""),
            "recommendations": recs,
            "basedOn": f"{len(data.get('userOrders', []))} signals (orders + wishlist)",
            "model": "Cosine Similarity — trained on 500 users, 50 products"
        })
    except Exception as e:
        return jsonify({"error": str(e)}), 500

if __name__ == "__main__":
    print("=" * 55)
    print("CodeArena AI Service v2.0 (ML-Powered)")
    print("Health:    http://localhost:5000/health")
    print("Eco Score: http://localhost:5000/api/eco-score")
    print("Batch:     http://localhost:5000/api/eco-score/batch")
    print("Recommend: http://localhost:5000/api/recommend")
    print("=" * 55)
    app.run(host="0.0.0.0", port=5000, debug=True)