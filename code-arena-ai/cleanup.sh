#!/bin/bash
# cleanup.sh — Remove all legacy/deprecated files from code-arena-ai

echo "🗑️  Cleaning up legacy files..."

# Legacy training scripts
rm -f gpt_model.py
rm -f train.py
rm -f train_gpt.py
rm -f train_gen.py
rm -f train_hint_model.py

# Legacy data utilities
rm -f data_preparation.py
rm -f dataset_generator.py
rm -f csv_to_sql.py

# Legacy model artifacts
rm -f models/hint_classifier.pth
rm -f models/hint_model.pkl
rm -f models/hint_meta.pkl
rm -f models/hint_mapping.pkl

echo "✅ Cleanup complete. Legacy files removed."
echo ""
echo "Remaining production files:"
ls -la app.py train_colab.py generate_dataset.py merge_data.py
echo ""
echo "Remaining model artifacts:"
ls -la models/nano_gpt.pth models/tokenizer.pkl models/retriever.pkl
