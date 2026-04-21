"""
CodeArena AI Model Training Script
===================================
Trains two models using the real Kaggle carbon footprint dataset:
  1. Eco Score Model — TF-IDF + Random Forest (text classification)
  2. Recommendation Model — Cosine Similarity (collaborative filtering)

Run: python train_model.py
Output: eco_model.pkl, recommend_model.pkl, eco_dataset.csv, orders_dataset.csv
"""

import pandas as pd
import numpy as np
import joblib
import random
import os
from sklearn.ensemble import RandomForestClassifier
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.pipeline import Pipeline
from sklearn.model_selection import train_test_split
from sklearn.metrics import classification_report
from sklearn.preprocessing import MinMaxScaler
from sklearn.metrics.pairwise import cosine_similarity

# ─────────────────────────────────────────────────────────────────────────────
# STEP 1 — LOAD REAL KAGGLE DATASET
# ─────────────────────────────────────────────────────────────────────────────

print("=" * 60)
print("STEP 1: Loading Kaggle carbon footprint dataset...")
print("=" * 60)

df_raw = pd.read_csv("dataset/product_lifecycle_carbon_dataset.csv")
print(f"✅ Loaded {len(df_raw)} rows from real dataset")
print(f"   Columns: {list(df_raw.columns)}")
print(f"   Carbon footprint range: {df_raw['total_lifecycle_carbon_footprint'].min():.1f} → {df_raw['total_lifecycle_carbon_footprint'].max():.1f}")

# ─────────────────────────────────────────────────────────────────────────────
# STEP 2 — BUILD ECO SCORE DATASET
# Maps real carbon footprint values → product names → eco scores (1-10)
# Lower carbon footprint = higher eco score
# ─────────────────────────────────────────────────────────────────────────────

print("\n" + "=" * 60)
print("STEP 2: Building eco score training dataset...")
print("=" * 60)

# Product templates per category with sustainability keywords
PRODUCT_TEMPLATES = {
    "MOUSEPAD": [
        "CodeArena XL Mouse Pad", "Recycled Rubber Mousepad", "Eco Desk Mat",
        "Bamboo Mouse Pad", "Standard Mousepad", "Plastic Mousepad Pro",
        "Organic Cotton Desk Pad", "Sustainable Gaming Mat", "Mousepad Basic",
        "Non-recyclable Foam Pad"
    ],
    "KEYBOARD": [
        "Mechanical Keyboard RGB", "Recycled Plastic Keyboard", "Eco Keyboard",
        "Bamboo Wireless Keyboard", "Standard USB Keyboard", "Disposable Keyboard",
        "Sustainable Keyboard Pro", "Organic Materials Keyboard", "Basic Keyboard",
        "Single-use Event Keyboard"
    ],
    "HOODIE": [
        "CodeArena Hoodie", "Organic Cotton Hoodie", "Recycled Fleece Hoodie",
        "Sustainable Zip Hoodie", "Polyester Hoodie", "Fast Fashion Hoodie",
        "Bamboo Blend Hoodie", "Eco-friendly Pullover", "Synthetic Hoodie",
        "Premium Cotton Hoodie"
    ],
    "TSHIRT": [
        "CodeArena T-Shirt", "Organic Cotton Tee", "Recycled Material Shirt",
        "Sustainable Print Tee", "Polyester T-Shirt", "Synthetic Blend Tee",
        "Bamboo Fiber Shirt", "Eco Print T-Shirt", "Standard Cotton Tee",
        "Acrylic Blend Shirt"
    ],
    "MUG": [
        "Dark Mode Mug", "Reusable Ceramic Mug", "Eco Bamboo Mug",
        "Recycled Glass Mug", "Plastic Travel Mug", "Disposable Coffee Cup",
        "Organic Clay Mug", "Sustainable Tumbler", "Standard Ceramic Mug",
        "Single-use Plastic Mug"
    ],
    "STICKER": [
        "Vinyl Sticker Pack", "Plastic Sticker Set", "Non-recyclable Stickers",
        "Disposable Label Pack", "Standard Sticker Pack", "Eco Paper Stickers",
        "Recycled Material Stickers", "Biodegradable Sticker Pack",
        "Synthetic Adhesive Stickers", "Basic Sticker Pack"
    ],
    "NOTEBOOK": [
        "Developer Notebook", "Recycled Paper Notebook", "Bamboo Cover Journal",
        "Sustainable Ruled Notebook", "Plastic Cover Notebook", "Eco Sketchbook",
        "Organic Paper Journal", "Standard Notebook", "Vinyl Cover Notebook",
        "Biodegradable Notebook"
    ],
    "CAP": [
        "CodeArena Cap", "Organic Cotton Cap", "Recycled Polyester Hat",
        "Sustainable Baseball Cap", "Synthetic Cap", "Eco-friendly Hat",
        "Bamboo Blend Cap", "Standard Cap", "Acrylic Cap", "Basic Hat"
    ],
    "BACKPACK": [
        "CodeArena Backpack", "Recycled Nylon Backpack", "Eco Canvas Bag",
        "Sustainable Travel Pack", "Plastic Backpack", "Bamboo Fiber Bag",
        "Organic Cotton Backpack", "Standard Backpack", "Synthetic Bag",
        "Durable Eco Backpack"
    ],
    "POSTER": [
        "Algorithm Poster", "Recycled Paper Print", "Eco Art Poster",
        "Sustainable Wall Print", "Vinyl Banner", "Plastic-coated Poster",
        "Biodegradable Print", "Standard Paper Poster", "Laminated Poster",
        "Non-recyclable Print"
    ]
}

CATEGORIES = list(PRODUCT_TEMPLATES.keys())

# Normalize carbon footprint to eco score (1-10)
# Lower carbon = higher eco score (inverted relationship)
scaler = MinMaxScaler(feature_range=(1, 10))
carbon_values = df_raw['total_lifecycle_carbon_footprint'].values.reshape(-1, 1)
eco_scores_raw = scaler.fit_transform(carbon_values)
# Invert: low carbon → high eco score
eco_scores_inverted = 11 - eco_scores_raw
eco_scores_int = np.clip(np.round(eco_scores_inverted).astype(int), 1, 10).flatten()

# Build eco training dataset
random.seed(42)
eco_rows = []

for i, row in df_raw.iterrows():
    score = int(eco_scores_int[i])
    category = CATEGORIES[i % len(CATEGORIES)]
    
    # Pick product name based on score range
    templates = PRODUCT_TEMPLATES[category]
    if score >= 8:
        # High score — pick eco/organic/recycled products (first half)
        name = templates[random.randint(0, 3)]
    elif score >= 5:
        # Medium score — pick standard products (middle)
        name = templates[random.randint(3, 6)]
    else:
        # Low score — pick plastic/synthetic/disposable products (last half)
        name = templates[random.randint(6, 9)]
    
    eco_rows.append({
        "product_name": name,
        "category": category,
        "carbon_footprint": row['total_lifecycle_carbon_footprint'],
        "recycling_efficiency": row['recycling_efficiency'],
        "manufacturing_efficiency": row['manufacturing_efficiency'],
        "raw_material_waste": row['raw_material_waste'],
        "eco_score": score
    })

eco_df = pd.DataFrame(eco_rows)
eco_df.to_csv("dataset/eco_dataset.csv", index=False)
print(f"✅ Created eco_dataset.csv with {len(eco_df)} rows")
print(f"   Score distribution:\n{eco_df['eco_score'].value_counts().sort_index()}")

# ─────────────────────────────────────────────────────────────────────────────
# STEP 3 — TRAIN ECO SCORE MODEL
# Pipeline: TF-IDF vectorizer on product name + Random Forest classifier
# ─────────────────────────────────────────────────────────────────────────────

print("\n" + "=" * 60)
print("STEP 3: Training Eco Score Model (TF-IDF + Random Forest)...")
print("=" * 60)

# Features: combine product name + category as text
eco_df['text_features'] = eco_df['product_name'] + ' ' + eco_df['category']

X = eco_df['text_features']
y = eco_df['eco_score']

X_train, X_test, y_train, y_test = train_test_split(
    X, y, test_size=0.2, random_state=42
)

# Build pipeline: TF-IDF → Random Forest
eco_pipeline = Pipeline([
    ('tfidf', TfidfVectorizer(
        ngram_range=(1, 2),      # unigrams + bigrams
        max_features=500,         # top 500 features
        lowercase=True,
        stop_words='english'
    )),
    ('clf', RandomForestClassifier(
        n_estimators=100,         # 100 decision trees
        random_state=42,
        n_jobs=-1                 # use all CPU cores
    ))
])

eco_pipeline.fit(X_train, y_train)

# Evaluate
y_pred = eco_pipeline.predict(X_test)
accuracy = (y_pred == y_test).mean()
print(f"✅ Model trained successfully!")
print(f"   Training samples: {len(X_train)}")
print(f"   Test samples: {len(X_test)}")
print(f"   Accuracy: {accuracy:.2%}")

# Save model + scaler
joblib.dump(eco_pipeline, 'eco_model.pkl')
joblib.dump(scaler, 'carbon_scaler.pkl')
print(f"✅ Saved eco_model.pkl")

# ─────────────────────────────────────────────────────────────────────────────
# STEP 4 — BUILD RECOMMENDATION DATASET
# Simulates 500 users with realistic order + wishlist behavior
# Based on category affinity patterns
# ─────────────────────────────────────────────────────────────────────────────

print("\n" + "=" * 60)
print("STEP 4: Building recommendation training dataset...")
print("=" * 60)

# 50 products (realistic CodeArena shop products)
PRODUCTS = [
    {"id": f"prod-{i+1:03d}", "name": name, "category": cat, "price": price, "stock": stock}
    for i, (name, cat, price, stock) in enumerate([
        ("CodeArena XL Mouse Pad", "MOUSEPAD", 24.99, 60),
        ("CodeArena Hoodie", "HOODIE", 39.99, 50),
        ("CodeArena Zip Hoodie", "HOODIE", 44.99, 14),
        ("Dark Mode Mug", "MUG", 17.99, 8),
        ("Developer Notebook", "NOTEBOOK", 12.99, 89),
        ("Developer Sticker Pack x10", "STICKER", 9.99, 198),
        ("Eat Sleep Code Cap", "CAP", 22.99, 50),
        ("CodeArena Backpack", "BACKPACK", 59.99, 4),
        ("Organic Cotton Hoodie", "HOODIE", 49.99, 30),
        ("Recycled Rubber Mousepad", "MOUSEPAD", 19.99, 45),
        ("Bamboo Developer Mug", "MUG", 21.99, 25),
        ("Eco Paper Stickers", "STICKER", 6.99, 150),
        ("Recycled Paper Notebook", "NOTEBOOK", 14.99, 70),
        ("Sustainable Cap", "CAP", 24.99, 40),
        ("CodeArena T-Shirt Black", "TSHIRT", 19.99, 80),
        ("Organic Cotton Tee", "TSHIRT", 22.99, 60),
        ("Mechanical Keyboard RGB", "KEYBOARD", 89.99, 20),
        ("Bamboo Wireless Keyboard", "KEYBOARD", 74.99, 15),
        ("Algorithm Poster A2", "POSTER", 14.99, 100),
        ("Recycled Paper Print", "POSTER", 12.99, 80),
        ("CodeArena Premium Backpack", "BACKPACK", 79.99, 10),
        ("Eco Canvas Backpack", "BACKPACK", 54.99, 20),
        ("Dark Mode T-Shirt", "TSHIRT", 18.99, 90),
        ("Bamboo Blend Hoodie", "HOODIE", 54.99, 25),
        ("Reusable Ceramic Mug", "MUG", 15.99, 35),
        ("Vinyl Sticker Set", "STICKER", 11.99, 200),
        ("Bamboo Cover Journal", "NOTEBOOK", 16.99, 55),
        ("CodeArena Snapback", "CAP", 19.99, 45),
        ("Eco Print T-Shirt", "TSHIRT", 21.99, 75),
        ("USB Mechanical Keyboard", "KEYBOARD", 99.99, 12),
        ("Developer Poster Pack", "POSTER", 19.99, 60),
        ("CodeArena Tote Bag", "BACKPACK", 34.99, 30),
        ("Binary Code Mug", "MUG", 13.99, 40),
        ("Premium Sticker Pack", "STICKER", 14.99, 120),
        ("CodeArena Ruled Notebook", "NOTEBOOK", 10.99, 95),
        ("Retro Dev Cap", "CAP", 27.99, 35),
        ("Algorithm T-Shirt", "TSHIRT", 24.99, 65),
        ("Compact Keyboard", "KEYBOARD", 59.99, 18),
        ("Motivational Dev Poster", "POSTER", 9.99, 110),
        ("Recycled Nylon Backpack", "BACKPACK", 64.99, 22),
        ("CodeArena White Mug", "MUG", 12.99, 50),
        ("Holographic Stickers", "STICKER", 7.99, 180),
        ("Leather Cover Notebook", "NOTEBOOK", 24.99, 40),
        ("CodeArena Dad Cap", "CAP", 17.99, 55),
        ("Oversized Dev Hoodie", "HOODIE", 59.99, 20),
        ("CodeArena Polo Shirt", "TSHIRT", 29.99, 45),
        ("Wireless Slim Keyboard", "KEYBOARD", 69.99, 16),
        ("Canvas Art Poster", "POSTER", 17.99, 75),
        ("Mini Backpack", "BACKPACK", 44.99, 28),
        ("Bamboo Travel Mug", "MUG", 19.99, 30),
    ])
]

# Category affinity groups — users tend to buy within groups
AFFINITY_GROUPS = {
    "developer_gear": ["KEYBOARD", "MOUSEPAD", "NOTEBOOK"],
    "wearables": ["HOODIE", "TSHIRT", "CAP"],
    "desk_setup": ["MUG", "POSTER", "MOUSEPAD"],
    "eco_conscious": ["NOTEBOOK", "MUG", "TSHIRT"],
    "collector": ["STICKER", "POSTER", "CAP"],
    "traveler": ["BACKPACK", "MUG", "CAP"],
}

# Generate 500 synthetic users with realistic order patterns
random.seed(42)
order_rows = []
user_ids = [f"google-oauth2|user{i:04d}" for i in range(500)]
product_ids = [p["id"] for p in PRODUCTS]
product_map = {p["id"]: p for p in PRODUCTS}

for user_id in user_ids:
    # Each user has a primary affinity group
    affinity = random.choice(list(AFFINITY_GROUPS.keys()))
    preferred_cats = AFFINITY_GROUPS[affinity]
    
    # Order 2-6 products
    num_orders = random.randint(2, 6)
    
    # 70% chance picks from preferred categories, 30% random
    for _ in range(num_orders):
        if random.random() < 0.7:
            # Pick from preferred category
            cat = random.choice(preferred_cats)
            cat_products = [p for p in PRODUCTS if p["category"] == cat]
            product = random.choice(cat_products)
        else:
            product = random.choice(PRODUCTS)
        
        quantity = random.randint(1, 3)
        signal = "order"
        
        order_rows.append({
            "user_id": user_id,
            "product_id": product["id"],
            "product_name": product["name"],
            "category": product["category"],
            "quantity": quantity,
            "signal": signal,
            "affinity_group": affinity
        })
    
    # Also add 1-3 wishlist items (stronger signal)
    num_wishlist = random.randint(1, 3)
    for _ in range(num_wishlist):
        if random.random() < 0.8:
            cat = random.choice(preferred_cats)
            cat_products = [p for p in PRODUCTS if p["category"] == cat]
            product = random.choice(cat_products)
        else:
            product = random.choice(PRODUCTS)
        
        order_rows.append({
            "user_id": user_id,
            "product_id": product["id"],
            "product_name": product["name"],
            "category": product["category"],
            "quantity": 3,  # wishlist = 3x weight
            "signal": "wishlist",
            "affinity_group": affinity
        })

orders_df = pd.DataFrame(order_rows)
orders_df.to_csv("dataset/orders_dataset.csv", index=False)
print(f"✅ Created orders_dataset.csv")
print(f"   Users: {orders_df['user_id'].nunique()}")
print(f"   Interactions: {len(orders_df)}")
print(f"   Products covered: {orders_df['product_id'].nunique()}")

# ─────────────────────────────────────────────────────────────────────────────
# STEP 5 — TRAIN RECOMMENDATION MODEL
# Builds category affinity matrix using cosine similarity
# ─────────────────────────────────────────────────────────────────────────────

print("\n" + "=" * 60)
print("STEP 5: Training Recommendation Model (Cosine Similarity)...")
print("=" * 60)

# Build user-category interaction matrix
# Each cell = total weighted interactions (order=1x, wishlist=3x)
category_list = list(set(p["category"] for p in PRODUCTS))

user_category_matrix = orders_df.pivot_table(
    index='user_id',
    columns='category',
    values='quantity',
    aggfunc='sum',
    fill_value=0
)

print(f"   User-category matrix shape: {user_category_matrix.shape}")

# Compute category-category similarity matrix
category_similarity = cosine_similarity(user_category_matrix.T)
category_sim_df = pd.DataFrame(
    category_similarity,
    index=user_category_matrix.columns,
    columns=user_category_matrix.columns
)

print(f"   Category similarity matrix computed")
print(f"   Sample similarities:")
for cat in ["HOODIE", "KEYBOARD", "MUG"]:
    if cat in category_sim_df:
        top = category_sim_df[cat].sort_values(ascending=False).head(3)
        print(f"   {cat} → {dict(top)}")

# Save recommendation model artifacts
recommend_artifacts = {
    "category_similarity": category_sim_df,
    "products": PRODUCTS,
    "category_list": category_list,
    "user_category_matrix": user_category_matrix
}
joblib.dump(recommend_artifacts, 'recommend_model.pkl')
print(f"✅ Saved recommend_model.pkl")

# ─────────────────────────────────────────────────────────────────────────────
# STEP 6 — VERIFY EVERYTHING WORKS
# ─────────────────────────────────────────────────────────────────────────────

print("\n" + "=" * 60)
print("STEP 6: Verification...")
print("=" * 60)

# Test eco model
test_products = [
    ("Organic Cotton Hoodie", "HOODIE"),
    ("Plastic Sticker Pack", "STICKER"),
    ("Recycled Rubber Mousepad", "MOUSEPAD"),
    ("Bamboo Developer Mug", "MUG"),
]

eco_model = joblib.load('eco_model.pkl')
print("Eco Score predictions:")
for name, cat in test_products:
    text = f"{name} {cat}"
    score = eco_model.predict([text])[0]
    print(f"  '{name}' ({cat}) → ECO {score}/10")

# Test recommendation model
artifacts = joblib.load('recommend_model.pkl')
sim_matrix = artifacts['category_similarity']
print("\nRecommendation similarities (HOODIE buyers also like):")
hoodie_sim = sim_matrix['HOODIE'].sort_values(ascending=False)
for cat, score in hoodie_sim.items():
    print(f"  {cat}: {score:.3f}")

print("\n" + "=" * 60)
print("✅ ALL MODELS TRAINED SUCCESSFULLY!")
print(f"   eco_model.pkl       — TF-IDF + Random Forest")
print(f"   recommend_model.pkl — Cosine Similarity Matrix")
print(f"   carbon_scaler.pkl   — MinMaxScaler for carbon values")
print(f"   dataset/eco_dataset.csv    — {len(eco_df)} training samples")
print(f"   dataset/orders_dataset.csv — {len(orders_df)} interactions")
print("=" * 60)
print("\nNext step: run python app.py to start the AI service")
