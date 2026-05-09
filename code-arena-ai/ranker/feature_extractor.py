"""Feature extraction module for the Score Ranker Optimizer.

Converts raw source code + Piston execution result into a flat dictionary
of numeric features that the ML model can consume.
"""

import re
from typing import Optional

try:
    from radon.complexity import cc_visit
except ImportError:
    cc_visit = None


LANGUAGE_SPEED_FACTORS = {
    "c": 1.0, "c++": 1.0, "rust": 1.0, "go": 1.1,
    "java": 1.3, "kotlin": 1.3, "javascript": 1.5,
    "node": 1.5, "typescript": 1.5, "python": 2.0,
    "ruby": 2.0, "php": 1.8,
}


def safe_float(value) -> float:
    """Safely cast to float, return 9999.0 on any failure."""
    try:
        return float(value)
    except (TypeError, ValueError):
        return 9999.0


def detect_nested_loops(code: str) -> int:
    """Return the maximum loop nesting depth found in the code.

    Uses an indentation-based heuristic: tracks indent level when a
    ``for`` or ``while`` keyword starts a line and returns the maximum
    concurrent nesting depth reached.
    """
    max_depth = 0
    current_depth = 0
    indent_stack: list[int] = []

    for line in code.splitlines():
        stripped = line.lstrip()
        if not stripped:
            continue

        indent = len(line) - len(stripped)

        # Pop from stack while current indent is <= stack top
        while indent_stack and indent <= indent_stack[-1]:
            indent_stack.pop()
            current_depth -= 1

        if re.match(r'\b(for|while)\b', stripped):
            current_depth += 1
            indent_stack.append(indent)
            max_depth = max(max_depth, current_depth)

    return max_depth


def detect_recursion(code: str) -> bool:
    """Return True if any function in the code calls itself.

    Extracts function names via regex (def, void, int, function keywords)
    and checks if each name appears as a call after its own definition.
    """
    # Match common function definitions across languages
    patterns = [
        r'\bdef\s+(\w+)\s*\(',           # Python
        r'\bfunction\s+(\w+)\s*\(',       # JavaScript
        r'(?:void|int|long|double|float|string|String|boolean|bool|char|auto)\s+(\w+)\s*\(',  # C/Java/etc
        r'(?:public|private|protected|static)\s+\w+\s+(\w+)\s*\(',  # Java methods
    ]

    func_names = set()
    for pattern in patterns:
        func_names.update(re.findall(pattern, code))

    for name in func_names:
        # Find the definition position
        def_pattern = re.compile(r'\b(?:def|function|void|int|long|double|float|string|String|boolean|bool|char|auto)\s+'
                                 + re.escape(name) + r'\s*\(')
        match = def_pattern.search(code)
        if match:
            after_def = code[match.end():]
            # Check if the function name appears as a call after the definition
            call_pattern = re.compile(r'\b' + re.escape(name) + r'\s*\(')
            if call_pattern.search(after_def):
                return True

    return False


def extract_features(source_code: str, piston_result: dict, language: str,
                     total_tests: int, passed_tests: int) -> dict:
    """Extract numeric features from source code and Piston execution result.

    Returns a flat dictionary of features suitable for ML model consumption.
    """
    features: dict = {}

    # Group 1 — Execution metrics
    run_info = piston_result.get("run", piston_result)
    features["execution_time_ms"] = safe_float(run_info.get("time", 9999))
    features["memory_kb"] = safe_float(run_info.get("memory", 999999))
    features["exit_code"] = int(run_info.get("code", 1))
    features["tests_passed_ratio"] = passed_tests / max(total_tests, 1)

    # Group 2 — Code size
    lines = source_code.splitlines()
    non_empty = [l for l in lines if l.strip()]
    features["total_lines"] = len(lines)
    features["code_lines"] = len(non_empty)
    features["avg_line_length"] = (
        sum(len(l) for l in non_empty) / len(non_empty) if non_empty else 0.0
    )

    # Group 3 — Loop & recursion signals
    features["for_loop_count"] = len(re.findall(r'\bfor\b', source_code))
    features["while_loop_count"] = len(re.findall(r'\bwhile\b', source_code))
    features["nested_loop_depth"] = detect_nested_loops(source_code)
    features["recursion_detected"] = 1 if detect_recursion(source_code) else 0

    # Group 4 — Data structure usage
    features["uses_hashmap"] = 1 if re.search(
        r'\b(dict|HashMap|unordered_map|Map)\b|\{\}', source_code
    ) else 0
    features["uses_set"] = 1 if re.search(
        r'\b(set|HashSet|unordered_set|Set)\b', source_code
    ) else 0
    features["uses_sorting"] = 1 if re.search(
        r'\b(sort|sorted|Arrays\.sort|Collections\.sort)\b', source_code
    ) else 0

    # Group 5 — Cyclomatic complexity
    if language == "python" and cc_visit is not None:
        try:
            results = cc_visit(source_code)
            if results:
                complexities = [r.complexity for r in results]
                features["cyclomatic_complexity_avg"] = sum(complexities) / len(complexities)
                features["cyclomatic_complexity_max"] = max(complexities)
            else:
                features["cyclomatic_complexity_avg"] = 1.0
                features["cyclomatic_complexity_max"] = 1.0
        except Exception:
            features["cyclomatic_complexity_avg"] = 1.0
            features["cyclomatic_complexity_max"] = 1.0
    else:
        if_count = len(re.findall(r'\bif\b', source_code))
        estimated = 1 + features["for_loop_count"] + features["while_loop_count"] + if_count
        features["cyclomatic_complexity_avg"] = float(estimated)
        features["cyclomatic_complexity_max"] = float(estimated)

    # Group 6 — Language speed factor
    features["language_speed_factor"] = LANGUAGE_SPEED_FACTORS.get(
        language.lower(), 1.5
    )

    return features
