import pandas as pd
import joblib
import os
from sklearn.ensemble import RandomForestClassifier
from sklearn.preprocessing import LabelEncoder
from sklearn.metrics import classification_report, confusion_matrix

os.makedirs("model", exist_ok=True)

df = pd.read_csv("dataset/skill_training.csv")

FEATURES = ["filesystem_score", "network_score", "process_score",
            "security_score", "disk_score", "service_score",
            "total_missions_completed", "avg_attempts", "avg_stars"]
X = df[FEATURES].values
y = df["weakest_category"].values

le = LabelEncoder()
y_enc = le.fit_transform(y)

model = RandomForestClassifier(
    n_estimators=100,
    max_depth=10,
    random_state=42,
    class_weight="balanced",
)
model.fit(X, y_enc)

joblib.dump(model, "model/skill_predictor.pkl")
joblib.dump(le,    "model/skill_label_encoder.pkl")
print("Models saved: model/skill_predictor.pkl, model/skill_label_encoder.pkl")

split = int(len(X) * 0.8)
X_test, y_test = X[split:], y_enc[split:]
y_pred = model.predict(X_test)

print("\nClassification Report:")
print(classification_report(y_test, y_pred, target_names=le.classes_))
print("Confusion Matrix:")
print(confusion_matrix(y_test, y_pred))

importances = sorted(zip(FEATURES, model.feature_importances_), key=lambda x: -x[1])
print("\nFeature Importances:")
for name, imp in importances:
    print(f"  {name:<30} {imp:.4f}")
