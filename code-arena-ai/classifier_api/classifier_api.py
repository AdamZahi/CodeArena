"""FastAPI HTTP wrapper around the trained Big-O complexity classifier.

Exposes /classify, /classify-batch, and /health. All ML logic lives in
``../classifier/classifier.py`` — this module is a thin HTTP shim that the
Spring Boot backend calls to obtain a complexity label + score for code
submissions executed on Piston.
"""

from __future__ import annotations

import logging
import os
import sys
from typing import List, Optional

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel

logging.basicConfig(level=logging.INFO,
                    format="[%(asctime)s] %(levelname)s %(name)s: %(message)s")
log = logging.getLogger("classifier_api")

# Resolve sibling classifier/ folder relative to this file and put it on
# sys.path so ``classifier`` and ``complexity_map`` import as top-level
# modules. Hardcoding absolute paths is forbidden — everything is relative
# to __file__.
HERE = os.path.dirname(os.path.abspath(__file__))
CLASSIFIER_DIR = os.path.normpath(os.path.join(HERE, "..", "classifier"))
if CLASSIFIER_DIR not in sys.path:
    sys.path.insert(0, CLASSIFIER_DIR)

model_ready = False
_classify_complexity = None
_classify_batch = None
_complexity_to_score = None

try:
    from classifier import classify_complexity as _classify_complexity  # type: ignore
    from classifier import classify_batch as _classify_batch  # type: ignore
    from classifier import complexity_to_score as _complexity_to_score  # type: ignore
    model_ready = True
    log.info("Complexity classifier loaded from %s", CLASSIFIER_DIR)
except Exception as exc:  # noqa: BLE001 — surface every load failure
    log.error("Failed to load classifier artifacts from %s: %s", CLASSIFIER_DIR, exc)
    model_ready = False


# ── Schemas ──────────────────────────────────────────────────────────

class ClassifyRequest(BaseModel):
    source_code: str
    language: Optional[str] = None


class ClassifyResponse(BaseModel):
    label: Optional[str]
    display: Optional[str]
    score: float
    confidence: float
    all_scores: dict
    error: Optional[str] = None


class ClassifyBatchRequest(BaseModel):
    submissions: List[ClassifyRequest]


class ClassifyBatchResponse(BaseModel):
    results: List[ClassifyResponse]


class HealthResponse(BaseModel):
    status: str
    model_ready: bool


# ── App ──────────────────────────────────────────────────────────────

app = FastAPI(title="Code Arena — Complexity Classifier API", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=False,
    allow_methods=["*"],
    allow_headers=["*"],
)


def _wrap_result(result: dict) -> ClassifyResponse:
    label = result.get("label")
    score = float(_complexity_to_score(label)) if _complexity_to_score is not None else 0.0
    return ClassifyResponse(
        label=label,
        display=result.get("display"),
        score=score,
        confidence=float(result.get("confidence", 0.0)),
        all_scores=result.get("all_scores", {}),
        error=result.get("error"),
    )


@app.get("/health", response_model=HealthResponse)
def health() -> HealthResponse:
    return HealthResponse(status="ok", model_ready=model_ready)


@app.post("/classify", response_model=ClassifyResponse)
def classify(req: ClassifyRequest) -> ClassifyResponse:
    if not model_ready or _classify_complexity is None:
        raise HTTPException(status_code=503, detail="Complexity classifier is not loaded")
    try:
        result = _classify_complexity(req.source_code)
        return _wrap_result(result)
    except Exception as exc:  # noqa: BLE001 — surface as structured 200 error
        log.exception("classify_complexity failed")
        return ClassifyResponse(
            label=None,
            display=None,
            score=0.0,
            confidence=0.0,
            all_scores={},
            error=str(exc),
        )


@app.post("/classify-batch", response_model=ClassifyBatchResponse)
def classify_batch(req: ClassifyBatchRequest) -> ClassifyBatchResponse:
    if not model_ready or _classify_batch is None:
        raise HTTPException(status_code=503, detail="Complexity classifier is not loaded")
    try:
        codes = [s.source_code for s in req.submissions]
        results = _classify_batch(codes)
        return ClassifyBatchResponse(results=[_wrap_result(r) for r in results])
    except Exception as exc:  # noqa: BLE001
        log.exception("classify_batch failed")
        empty = ClassifyResponse(
            label=None, display=None, score=0.0, confidence=0.0,
            all_scores={}, error=str(exc),
        )
        return ClassifyBatchResponse(results=[empty for _ in req.submissions])
