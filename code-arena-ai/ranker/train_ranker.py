"""Train the Score Ranker model.

Loads training_data.csv, trains a GradientBoostingRegressor, evaluates it,
and saves the model artifacts (ranker_model.pkl, label_encoder.pkl,
feature_columns.pkl).
"""

import os

import joblib
import numpy as np
import pandas as pd
from sklearn.ensemble import GradientBoostingRegressor
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))

FEATURE_COLUMNS = [
    "execution_time_ms", "memory_kb", "exit_code", "tests_passed_ratio",
    "total_lines", "code_lines", "avg_line_length",
    "for_loop_count", "while_loop_count", "nested_loop_depth", "recursion_detected",
    "uses_hashmap", "uses_set", "uses_sorting",
    "cyclomatic_complexity_avg", "cyclomatic_complexity_max",
    "language_speed_factor", "language_encoded",
]


def main() -> None:
    """Train the ranker model and save artifacts."""
    csv_path = os.path.join(SCRIPT_DIR, "training_data.csv")
    df = pd.read_csv(csv_path)

    print(f"[train_ranker] Loaded {len(df)} rows from {csv_path}")

    if len(df) < 10:
        print("\u26a0\ufe0f  Dataset too small. Add more samples to generate_dataset.py")
        print(f"   Current size: {len(df)} rows. Minimum recommended: 10 for testing, 500+ for production.")

    # Encode language
    le = LabelEncoder()
    df["language_encoded"] = le.fit_transform(df["language"])
    joblib.dump(le, os.path.join(SCRIPT_DIR, "label_encoder.pkl"))

    X = df[FEATURE_COLUMNS]
    y = df["optimization_score"]

    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42,
    )

    print(f"[train_ranker] Training on {len(X_train)} samples, testing on {len(X_test)}")

    model = GradientBoostingRegressor(
        n_estimators=200, max_depth=4, learning_rate=0.05, random_state=42,
    )
    model.fit(X_train, y_train)

    # Evaluate
    y_pred = model.predict(X_test)
    mae = np.mean(np.abs(y_test.values - y_pred))
    ss_res = np.sum((y_test.values - y_pred) ** 2)
    ss_tot = np.sum((y_test.values - np.mean(y_test.values)) ** 2)
    r2 = 1 - (ss_res / ss_tot) if ss_tot > 0 else 0.0

    print(f"[train_ranker] MAE: {mae:.2f} | R\u00b2: {r2:.3f}")

    # Feature importances
    importances = sorted(
        zip(FEATURE_COLUMNS, model.feature_importances_),
        key=lambda x: x[1], reverse=True,
    )
    print("\n[train_ranker] Feature importances:")
    for name, imp in importances:
        print(f"  {name:35s} {imp:.4f}")

    # Sample predictions
    print("\n[train_ranker] Sample predictions (actual vs predicted):")
    sample_indices = X_test.index[:5]
    for idx in sample_indices:
        actual = y.loc[idx]
        pred = model.predict(X.loc[[idx]])[0]
        print(f"  idx={idx:3d}  actual={actual:6.2f}  predicted={pred:6.2f}")

    # Save artifacts
    joblib.dump(model, os.path.join(SCRIPT_DIR, "ranker_model.pkl"))
    joblib.dump(FEATURE_COLUMNS, os.path.join(SCRIPT_DIR, "feature_columns.pkl"))
    print(f"\n[train_ranker] Saved model artifacts to {SCRIPT_DIR}")


if __name__ == "__main__":
    main()
