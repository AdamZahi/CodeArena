import json, pandas as pd

with open('data/leetcode_generated.json') as f:
    data = json.load(f)

df_new = pd.DataFrame(data)[['title','description','tags','difficulty','hint']]
print(f"New problems: {len(df_new)}")

# Re-read from backup/original
existing = pd.read_csv('data/full_cleaned.csv')
print(f"Existing rows: {len(existing)}")

# Get titles already in existing
existing_titles = set(existing['title'].str.lower().str.strip())

# Only add truly new problems
new_only = df_new[~df_new['title'].str.lower().str.strip().isin(existing_titles)]
print(f"Truly new problems: {len(new_only)}")

combined = pd.concat([existing, new_only], ignore_index=True)
combined.to_csv('data/full_cleaned.csv', index=False)
print(f"Total after merge: {len(combined)}")
