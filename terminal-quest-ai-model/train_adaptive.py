import numpy as np
import pandas as pd
import joblib
import os
from sklearn.preprocessing import StandardScaler
from sklearn.neural_network import MLPClassifier
from sklearn.metrics import classification_report, confusion_matrix

os.makedirs("model", exist_ok=True)

df = pd.read_csv("dataset/adaptive_sessions.csv")
X = df[["success_rate", "avg_attempts", "avg_response_time",
        "command_category", "difficulty", "streak"]].values
y = df["result"].values

scaler = StandardScaler()
X_scaled = scaler.fit_transform(X)

joblib.dump(scaler, "model/adaptive_scaler.pkl")

model = MLPClassifier(
    hidden_layer_sizes=(32, 16),
    activation="relu",
    max_iter=200,
    early_stopping=True,
    validation_fraction=0.2,
    random_state=42,
    verbose=True,
)
model.fit(X_scaled, y)

joblib.dump(model, "model/adaptive_model.pkl")
print("\nModel saved to model/adaptive_model.pkl")

# Evaluation
split = int(len(X_scaled) * 0.8)
X_test, y_test = X_scaled[split:], y[split:]
y_pred = model.predict(X_test)

print("\nClassification Report:")
print(classification_report(y_test, y_pred, target_names=["Fail", "Success"]))
print("Confusion Matrix:")
print(confusion_matrix(y_test, y_pred))
