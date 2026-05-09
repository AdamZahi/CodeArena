import numpy as np
import pandas as pd
import os

np.random.seed(42)
N = 1500
CATEGORIES = ["filesystem", "network", "process", "security", "disk", "service"]

os.makedirs("dataset", exist_ok=True)

records = []
per_cat = N // len(CATEGORIES)

for weak_idx, weak_cat in enumerate(CATEGORIES):
    for _ in range(per_cat):
        scores = {}
        for i, cat in enumerate(CATEGORIES):
            if i == weak_idx:
                scores[cat] = np.random.uniform(5, 40)   # weak
            else:
                scores[cat] = np.random.uniform(50, 100) # strong
        total_completed = int(np.random.uniform(4, 16))
        avg_attempts    = round(np.random.uniform(1, 8), 2)
        avg_stars       = round(np.random.uniform(0.5, 3.0), 2)
        records.append({
            "filesystem_score":        round(scores["filesystem"], 2),
            "network_score":           round(scores["network"], 2),
            "process_score":           round(scores["process"], 2),
            "security_score":          round(scores["security"], 2),
            "disk_score":              round(scores["disk"], 2),
            "service_score":           round(scores["service"], 2),
            "total_missions_completed": total_completed,
            "avg_attempts":            avg_attempts,
            "avg_stars":               avg_stars,
            "weakest_category":        weak_cat,
        })

df = pd.DataFrame(records).sample(frac=1, random_state=42).reset_index(drop=True)
df.to_csv("dataset/skill_training.csv", index=False)
print(f"skill_training.csv generated: {len(df)} rows")
print(df["weakest_category"].value_counts())
