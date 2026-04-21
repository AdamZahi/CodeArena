"""
Smoke tests for the trained complexity classifier.

Run after :mod:`train_classifier` has produced ``model/best/``. The script
exits non-zero if fewer than 3 of the 5 hardcoded sanity checks pass.
"""

from __future__ import annotations

import sys

from classifier import classify_batch, classify_complexity, complexity_to_score
from complexity_map import COMPLEXITY_LABELS, LABEL_DISPLAY

TEST_CASES = [
    (
        "O1",
        """def head(arr):
    if not arr:
        return None
    return arr[0]
""",
    ),
    (
        "On",
        """def total(arr):
    s = 0
    for x in arr:
        s += x
    return s
""",
    ),
    (
        "On2",
        """def all_pairs_equal(arr):
    for i in range(len(arr)):
        for j in range(len(arr)):
            if arr[i] == arr[j] and i != j:
                return True
    return False
""",
    ),
    (
        "Ologn",
        """def bsearch(arr, target):
    lo, hi = 0, len(arr) - 1
    while lo <= hi:
        mid = (lo + hi) // 2
        if arr[mid] == target:
            return mid
        if arr[mid] < target:
            lo = mid + 1
        else:
            hi = mid - 1
    return -1
""",
    ),
    (
        "O2n",
        """def fib(n):
    if n < 2:
        return n
    return fib(n - 1) + fib(n - 2)
""",
    ),
]


def test_single_predictions() -> int:
    print("=== Single-snippet predictions ===")
    passes = 0
    for expected, code in TEST_CASES:
        result = classify_complexity(code)
        predicted = result["label"]
        status = "OK" if predicted == expected else "FAIL"
        if predicted == expected:
            passes += 1
        print(
            f"[{status}] expected={expected:<7} predicted={str(predicted):<7} "
            f"display={result['display']!s:<10} confidence={result['confidence']:.3f}"
        )
        if result.get("error"):
            print(f"        error: {result['error']}")
    print(f"\nPassed {passes}/{len(TEST_CASES)} sanity checks")
    return passes


def test_batch_prediction() -> None:
    print("\n=== Batch prediction ===")
    snippets = [code for _, code in TEST_CASES]
    results = classify_batch(snippets)
    assert len(results) == len(TEST_CASES), "Batch length must match input length"
    for (expected, _), result in zip(TEST_CASES, results):
        print(
            f"expected={expected:<7} predicted={str(result['label']):<7} "
            f"confidence={result['confidence']:.3f}"
        )


def test_score_mapping() -> None:
    print("\n=== Score mapping ===")
    expected_scores = {
        "O1": 100.0,
        "Ologn": 90.0,
        "On": 75.0,
        "Onlogn": 60.0,
        "On2": 35.0,
        "O2n": 10.0,
    }
    for label in COMPLEXITY_LABELS:
        score = complexity_to_score(label)
        assert score == expected_scores[label], f"{label} score mismatch: {score}"
        print(f"{label:<7} ({LABEL_DISPLAY[label]:<10}) -> {score}")
    assert complexity_to_score(None) == 0.0
    assert complexity_to_score("does_not_exist") == 0.0
    print("Unknown / None labels correctly map to 0.0")


def test_empty_input() -> None:
    print("\n=== Empty input handling ===")
    result = classify_complexity("")
    assert result["label"] is None
    assert result["error"] is not None
    print(f"Empty string -> error: {result['error']}")


# ---------------------------------------------------------------------------
# Integration sketch — illustrative only, kept commented so it doesn't run.
# Shows how the Battle backend would combine the ranker (see ../ranker/) and
# the complexity classifier into one final score per submission.
# ---------------------------------------------------------------------------
#
# from ranker.scorer import score_submission       # hypothetical ranker entrypoint
# from classifier import classify_complexity, complexity_to_score
#
# def evaluate_submission(code: str, runtime_metrics: dict) -> dict:
#     ranker_score = score_submission(runtime_metrics)            # 0..100
#     complexity = classify_complexity(code)                      # full dict
#     complexity_score = complexity_to_score(complexity["label"]) # 0..100
#     final = 0.6 * ranker_score + 0.4 * complexity_score
#     return {
#         "final_score": final,
#         "ranker_score": ranker_score,
#         "complexity_label": complexity["label"],
#         "complexity_display": complexity["display"],
#         "complexity_score": complexity_score,
#         "complexity_confidence": complexity["confidence"],
#     }


def main() -> int:
    passes = test_single_predictions()
    test_batch_prediction()
    test_score_mapping()
    test_empty_input()
    if passes < 3:
        print(f"\nFAILURE: only {passes}/5 sanity checks passed (need >= 3)")
        return 1
    print(f"\nSUCCESS: {passes}/5 sanity checks passed")
    return 0


if __name__ == "__main__":
    sys.exit(main())
