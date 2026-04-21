"""
Builds a labeled dataset of source-code snippets annotated with their Big-O
time-complexity class, then writes a stratified 80/10/10 train/val/test split
to ``dataset/`` as CSV files.

The dataset always contains 48 built-in curated samples (8 per class * 6
classes) covering Python, JavaScript, and Java. If ``--codenet PATH`` is
provided, additional samples loaded from that directory are appended before
the split so the model can see real-world solutions.

Usage:
    python prepare_dataset.py
    python prepare_dataset.py --codenet /path/to/Project_CodeNet
"""

from __future__ import annotations

import argparse
import os
from pathlib import Path

import pandas as pd
from sklearn.model_selection import train_test_split

from complexity_map import COMPLEXITY_LABELS

OUTPUT_DIR = Path(__file__).resolve().parent / "dataset"


# ---------------------------------------------------------------------------
# Built-in curated samples. Eight per complexity class, mixed languages.
# ---------------------------------------------------------------------------

BUILTIN_SAMPLES: list[tuple[str, str]] = [
    # =====================================================================
    # O(1) — constant-time
    # =====================================================================
    (
        "O1",
        """def get_first(arr):
    if not arr:
        return None
    return arr[0]
""",
    ),
    (
        "O1",
        """def add(a, b):
    return a + b
""",
    ),
    (
        "O1",
        """def is_even(n):
    return n % 2 == 0
""",
    ),
    (
        "O1",
        """function swap(arr, i, j) {
    const tmp = arr[i];
    arr[i] = arr[j];
    arr[j] = tmp;
}
""",
    ),
    (
        "O1",
        """function getHead(node) {
    if (node === null) return null;
    return node.value;
}
""",
    ),
    (
        "O1",
        """public class Solution {
    public int square(int n) {
        return n * n;
    }
}
""",
    ),
    (
        "O1",
        """public class Solution {
    public boolean inRange(int x, int lo, int hi) {
        return x >= lo && x <= hi;
    }
}
""",
    ),
    (
        "O1",
        """def stack_push(stack, value):
    stack.append(value)
    return len(stack)
""",
    ),

    # =====================================================================
    # O(log n) — binary-search style halving
    # =====================================================================
    (
        "Ologn",
        """def binary_search(arr, target):
    lo, hi = 0, len(arr) - 1
    while lo <= hi:
        mid = (lo + hi) // 2
        if arr[mid] == target:
            return mid
        elif arr[mid] < target:
            lo = mid + 1
        else:
            hi = mid - 1
    return -1
""",
    ),
    (
        "Ologn",
        """def power_of_two(n):
    count = 0
    while n > 1:
        n //= 2
        count += 1
    return count
""",
    ),
    (
        "Ologn",
        """def int_log2(n):
    result = 0
    while n > 1:
        n >>= 1
        result += 1
    return result
""",
    ),
    (
        "Ologn",
        """function binarySearch(arr, target) {
    let lo = 0, hi = arr.length - 1;
    while (lo <= hi) {
        const mid = Math.floor((lo + hi) / 2);
        if (arr[mid] === target) return mid;
        if (arr[mid] < target) lo = mid + 1;
        else hi = mid - 1;
    }
    return -1;
}
""",
    ),
    (
        "Ologn",
        """function countDigits(n) {
    let count = 0;
    while (n > 0) {
        n = Math.floor(n / 10);
        count++;
    }
    return count;
}
""",
    ),
    (
        "Ologn",
        """public class Solution {
    public int binarySearch(int[] arr, int target) {
        int lo = 0, hi = arr.length - 1;
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            if (arr[mid] == target) return mid;
            if (arr[mid] < target) lo = mid + 1;
            else hi = mid - 1;
        }
        return -1;
    }
}
""",
    ),
    (
        "Ologn",
        """public class Solution {
    public int log2(int n) {
        int r = 0;
        while (n > 1) { n >>= 1; r++; }
        return r;
    }
}
""",
    ),
    (
        "Ologn",
        """def gcd(a, b):
    while b != 0:
        a, b = b, a % b
    return a
""",
    ),

    # =====================================================================
    # O(n) — single linear pass
    # =====================================================================
    (
        "On",
        """def sum_list(arr):
    total = 0
    for x in arr:
        total += x
    return total
""",
    ),
    (
        "On",
        """def find_max(arr):
    best = arr[0]
    for x in arr[1:]:
        if x > best:
            best = x
    return best
""",
    ),
    (
        "On",
        """def reverse_list(arr):
    out = []
    for i in range(len(arr) - 1, -1, -1):
        out.append(arr[i])
    return out
""",
    ),
    (
        "On",
        """function countOccurrences(arr, target) {
    let count = 0;
    for (let i = 0; i < arr.length; i++) {
        if (arr[i] === target) count++;
    }
    return count;
}
""",
    ),
    (
        "On",
        """function joinWithSpaces(words) {
    let result = '';
    for (let i = 0; i < words.length; i++) {
        result += words[i];
        if (i < words.length - 1) result += ' ';
    }
    return result;
}
""",
    ),
    (
        "On",
        """public class Solution {
    public int sum(int[] arr) {
        int total = 0;
        for (int x : arr) total += x;
        return total;
    }
}
""",
    ),
    (
        "On",
        """public class Solution {
    public boolean contains(int[] arr, int target) {
        for (int x : arr) if (x == target) return true;
        return false;
    }
}
""",
    ),
    (
        "On",
        """def linear_search(arr, target):
    for i, value in enumerate(arr):
        if value == target:
            return i
    return -1
""",
    ),

    # =====================================================================
    # O(n log n) — divide-and-conquer / sort-then-iterate
    # =====================================================================
    (
        "Onlogn",
        """def merge_sort(arr):
    if len(arr) <= 1:
        return arr
    mid = len(arr) // 2
    left = merge_sort(arr[:mid])
    right = merge_sort(arr[mid:])
    merged = []
    i = j = 0
    while i < len(left) and j < len(right):
        if left[i] <= right[j]:
            merged.append(left[i]); i += 1
        else:
            merged.append(right[j]); j += 1
    merged.extend(left[i:]); merged.extend(right[j:])
    return merged
""",
    ),
    (
        "Onlogn",
        """def has_duplicate_sorted(arr):
    sorted_arr = sorted(arr)
    for i in range(1, len(sorted_arr)):
        if sorted_arr[i] == sorted_arr[i-1]:
            return True
    return False
""",
    ),
    (
        "Onlogn",
        """def k_largest(arr, k):
    arr_sorted = sorted(arr, reverse=True)
    return arr_sorted[:k]
""",
    ),
    (
        "Onlogn",
        """function quickSort(arr) {
    if (arr.length <= 1) return arr;
    const pivot = arr[Math.floor(arr.length / 2)];
    const left = arr.filter(x => x < pivot);
    const mid = arr.filter(x => x === pivot);
    const right = arr.filter(x => x > pivot);
    return [...quickSort(left), ...mid, ...quickSort(right)];
}
""",
    ),
    (
        "Onlogn",
        """function sortAndSum(arr) {
    arr.sort((a, b) => a - b);
    let total = 0;
    for (let i = 0; i < arr.length; i++) total += arr[i];
    return total;
}
""",
    ),
    (
        "Onlogn",
        """import java.util.Arrays;
public class Solution {
    public int[] sortAsc(int[] arr) {
        Arrays.sort(arr);
        return arr;
    }
}
""",
    ),
    (
        "Onlogn",
        """import java.util.Arrays;
public class Solution {
    public boolean hasDuplicate(int[] arr) {
        Arrays.sort(arr);
        for (int i = 1; i < arr.length; i++) {
            if (arr[i] == arr[i-1]) return true;
        }
        return false;
    }
}
""",
    ),
    (
        "Onlogn",
        """def heap_sort(arr):
    import heapq
    h = list(arr)
    heapq.heapify(h)
    out = []
    while h:
        out.append(heapq.heappop(h))
    return out
""",
    ),

    # =====================================================================
    # O(n^2) — nested linear loops over the same input
    # =====================================================================
    (
        "On2",
        """def bubble_sort(arr):
    n = len(arr)
    for i in range(n):
        for j in range(0, n - i - 1):
            if arr[j] > arr[j+1]:
                arr[j], arr[j+1] = arr[j+1], arr[j]
    return arr
""",
    ),
    (
        "On2",
        """def has_pair_with_sum(arr, target):
    for i in range(len(arr)):
        for j in range(i+1, len(arr)):
            if arr[i] + arr[j] == target:
                return True
    return False
""",
    ),
    (
        "On2",
        """def all_pairs(arr):
    pairs = []
    for i in range(len(arr)):
        for j in range(len(arr)):
            pairs.append((arr[i], arr[j]))
    return pairs
""",
    ),
    (
        "On2",
        """function selectionSort(arr) {
    for (let i = 0; i < arr.length; i++) {
        let minIdx = i;
        for (let j = i + 1; j < arr.length; j++) {
            if (arr[j] < arr[minIdx]) minIdx = j;
        }
        const tmp = arr[i]; arr[i] = arr[minIdx]; arr[minIdx] = tmp;
    }
    return arr;
}
""",
    ),
    (
        "On2",
        """function insertionSort(arr) {
    for (let i = 1; i < arr.length; i++) {
        let key = arr[i];
        let j = i - 1;
        while (j >= 0 && arr[j] > key) {
            arr[j+1] = arr[j];
            j--;
        }
        arr[j+1] = key;
    }
    return arr;
}
""",
    ),
    (
        "On2",
        """public class Solution {
    public int[] twoSumBrute(int[] nums, int target) {
        for (int i = 0; i < nums.length; i++) {
            for (int j = i+1; j < nums.length; j++) {
                if (nums[i] + nums[j] == target) return new int[]{i, j};
            }
        }
        return new int[]{-1, -1};
    }
}
""",
    ),
    (
        "On2",
        """public class Solution {
    public void bubbleSort(int[] arr) {
        for (int i = 0; i < arr.length - 1; i++) {
            for (int j = 0; j < arr.length - 1 - i; j++) {
                if (arr[j] > arr[j+1]) {
                    int t = arr[j]; arr[j] = arr[j+1]; arr[j+1] = t;
                }
            }
        }
    }
}
""",
    ),
    (
        "On2",
        """def matrix_sum(mat):
    total = 0
    for row in mat:
        for value in row:
            total += value
    return total
""",
    ),

    # =====================================================================
    # O(2^n) — exponential recursion / subset enumeration
    # =====================================================================
    (
        "O2n",
        """def fib(n):
    if n < 2:
        return n
    return fib(n-1) + fib(n-2)
""",
    ),
    (
        "O2n",
        """def subsets(arr):
    if not arr:
        return [[]]
    rest = subsets(arr[1:])
    return rest + [[arr[0]] + s for s in rest]
""",
    ),
    (
        "O2n",
        """def hanoi(n, src, dst, tmp):
    if n == 0:
        return
    hanoi(n-1, src, tmp, dst)
    hanoi(n-1, tmp, dst, src)
""",
    ),
    (
        "O2n",
        """function fib(n) {
    if (n < 2) return n;
    return fib(n - 1) + fib(n - 2);
}
""",
    ),
    (
        "O2n",
        """function powerSet(arr) {
    if (arr.length === 0) return [[]];
    const rest = powerSet(arr.slice(1));
    return rest.concat(rest.map(s => [arr[0], ...s]));
}
""",
    ),
    (
        "O2n",
        """public class Solution {
    public int fib(int n) {
        if (n < 2) return n;
        return fib(n - 1) + fib(n - 2);
    }
}
""",
    ),
    (
        "O2n",
        """public class Solution {
    public int countSubsets(int[] arr, int idx) {
        if (idx == arr.length) return 1;
        return countSubsets(arr, idx + 1) + countSubsets(arr, idx + 1);
    }
}
""",
    ),
    (
        "O2n",
        """def climb_stairs_naive(n):
    if n <= 1:
        return 1
    return climb_stairs_naive(n-1) + climb_stairs_naive(n-2)
""",
    ),
]


# ---------------------------------------------------------------------------
# Optional Project CodeNet loader
# ---------------------------------------------------------------------------

def load_codenet_samples(codenet_path: str) -> list[tuple[str, str]]:
    """
    Best-effort loader for additional labeled samples placed under
    ``<codenet_path>/labeled/<LABEL>/`` where ``<LABEL>`` is one of the keys
    in :data:`complexity_map.COMPLEXITY_LABELS`. Each file under that folder
    contributes one sample. Missing folders are silently skipped.
    """
    root = Path(codenet_path) / "labeled"
    if not root.is_dir():
        print(f"[codenet] No labeled/ directory at {root}, skipping")
        return []

    extra: list[tuple[str, str]] = []
    for label in COMPLEXITY_LABELS:
        folder = root / label
        if not folder.is_dir():
            continue
        for path in folder.rglob("*"):
            if not path.is_file():
                continue
            try:
                code = path.read_text(encoding="utf-8", errors="ignore")
            except OSError:
                continue
            code = code.strip()
            if code:
                extra.append((label, code))
    print(f"[codenet] Loaded {len(extra)} additional samples")
    return extra


# ---------------------------------------------------------------------------
# Split + persist
# ---------------------------------------------------------------------------

def build_dataframe(samples: list[tuple[str, str]]) -> pd.DataFrame:
    rows = []
    for label, code in samples:
        if label not in COMPLEXITY_LABELS:
            raise ValueError(f"Unknown complexity label: {label!r}")
        rows.append({"code": code, "label": label, "label_id": COMPLEXITY_LABELS[label]})
    return pd.DataFrame(rows)


def stratified_split(df: pd.DataFrame) -> tuple[pd.DataFrame, pd.DataFrame, pd.DataFrame]:
    train_df, temp_df = train_test_split(
        df, test_size=0.2, stratify=df["label_id"], random_state=42
    )
    val_df, test_df = train_test_split(
        temp_df, test_size=0.5, stratify=temp_df["label_id"], random_state=42
    )
    return train_df, val_df, test_df


def main() -> None:
    parser = argparse.ArgumentParser(description="Build complexity-classifier dataset")
    parser.add_argument(
        "--codenet",
        type=str,
        default=None,
        help="Optional path to a Project CodeNet root containing labeled/<LABEL>/ dirs",
    )
    args = parser.parse_args()

    samples = list(BUILTIN_SAMPLES)
    if args.codenet:
        samples.extend(load_codenet_samples(args.codenet))

    df = build_dataframe(samples)
    print(f"Total samples: {len(df)}")
    print("Per-class counts:")
    print(df["label"].value_counts().sort_index())

    train_df, val_df, test_df = stratified_split(df)
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

    train_df.to_csv(OUTPUT_DIR / "train.csv", index=False)
    val_df.to_csv(OUTPUT_DIR / "val.csv", index=False)
    test_df.to_csv(OUTPUT_DIR / "test.csv", index=False)

    print(f"\nWrote splits to {OUTPUT_DIR}")
    print(f"  train: {len(train_df)}")
    print(f"  val:   {len(val_df)}")
    print(f"  test:  {len(test_df)}")


if __name__ == "__main__":
    main()
