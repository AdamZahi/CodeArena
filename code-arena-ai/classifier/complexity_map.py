"""
Single source of truth for the six Big-O complexity classes recognized by the
classifier. All other modules in this package must import from here so that
training, inference, and tests cannot drift apart.
"""

COMPLEXITY_LABELS = {
    "O1": 0,
    "Ologn": 1,
    "On": 2,
    "Onlogn": 3,
    "On2": 4,
    "O2n": 5,
}

ID_TO_LABEL = {idx: label for label, idx in COMPLEXITY_LABELS.items()}

LABEL_DISPLAY = {
    "O1": "O(1)",
    "Ologn": "O(log n)",
    "On": "O(n)",
    "Onlogn": "O(n log n)",
    "On2": "O(n^2)",
    "O2n": "O(2^n)",
}

NUM_LABELS = len(COMPLEXITY_LABELS)
