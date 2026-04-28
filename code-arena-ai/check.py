import json

with open("data/leetcode_generated.json", "r", encoding="utf-8") as f:
    data = json.load(f)

for i in range(5):
    print(f"[{i}] {data[i]['title']}: {data[i]['hint']}")
