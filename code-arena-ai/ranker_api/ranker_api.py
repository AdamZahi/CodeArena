"""FastAPI HTTP wrapper around the trained Score Ranker model.

Exposes /score, /compare, and /health. All ML logic lives in
``../ranker/ranker.py`` — this module is purely an HTTP shim that the
Spring Boot backend calls to obtain optimization scores for code
submissions executed on Piston.
"""

from __future__ import annotations

import logging
import os
import sys
from typing import Optional

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel

logging.basicConfig(level=logging.INFO,
                    format="[%(asctime)s] %(levelname)s %(name)s: %(message)s")
log = logging.getLogger("ranker_api")

# Resolve sibling ranker/ folder relative to this file and put it on sys.path
# so that ``ranker`` and ``feature_extractor`` can be imported as top-level
# modules. Hardcoding absolute paths is forbidden — everything is relative
# to __file__.
HERE = os.path.dirname(os.path.abspath(__file__))
RANKER_DIR = os.path.normpath(os.path.join(HERE, "..", "ranker"))
if RANKER_DIR not in sys.path:
    sys.path.insert(0, RANKER_DIR)

model_ready = False
_score_submission = None
_decide_winner = None

try:
    from ranker import score_submission as _score_submission  # type: ignore
    from ranker import decide_winner as _decide_winner  # type: ignore
    model_ready = True
    log.info("Ranker model loaded from %s", RANKER_DIR)
except Exception as exc:  # noqa: BLE001 — surface every load failure
    log.error("Failed to load ranker artifacts from %s: %s", RANKER_DIR, exc)
    model_ready = False


# ── Schemas ──────────────────────────────────────────────────────────

class ScoreRequest(BaseModel):
    source_code: str
    language: str
    time_ms: float = 0.0
    memory_kb: float = 0.0
    exit_code: int = 0
    total_tests: int = 1
    passed_tests: int = 0


class Breakdown(BaseModel):
    time_ms: float
    memory_kb: float
    tests_ratio: float


class ScoreResponse(BaseModel):
    score: float
    breakdown: Breakdown
    error: Optional[str] = None


class CompareRequest(BaseModel):
    player_a: ScoreRequest
    player_b: ScoreRequest


class CompareResponse(BaseModel):
    player_a_score: float
    player_b_score: float
    winner: str  # "A" | "B" | "draw"
    margin: float
    player_a_breakdown: Breakdown
    player_b_breakdown: Breakdown
    error: Optional[str] = None


class HealthResponse(BaseModel):
    status: str
    model_ready: bool


# ── App ──────────────────────────────────────────────────────────────

app = FastAPI(title="Code Arena — Score Ranker API", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=False,
    allow_methods=["*"],
    allow_headers=["*"],
)


def _piston_result(req: ScoreRequest) -> dict:
    """Build the piston_result dict shape expected by ranker.score_submission."""
    return {
        "run": {
            "time": req.time_ms,
            "memory": req.memory_kb,
            "code": req.exit_code,
        }
    }


def _score_one(req: ScoreRequest) -> ScoreResponse:
    if not model_ready or _score_submission is None:
        raise HTTPException(status_code=503, detail="Ranker model is not loaded")
    try:
        result = _score_submission(
            source_code=req.source_code,
            piston_result=_piston_result(req),
            language=req.language,
            total_tests=max(req.total_tests, 1),
            passed_tests=max(req.passed_tests, 0),
        )
        return ScoreResponse(
            score=float(result.get("score", 0.0)),
            breakdown=Breakdown(**result.get("breakdown", {
                "time_ms": 0.0, "memory_kb": 0.0, "tests_ratio": 0.0,
            })),
            error=result.get("error"),
        )
    except Exception as exc:  # noqa: BLE001 — surface as structured 200 error
        log.exception("score_submission failed")
        return ScoreResponse(
            score=0.0,
            breakdown=Breakdown(time_ms=0.0, memory_kb=0.0, tests_ratio=0.0),
            error=str(exc),
        )


@app.get("/health", response_model=HealthResponse)
def health() -> HealthResponse:
    return HealthResponse(status="ok", model_ready=model_ready)


@app.post("/score", response_model=ScoreResponse)
def score(req: ScoreRequest) -> ScoreResponse:
    return _score_one(req)


@app.post("/compare", response_model=CompareResponse)
def compare(req: CompareRequest) -> CompareResponse:
    if not model_ready or _decide_winner is None:
        raise HTTPException(status_code=503, detail="Ranker model is not loaded")

    a = _score_one(req.player_a)
    b = _score_one(req.player_b)

    decision = _decide_winner(
        {"score": a.score},
        {"score": b.score},
    )

    err = a.error or b.error
    return CompareResponse(
        player_a_score=a.score,
        player_b_score=b.score,
        winner=str(decision.get("winner", "draw")),
        margin=float(decision.get("margin", 0.0)),
        player_a_breakdown=a.breakdown,
        player_b_breakdown=b.breakdown,
        error=err,
    )
