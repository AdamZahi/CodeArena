"""End-to-end integration tests for the Score Ranker pipeline."""

from ranker import score_submission, decide_winner


def test_efficient_vs_inefficient() -> None:
    """Test 1: Efficient O(n) hashmap solution should score higher than O(n²) brute force."""
    code_a = """
n = int(input())
nums = list(map(int, input().split()))
seen = {}
for i, num in enumerate(nums):
    if num in seen:
        print(seen[num], i)
    seen[num] = i
"""
    piston_result_a = {"time": 45, "memory": 8200, "code": 0}

    code_b = """
n = int(input())
nums = list(map(int, input().split()))
for i in range(n):
    for j in range(i+1, n):
        if nums[i] == nums[j]:
            print(i, j)
"""
    piston_result_b = {"time": 820, "memory": 42000, "code": 0}

    score_a = score_submission(code_a, piston_result_a, "python", 1, 1)
    score_b = score_submission(code_b, piston_result_b, "python", 1, 1)

    assert 0 <= score_a["score"] <= 100, f"Score A out of range: {score_a['score']}"
    assert 0 <= score_b["score"] <= 100, f"Score B out of range: {score_b['score']}"
    assert score_a["score"] > score_b["score"], (
        f"Efficient solution should score higher: A={score_a['score']} vs B={score_b['score']}"
    )

    winner = decide_winner(score_a, score_b)
    assert winner["winner"] == "A", f"Expected winner A, got {winner['winner']}"

    print(f"  Test 1 PASSED: A={score_a['score']}, B={score_b['score']}, "
          f"winner={winner['winner']}, margin={winner['margin']}")


def test_failed_submission() -> None:
    """Test 2: Failed submission (exit_code != 0) should score 0.0."""
    code = "print('hello')"
    piston_result_fail = {"time": 0, "memory": 0, "code": 1}

    score_fail = score_submission(code, piston_result_fail, "python", 1, 0)

    assert score_fail["score"] == 0.0, f"Failed submission should score 0.0, got {score_fail['score']}"

    print(f"  Test 2 PASSED: failed submission score={score_fail['score']}")


def test_draw_scenario() -> None:
    """Test 3: Two identical results should produce a draw."""
    code = "print(42)"
    piston_result = {"time": 100, "memory": 10000, "code": 0}

    score_x = score_submission(code, piston_result, "python", 1, 1)
    score_y = score_submission(code, piston_result, "python", 1, 1)

    result = decide_winner(score_x, score_y)
    assert result["winner"] == "draw", f"Expected draw, got {result['winner']}"
    assert result["margin"] == 0.0, f"Expected margin 0.0, got {result['margin']}"

    print(f"  Test 3 PASSED: draw scenario, scores={score_x['score']}/{score_y['score']}")


def main() -> None:
    """Run all tests."""
    test_efficient_vs_inefficient()
    test_failed_submission()
    test_draw_scenario()
    print("\n\u2705 All tests passed")


if __name__ == "__main__":
    main()
