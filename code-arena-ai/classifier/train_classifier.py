"""
Fine-tunes ``microsoft/codebert-base`` for six-way Big-O complexity
classification using the CSV splits produced by :mod:`prepare_dataset`.

Outputs
-------
``model/best/``  - tokenizer + weights at the epoch with the best val accuracy
``model/final/`` - tokenizer + weights from the final epoch
"""

from __future__ import annotations

import os
import random
from pathlib import Path

import numpy as np
import pandas as pd
import torch
from sklearn.metrics import accuracy_score, classification_report, confusion_matrix
from torch.optim import AdamW
from torch.utils.data import DataLoader, Dataset
from tqdm import tqdm
from transformers import AutoModelForSequenceClassification, AutoTokenizer, get_linear_schedule_with_warmup

from complexity_map import COMPLEXITY_LABELS, ID_TO_LABEL, NUM_LABELS

MODEL_NAME = "microsoft/codebert-base"
MAX_LENGTH = 512
BATCH_SIZE = 8
EPOCHS = 8
LEARNING_RATE = 2e-5
WARMUP_STEPS = 50

ROOT = Path(__file__).resolve().parent
DATASET_DIR = ROOT / "dataset"
MODEL_DIR = ROOT / "model"
BEST_DIR = MODEL_DIR / "best"
FINAL_DIR = MODEL_DIR / "final"


def set_seed(seed: int = 42) -> None:
    random.seed(seed)
    np.random.seed(seed)
    torch.manual_seed(seed)
    if torch.cuda.is_available():
        torch.cuda.manual_seed_all(seed)


class CodeDataset(Dataset):
    def __init__(self, dataframe: pd.DataFrame, tokenizer, max_length: int = MAX_LENGTH):
        self.codes = dataframe["code"].tolist()
        self.labels = dataframe["label_id"].tolist()
        self.tokenizer = tokenizer
        self.max_length = max_length

    def __len__(self) -> int:
        return len(self.codes)

    def __getitem__(self, idx: int):
        encoding = self.tokenizer(
            self.codes[idx],
            truncation=True,
            padding="max_length",
            max_length=self.max_length,
            return_tensors="pt",
        )
        return {
            "input_ids": encoding["input_ids"].squeeze(0),
            "attention_mask": encoding["attention_mask"].squeeze(0),
            "labels": torch.tensor(self.labels[idx], dtype=torch.long),
        }


def evaluate(model, loader, device) -> tuple[float, list[int], list[int]]:
    model.eval()
    preds: list[int] = []
    truths: list[int] = []
    with torch.no_grad():
        for batch in loader:
            input_ids = batch["input_ids"].to(device)
            attention_mask = batch["attention_mask"].to(device)
            labels = batch["labels"].to(device)
            outputs = model(input_ids=input_ids, attention_mask=attention_mask)
            batch_preds = outputs.logits.argmax(dim=-1).cpu().tolist()
            preds.extend(batch_preds)
            truths.extend(labels.cpu().tolist())
    acc = accuracy_score(truths, preds)
    return acc, preds, truths


def main() -> None:
    set_seed(42)

    if not DATASET_DIR.is_dir():
        raise FileNotFoundError(
            f"{DATASET_DIR} does not exist. Run `python prepare_dataset.py` first."
        )

    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    if device.type == "cpu":
        print(
            "WARNING: CUDA is not available. Training on CPU will be slow "
            "(expect several minutes per epoch even on the small built-in dataset)."
        )
    else:
        print(f"Using device: {torch.cuda.get_device_name(0)}")

    train_df = pd.read_csv(DATASET_DIR / "train.csv")
    val_df = pd.read_csv(DATASET_DIR / "val.csv")
    test_df = pd.read_csv(DATASET_DIR / "test.csv")
    print(f"Loaded splits — train: {len(train_df)}, val: {len(val_df)}, test: {len(test_df)}")

    tokenizer = AutoTokenizer.from_pretrained(MODEL_NAME)
    model = AutoModelForSequenceClassification.from_pretrained(
        MODEL_NAME, num_labels=NUM_LABELS
    ).to(device)

    train_loader = DataLoader(CodeDataset(train_df, tokenizer), batch_size=BATCH_SIZE, shuffle=True)
    val_loader = DataLoader(CodeDataset(val_df, tokenizer), batch_size=BATCH_SIZE)
    test_loader = DataLoader(CodeDataset(test_df, tokenizer), batch_size=BATCH_SIZE)

    optimizer = AdamW(model.parameters(), lr=LEARNING_RATE)
    total_steps = max(1, len(train_loader) * EPOCHS)
    scheduler = get_linear_schedule_with_warmup(
        optimizer, num_warmup_steps=WARMUP_STEPS, num_training_steps=total_steps
    )

    BEST_DIR.mkdir(parents=True, exist_ok=True)
    FINAL_DIR.mkdir(parents=True, exist_ok=True)

    best_val_acc = -1.0
    label_names = [ID_TO_LABEL[i] for i in range(NUM_LABELS)]

    for epoch in range(1, EPOCHS + 1):
        model.train()
        running_loss = 0.0
        progress = tqdm(train_loader, desc=f"Epoch {epoch}/{EPOCHS}")
        for batch in progress:
            optimizer.zero_grad()
            input_ids = batch["input_ids"].to(device)
            attention_mask = batch["attention_mask"].to(device)
            labels = batch["labels"].to(device)
            outputs = model(input_ids=input_ids, attention_mask=attention_mask, labels=labels)
            loss = outputs.loss
            loss.backward()
            optimizer.step()
            scheduler.step()
            running_loss += loss.item()
            progress.set_postfix(loss=f"{loss.item():.4f}")

        avg_loss = running_loss / max(1, len(train_loader))
        val_acc, val_preds, val_truths = evaluate(model, val_loader, device)
        print(f"\nEpoch {epoch}: train_loss={avg_loss:.4f}  val_acc={val_acc:.4f}")
        print(
            classification_report(
                val_truths,
                val_preds,
                labels=list(range(NUM_LABELS)),
                target_names=label_names,
                zero_division=0,
            )
        )

        if val_acc > best_val_acc:
            best_val_acc = val_acc
            model.save_pretrained(BEST_DIR)
            tokenizer.save_pretrained(BEST_DIR)
            print(f"  -> New best model (val_acc={val_acc:.4f}) saved to {BEST_DIR}")

    model.save_pretrained(FINAL_DIR)
    tokenizer.save_pretrained(FINAL_DIR)
    print(f"\nFinal model saved to {FINAL_DIR}")
    print(f"Best val accuracy across run: {best_val_acc:.4f}")

    test_acc, test_preds, test_truths = evaluate(model, test_loader, device)
    print(f"\nTest accuracy (final-epoch weights): {test_acc:.4f}")
    print(
        classification_report(
            test_truths,
            test_preds,
            labels=list(range(NUM_LABELS)),
            target_names=label_names,
            zero_division=0,
        )
    )
    print("Confusion matrix (rows=truth, cols=prediction):")
    print(pd.DataFrame(
        confusion_matrix(test_truths, test_preds, labels=list(range(NUM_LABELS))),
        index=label_names,
        columns=label_names,
    ))


if __name__ == "__main__":
    main()
