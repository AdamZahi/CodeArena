"""Production Score Ranker module.

Loads trained model artifacts at import time and exposes score_submission()
and decide_winner() for the Battle backend.
"""

import os

import joblib
import numpy as np

from feature_extractor import extract_features

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))

try:
    model = joblib.load(os.path.join(SCRIPT_DIR, "ranker_model.pkl"))
    label_encoder = joblib.load(os.path.join(SCRIPT_DIR, "label_encoder.pkl"))
    feature_columns = joblib.load(os.path.join(SCRIPT_DIR, "feature_columns.pkl"))
except FileNotFoundError as e:
    raise FileNotFoundError(
        f"Model artifacts not found in {SCRIPT_DIR}. "
        "Run train_ranker.py first to generate them."
    ) from e


def score_submission(source_code: str, piston_result: dict, language: str,
                     total_tests: int, passed_tests: int) -> dict:
    """Score a single code submission.

    Returns a dict with 'score' (0–100), 'breakdown', and 'error' fields.
    """
    try:
        features = extract_features(source_code, piston_result, language,
                                    total_tests, passed_tests)

        # Encode language
        try:
            lang_encoded = label_encoder.transform([language])[0]
        except ValueError:
            lang_encoded = -1
        features["language_encoded"] = lang_encoded

        # Build feature vector in the correct column order
        feature_vector = [features.get(col, 0) for col in feature_columns]

        raw_score = model.predict([feature_vector])[0]
        score = round(float(np.clip(raw_score, 0, 100)), 2)

        run_info = piston_result.get("run", piston_result)

        print(f"[ranker] score_submission called: {language} | "
              f"time={features.get('execution_time_ms', '?')}ms | score={score}")

        return {
            "score": score,
            "breakdown": {
                "time_ms": features.get("execution_time_ms", 0.0),
                "memory_kb": features.get("memory_kb", 0.0),
                "tests_ratio": features.get("tests_passed_ratio", 0.0),
            },
            "error": None,
        }
    except Exception as e:
        return {
            "score": 0.0,
            "breakdown": {"time_ms": 0.0, "memory_kb": 0.0, "tests_ratio": 0.0},
            "error": str(e),
        }


def decide_winner(result_a: dict, result_b: dict) -> dict:
    """Decide the winner between two scored submissions.

    Returns a dict with 'winner' ('A', 'B', or 'draw'), both scores,
    and the margin.
    """
    score_a = result_a["score"]
    score_b = result_b["score"]
    margin = abs(score_a - score_b)

    if margin < 2.0:
        winner = "draw"
        margin = 0.0
    elif score_a > score_b:
        winner = "A"
    else:
        winner = "B"

    return {
        "winner": winner,
        "score_a": score_a,
        "score_b": score_b,
        "margin": round(margin, 2),
    }
