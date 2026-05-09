import numpy as np
import pandas as pd
import os

np.random.seed(42)
N = 2000

success_rate      = np.random.beta(2, 2, N)
avg_attempts      = np.random.uniform(1, 10, N)
avg_response_time = np.random.uniform(5, 90, N)
command_category  = np.random.randint(0, 6, N)
difficulty        = np.random.randint(0, 4, N)
streak            = np.random.randint(-5, 6, N)

# Compute realistic success probability — centered so global mean ≈ 0.5.
# Each term is mean-centered: high success_rate / low difficulty / positive streak → success.
base_prob = (
    0.5
    + (success_rate - 0.5) * 0.6          # skill matters most
    - (difficulty - 1.5) * 0.1            # harder missions reduce success
    - (avg_attempts - 5.5) / 9.0 * 0.08  # more attempts = struggling
    - (avg_response_time - 47.5) / 85.0 * 0.06  # slow response = harder
    + streak * 0.03                        # momentum effect
)
base_prob = np.clip(base_prob, 0.02, 0.98)
result = (np.random.rand(N) < base_prob).astype(int)

df = pd.DataFrame({
    "success_rate":      np.round(success_rate, 4),
    "avg_attempts":      np.round(avg_attempts, 2),
    "avg_response_time": np.round(avg_response_time, 2),
    "command_category":  command_category,
    "difficulty":        difficulty,
    "streak":            streak,
    "result":            result,
})

os.makedirs("dataset", exist_ok=True)
df.to_csv("dataset/adaptive_sessions.csv", index=False)
print(f"Dataset generated: {len(df)} rows")
print(df.describe())
print(f"\nSuccess rate: {df['result'].mean():.3f}")
