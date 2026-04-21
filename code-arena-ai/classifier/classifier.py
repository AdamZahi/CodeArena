"""
Inference API for the Big-O complexity classifier.

The fine-tuned weights produced by :mod:`train_classifier` are loaded once at
module import time. Callers use :func:`classify_complexity` for a single
snippet, :func:`classify_batch` for many at once, and
:func:`complexity_to_score` to map a predicted label to the score used by the
Battle scoring pipeline.
"""

from __future__ import annotations

from pathlib import Path
from typing import Iterable

import torch
import torch.nn.functional as F
from transformers import AutoModelForSequenceClassification, AutoTokenizer

from complexity_map import COMPLEXITY_LABELS, ID_TO_LABEL, LABEL_DISPLAY, NUM_LABELS

MAX_LENGTH = 512

ROOT = Path(__file__).resolve().parent
MODEL_PATH = ROOT / "model" / "best"

COMPLEXITY_SCORES: dict[str, float] = {
    "O1": 100.0,
    "Ologn": 90.0,
    "On": 75.0,
    "Onlogn": 60.0,
    "On2": 35.0,
    "O2n": 10.0,
}


def _load_model():
    if not MODEL_PATH.is_dir():
        raise FileNotFoundError("Model not found. Run train_classifier.py first.")
    tokenizer = AutoTokenizer.from_pretrained(str(MODEL_PATH))
    model = AutoModelForSequenceClassification.from_pretrained(str(MODEL_PATH))
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    model.to(device)
    model.eval()
    return tokenizer, model, device


_TOKENIZER, _MODEL, _DEVICE = _load_model()


def _predict(codes: list[str]) -> list[dict]:
    encoding = _TOKENIZER(
        codes,
        truncation=True,
        padding=True,
        max_length=MAX_LENGTH,
        return_tensors="pt",
    ).to(_DEVICE)

    with torch.no_grad():
        logits = _MODEL(**encoding).logits
        probs = F.softmax(logits, dim=-1).cpu()

    results: list[dict] = []
    for row in probs:
        pred_id = int(torch.argmax(row).item())
        label = ID_TO_LABEL[pred_id]
        all_scores = {ID_TO_LABEL[i]: float(row[i].item()) for i in range(NUM_LABELS)}
        results.append({
            "label": label,
            "display": LABEL_DISPLAY[label],
            "confidence": float(row[pred_id].item()),
            "all_scores": all_scores,
            "error": None,
        })
    return results


def classify_complexity(source_code: str) -> dict:
    """
    Predict the Big-O class of a single code snippet.

    Returns a dict with keys: ``label``, ``display``, ``confidence``,
    ``all_scores`` and ``error`` (``None`` on success). On any inference
    failure, an ``error`` message is returned and ``label`` is ``None``.
    """
    if source_code is None or not str(source_code).strip():
        return {
            "label": None,
            "display": None,
            "confidence": 0.0,
            "all_scores": {label: 0.0 for label in COMPLEXITY_LABELS},
            "error": "Empty source code",
        }
    try:
        return _predict([source_code])[0]
    except Exception as exc:
        return {
            "label": None,
            "display": None,
            "confidence": 0.0,
            "all_scores": {label: 0.0 for label in COMPLEXITY_LABELS},
            "error": f"{type(exc).__name__}: {exc}",
        }


def classify_batch(submissions: Iterable[str]) -> list[dict]:
    """Vectorized version of :func:`classify_complexity` for many snippets."""
    items = list(submissions)
    if not items:
        return []
    valid_indices = [i for i, code in enumerate(items) if code and str(code).strip()]
    if not valid_indices:
        return [classify_complexity(code) for code in items]
    try:
        valid_codes = [items[i] for i in valid_indices]
        valid_results = _predict(valid_codes)
    except Exception as exc:
        message = f"{type(exc).__name__}: {exc}"
        return [
            {
                "label": None,
                "display": None,
                "confidence": 0.0,
                "all_scores": {label: 0.0 for label in COMPLEXITY_LABELS},
                "error": message,
            }
            for _ in items
        ]

    output: list[dict] = []
    valid_iter = iter(valid_results)
    for idx, code in enumerate(items):
        if idx in valid_indices:
            output.append(next(valid_iter))
        else:
            output.append(classify_complexity(code))
    return output


def complexity_to_score(label: str | None) -> float:
    """Map a predicted complexity label to the Battle-scoring complexity score."""
    if label is None:
        return 0.0
    return COMPLEXITY_SCORES.get(label, 0.0)
