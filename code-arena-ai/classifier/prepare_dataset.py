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
        """def get_last(arr):
    if len(arr) == 0:
        return None
    return arr[-1]
""",
    ),
    (
        "O1",
        """def dict_lookup(d, key, default=None):
    if key in d:
        return d[key]
    return default
""",
    ),
    (
        "O1",
        """def swap_pair(a, b):
    a, b = b, a
    return a, b
""",
    ),
    (
        "O1",
        """def absolute(n):
    if n < 0:
        return -n
    return n
""",
    ),
    (
        "O1",
        """def triangle_area(base, height):
    return (base * height) / 2
""",
    ),
    (
        "O1",
        """def matrix_cell(mat, i, j):
    return mat[i][j]
""",
    ),
    (
        "O1",
        """def check_bit(n, pos):
    return (n >> pos) & 1 == 1
""",
    ),
    (
        "O1",
        """def is_empty(container):
    return len(container) == 0
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
        """function maxOfTwo(a, b) {
    return a > b ? a : b;
}
""",
    ),
    (
        "O1",
        """function isPositive(n) {
    return n > 0;
}
""",
    ),
    (
        "O1",
        """function toggleFlag(flags, mask) {
    return flags ^ mask;
}
""",
    ),
    (
        "O1",
        """function addToStack(stack, value) {
    stack.push(value);
    return stack.length;
}
""",
    ),
    (
        "O1",
        """function hashGet(map, key) {
    return map.get(key);
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
        """public class Solution {
    public int cellAt(int[][] grid, int r, int c) {
        return grid[r][c];
    }
}
""",
    ),
    (
        "O1",
        """import java.util.Map;
public class Solution {
    public Integer lookup(Map<String, Integer> map, String key) {
        return map.get(key);
    }
}
""",
    ),
    (
        "O1",
        """public class Solution {
    public boolean isBitSet(int n, int pos) {
        return ((n >> pos) & 1) == 1;
    }
}
""",
    ),
    (
        "O1",
        """public class Solution {
    public int circleArea(int radius) {
        return (int) (Math.PI * radius * radius);
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
        """def fast_power(base, exp):
    result = 1
    while exp > 0:
        if exp % 2 == 1:
            result *= base
        base *= base
        exp //= 2
    return result
""",
    ),
    (
        "Ologn",
        """def lower_bound(arr, target):
    lo, hi = 0, len(arr)
    while lo < hi:
        mid = (lo + hi) // 2
        if arr[mid] < target:
            lo = mid + 1
        else:
            hi = mid
    return lo
""",
    ),
    (
        "Ologn",
        """def upper_bound(arr, target):
    lo, hi = 0, len(arr)
    while lo < hi:
        mid = (lo + hi) // 2
        if arr[mid] <= target:
            lo = mid + 1
        else:
            hi = mid
    return lo
""",
    ),
    (
        "Ologn",
        """def int_sqrt(n):
    if n < 2:
        return n
    lo, hi = 1, n
    while lo <= hi:
        mid = (lo + hi) // 2
        if mid * mid <= n < (mid + 1) * (mid + 1):
            return mid
        if mid * mid < n:
            lo = mid + 1
        else:
            hi = mid - 1
    return lo
""",
    ),
    (
        "Ologn",
        """def count_bits(n):
    count = 0
    while n > 0:
        count += n & 1
        n >>= 1
    return count
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
        """function searchRotated(arr, target) {
    let lo = 0, hi = arr.length - 1;
    while (lo <= hi) {
        const mid = Math.floor((lo + hi) / 2);
        if (arr[mid] === target) return mid;
        if (arr[lo] <= arr[mid]) {
            if (arr[lo] <= target && target < arr[mid]) hi = mid - 1;
            else lo = mid + 1;
        } else {
            if (arr[mid] < target && target <= arr[hi]) lo = mid + 1;
            else hi = mid - 1;
        }
    }
    return -1;
}
""",
    ),
    (
        "Ologn",
        """function findPeak(arr) {
    let lo = 0, hi = arr.length - 1;
    while (lo < hi) {
        const mid = Math.floor((lo + hi) / 2);
        if (arr[mid] > arr[mid + 1]) hi = mid;
        else lo = mid + 1;
    }
    return lo;
}
""",
    ),
    (
        "Ologn",
        """function bstSearch(root, target) {
    let node = root;
    while (node !== null) {
        if (node.value === target) return node;
        node = target < node.value ? node.left : node.right;
    }
    return null;
}
""",
    ),
    (
        "Ologn",
        """function fastPower(base, exp) {
    let result = 1;
    while (exp > 0) {
        if (exp & 1) result *= base;
        base *= base;
        exp = Math.floor(exp / 2);
    }
    return result;
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
        """public class Solution {
    public int firstTrue(boolean[] arr) {
        int lo = 0, hi = arr.length;
        while (lo < hi) {
            int mid = lo + (hi - lo) / 2;
            if (arr[mid]) hi = mid;
            else lo = mid + 1;
        }
        return lo;
    }
}
""",
    ),
    (
        "Ologn",
        """public class Solution {
    public long fastPow(long base, long exp) {
        long result = 1;
        while (exp > 0) {
            if ((exp & 1) == 1) result *= base;
            base *= base;
            exp >>= 1;
        }
        return result;
    }
}
""",
    ),
    (
        "Ologn",
        """public class Solution {
    public int countSetBits(int n) {
        int c = 0;
        while (n != 0) {
            c += n & 1;
            n >>>= 1;
        }
        return c;
    }
}
""",
    ),
    (
        "Ologn",
        """public class Solution {
    public int sqrtInt(int x) {
        if (x < 2) return x;
        int lo = 1, hi = x;
        while (lo < hi) {
            int mid = lo + (hi - lo) / 2;
            if ((long) mid * mid <= x) lo = mid + 1;
            else hi = mid;
        }
        return lo - 1;
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
    (
        "Ologn",
        """def bsearch_first(arr, target):
    lo, hi = 0, len(arr) - 1
    result = -1
    while lo <= hi:
        mid = (lo + hi) // 2
        if arr[mid] == target:
            result = mid
            hi = mid - 1
        elif arr[mid] < target:
            lo = mid + 1
        else:
            hi = mid - 1
    return result
""",
    ),
    (
        "Ologn",
        """def ternary_search(arr, target):
    lo, hi = 0, len(arr) - 1
    while lo <= hi:
        third = (hi - lo) // 3
        m1 = lo + third
        m2 = hi - third
        if arr[m1] == target:
            return m1
        if arr[m2] == target:
            return m2
        if target < arr[m1]:
            hi = m1 - 1
        elif target > arr[m2]:
            lo = m2 + 1
        else:
            lo, hi = m1 + 1, m2 - 1
    return -1
""",
    ),
    (
        "Ologn",
        """def find_min_rotated(arr):
    lo, hi = 0, len(arr) - 1
    while lo < hi:
        mid = (lo + hi) // 2
        if arr[mid] > arr[hi]:
            lo = mid + 1
        else:
            hi = mid
    return arr[lo]
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
        """def count_vowels(s):
    count = 0
    for ch in s:
        if ch in 'aeiouAEIOU':
            count += 1
    return count
""",
    ),
    (
        "On",
        """def is_palindrome(s):
    lo, hi = 0, len(s) - 1
    while lo < hi:
        if s[lo] != s[hi]:
            return False
        lo += 1
        hi -= 1
    return True
""",
    ),
    (
        "On",
        """def kadane(arr):
    best = current = arr[0]
    for x in arr[1:]:
        current = max(x, current + x)
        best = max(best, current)
    return best
""",
    ),
    (
        "On",
        """def remove_duplicates(sorted_arr):
    if not sorted_arr:
        return 0
    write = 1
    for i in range(1, len(sorted_arr)):
        if sorted_arr[i] != sorted_arr[i-1]:
            sorted_arr[write] = sorted_arr[i]
            write += 1
    return write
""",
    ),
    (
        "On",
        """def prefix_sum(arr):
    out = [0] * (len(arr) + 1)
    for i, x in enumerate(arr):
        out[i+1] = out[i] + x
    return out
""",
    ),
    (
        "On",
        """def two_pointer_target(sorted_arr, target):
    lo, hi = 0, len(sorted_arr) - 1
    while lo < hi:
        s = sorted_arr[lo] + sorted_arr[hi]
        if s == target:
            return (lo, hi)
        if s < target:
            lo += 1
        else:
            hi -= 1
    return None
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
        """function reverseLinkedList(head) {
    let prev = null;
    let curr = head;
    while (curr !== null) {
        const next = curr.next;
        curr.next = prev;
        prev = curr;
        curr = next;
    }
    return prev;
}
""",
    ),
    (
        "On",
        """function moveZeroes(arr) {
    let write = 0;
    for (let i = 0; i < arr.length; i++) {
        if (arr[i] !== 0) {
            arr[write++] = arr[i];
        }
    }
    while (write < arr.length) arr[write++] = 0;
    return arr;
}
""",
    ),
    (
        "On",
        """function validAnagram(a, b) {
    if (a.length !== b.length) return false;
    const counts = new Array(26).fill(0);
    for (let i = 0; i < a.length; i++) {
        counts[a.charCodeAt(i) - 97]++;
        counts[b.charCodeAt(i) - 97]--;
    }
    return counts.every(c => c === 0);
}
""",
    ),
    (
        "On",
        """function fizzbuzz(n) {
    const out = [];
    for (let i = 1; i <= n; i++) {
        if (i % 15 === 0) out.push('FizzBuzz');
        else if (i % 3 === 0) out.push('Fizz');
        else if (i % 5 === 0) out.push('Buzz');
        else out.push(String(i));
    }
    return out;
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
        """public class Solution {
    public int findMissing(int[] nums) {
        int xor = nums.length;
        for (int i = 0; i < nums.length; i++) {
            xor ^= i ^ nums[i];
        }
        return xor;
    }
}
""",
    ),
    (
        "On",
        """public class Solution {
    public int maxConsecutiveOnes(int[] nums) {
        int best = 0, current = 0;
        for (int x : nums) {
            current = (x == 1) ? current + 1 : 0;
            if (current > best) best = current;
        }
        return best;
    }
}
""",
    ),
    (
        "On",
        """public class Solution {
    public int[] runningSum(int[] nums) {
        for (int i = 1; i < nums.length; i++) {
            nums[i] += nums[i - 1];
        }
        return nums;
    }
}
""",
    ),
    (
        "On",
        """public class Solution {
    public int singleNumber(int[] nums) {
        int result = 0;
        for (int x : nums) result ^= x;
        return result;
    }
}
""",
    ),
    (
        "On",
        """public class Solution {
    public int countChar(String s, char c) {
        int count = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == c) count++;
        }
        return count;
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
    (
        "On",
        """def rotate_left(arr, k):
    n = len(arr)
    k %= n
    return arr[k:] + arr[:k]
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
        """def quicksort(arr):
    if len(arr) <= 1:
        return arr
    pivot = arr[len(arr) // 2]
    left = [x for x in arr if x < pivot]
    mid = [x for x in arr if x == pivot]
    right = [x for x in arr if x > pivot]
    return quicksort(left) + mid + quicksort(right)
""",
    ),
    (
        "Onlogn",
        """def group_anagrams(words):
    groups = {}
    for w in words:
        key = ''.join(sorted(w))
        groups.setdefault(key, []).append(w)
    return list(groups.values())
""",
    ),
    (
        "Onlogn",
        """def longest_increasing_subseq(arr):
    import bisect
    tails = []
    for x in arr:
        pos = bisect.bisect_left(tails, x)
        if pos == len(tails):
            tails.append(x)
        else:
            tails[pos] = x
    return len(tails)
""",
    ),
    (
        "Onlogn",
        """def k_smallest_heap(arr, k):
    import heapq
    return heapq.nsmallest(k, arr)
""",
    ),
    (
        "Onlogn",
        """def merge_k_sorted(lists):
    import heapq
    heap = []
    for i, lst in enumerate(lists):
        if lst:
            heapq.heappush(heap, (lst[0], i, 0))
    out = []
    while heap:
        val, i, j = heapq.heappop(heap)
        out.append(val)
        if j + 1 < len(lists[i]):
            heapq.heappush(heap, (lists[i][j+1], i, j+1))
    return out
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
        """function mergeIntervals(intervals) {
    intervals.sort((a, b) => a[0] - b[0]);
    const out = [];
    for (const interval of intervals) {
        if (out.length && interval[0] <= out[out.length - 1][1]) {
            out[out.length - 1][1] = Math.max(out[out.length - 1][1], interval[1]);
        } else {
            out.push(interval);
        }
    }
    return out;
}
""",
    ),
    (
        "Onlogn",
        """function kClosestPoints(points, k) {
    points.sort((a, b) => (a[0]*a[0] + a[1]*a[1]) - (b[0]*b[0] + b[1]*b[1]));
    return points.slice(0, k);
}
""",
    ),
    (
        "Onlogn",
        """function sortByFrequency(arr) {
    const freq = new Map();
    for (const x of arr) freq.set(x, (freq.get(x) || 0) + 1);
    return arr.sort((a, b) => freq.get(b) - freq.get(a));
}
""",
    ),
    (
        "Onlogn",
        """function mergeSortJs(arr) {
    if (arr.length <= 1) return arr;
    const mid = Math.floor(arr.length / 2);
    const left = mergeSortJs(arr.slice(0, mid));
    const right = mergeSortJs(arr.slice(mid));
    const out = [];
    let i = 0, j = 0;
    while (i < left.length && j < right.length) {
        if (left[i] <= right[j]) out.push(left[i++]);
        else out.push(right[j++]);
    }
    return out.concat(left.slice(i)).concat(right.slice(j));
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
        """import java.util.Arrays;
public class Solution {
    public int minMeetingRooms(int[][] intervals) {
        int n = intervals.length;
        int[] starts = new int[n];
        int[] ends = new int[n];
        for (int i = 0; i < n; i++) {
            starts[i] = intervals[i][0];
            ends[i] = intervals[i][1];
        }
        Arrays.sort(starts);
        Arrays.sort(ends);
        int rooms = 0, endIdx = 0;
        for (int i = 0; i < n; i++) {
            if (starts[i] < ends[endIdx]) rooms++;
            else endIdx++;
        }
        return rooms;
    }
}
""",
    ),
    (
        "Onlogn",
        """import java.util.PriorityQueue;
public class Solution {
    public int[] topKLargest(int[] nums, int k) {
        PriorityQueue<Integer> heap = new PriorityQueue<>();
        for (int x : nums) {
            heap.offer(x);
            if (heap.size() > k) heap.poll();
        }
        int[] out = new int[k];
        for (int i = k - 1; i >= 0; i--) out[i] = heap.poll();
        return out;
    }
}
""",
    ),
    (
        "Onlogn",
        """import java.util.Arrays;
public class Solution {
    public int findKthLargest(int[] nums, int k) {
        Arrays.sort(nums);
        return nums[nums.length - k];
    }
}
""",
    ),
    (
        "Onlogn",
        """import java.util.Arrays;
public class Solution {
    public int hIndex(int[] citations) {
        Arrays.sort(citations);
        int n = citations.length;
        for (int i = 0; i < n; i++) {
            if (citations[i] >= n - i) return n - i;
        }
        return 0;
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
    (
        "Onlogn",
        """def reorganize_by_freq(s):
    import heapq
    from collections import Counter
    counts = Counter(s)
    heap = [(-v, ch) for ch, v in counts.items()]
    heapq.heapify(heap)
    out = []
    while heap:
        v, ch = heapq.heappop(heap)
        out.append(ch)
    return ''.join(out)
""",
    ),
    (
        "Onlogn",
        """def schedule_deadlines(tasks):
    tasks.sort(key=lambda t: t[1])
    time = 0
    for duration, deadline in tasks:
        time += duration
    return time
""",
    ),
    (
        "Onlogn",
        """def closest_pair(points):
    sorted_pts = sorted(points)
    best = float('inf')
    for i in range(len(sorted_pts) - 1):
        dist = sorted_pts[i+1][0] - sorted_pts[i][0]
        if dist < best:
            best = dist
    return best
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
        """def transpose_matrix(mat):
    n = len(mat)
    out = [[0] * n for _ in range(n)]
    for i in range(n):
        for j in range(n):
            out[j][i] = mat[i][j]
    return out
""",
    ),
    (
        "On2",
        """def pascals_triangle(n):
    triangle = []
    for i in range(n):
        row = [1] * (i + 1)
        for j in range(1, i):
            row[j] = triangle[i-1][j-1] + triangle[i-1][j]
        triangle.append(row)
    return triangle
""",
    ),
    (
        "On2",
        """def longest_palindrome_substring(s):
    best = ''
    for i in range(len(s)):
        for j in range(i, len(s)):
            sub = s[i:j+1]
            if sub == sub[::-1] and len(sub) > len(best):
                best = sub
    return best
""",
    ),
    (
        "On2",
        """def has_duplicate_brute(arr):
    for i in range(len(arr)):
        for j in range(i+1, len(arr)):
            if arr[i] == arr[j]:
                return True
    return False
""",
    ),
    (
        "On2",
        """def three_sum(arr, target):
    arr.sort()
    triples = []
    for i in range(len(arr) - 2):
        lo, hi = i + 1, len(arr) - 1
        while lo < hi:
            s = arr[i] + arr[lo] + arr[hi]
            if s == target:
                triples.append((arr[i], arr[lo], arr[hi]))
                lo += 1
                hi -= 1
            elif s < target:
                lo += 1
            else:
                hi -= 1
    return triples
""",
    ),
    (
        "On2",
        """def gnome_sort(arr):
    i = 0
    while i < len(arr):
        if i == 0 or arr[i] >= arr[i-1]:
            i += 1
        else:
            arr[i], arr[i-1] = arr[i-1], arr[i]
            i -= 1
    return arr
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
        """function rotateImage(mat) {
    const n = mat.length;
    for (let i = 0; i < n; i++) {
        for (let j = i + 1; j < n; j++) {
            [mat[i][j], mat[j][i]] = [mat[j][i], mat[i][j]];
        }
    }
    for (let i = 0; i < n; i++) mat[i].reverse();
    return mat;
}
""",
    ),
    (
        "On2",
        """function longestCommonSubseq(a, b) {
    const dp = Array.from({length: a.length + 1}, () => new Array(b.length + 1).fill(0));
    for (let i = 1; i <= a.length; i++) {
        for (let j = 1; j <= b.length; j++) {
            if (a[i-1] === b[j-1]) dp[i][j] = dp[i-1][j-1] + 1;
            else dp[i][j] = Math.max(dp[i-1][j], dp[i][j-1]);
        }
    }
    return dp[a.length][b.length];
}
""",
    ),
    (
        "On2",
        """function editDistance(a, b) {
    const dp = Array.from({length: a.length + 1}, () => new Array(b.length + 1).fill(0));
    for (let i = 0; i <= a.length; i++) dp[i][0] = i;
    for (let j = 0; j <= b.length; j++) dp[0][j] = j;
    for (let i = 1; i <= a.length; i++) {
        for (let j = 1; j <= b.length; j++) {
            if (a[i-1] === b[j-1]) dp[i][j] = dp[i-1][j-1];
            else dp[i][j] = 1 + Math.min(dp[i-1][j-1], dp[i-1][j], dp[i][j-1]);
        }
    }
    return dp[a.length][b.length];
}
""",
    ),
    (
        "On2",
        """function cocktailSort(arr) {
    let swapped = true;
    let start = 0, end = arr.length - 1;
    while (swapped) {
        swapped = false;
        for (let i = start; i < end; i++) {
            if (arr[i] > arr[i+1]) {
                [arr[i], arr[i+1]] = [arr[i+1], arr[i]];
                swapped = true;
            }
        }
        if (!swapped) break;
        swapped = false;
        end--;
        for (let i = end - 1; i >= start; i--) {
            if (arr[i] > arr[i+1]) {
                [arr[i], arr[i+1]] = [arr[i+1], arr[i]];
                swapped = true;
            }
        }
        start++;
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
        """public class Solution {
    public int uniquePaths(int m, int n) {
        int[][] dp = new int[m][n];
        for (int i = 0; i < m; i++) dp[i][0] = 1;
        for (int j = 0; j < n; j++) dp[0][j] = 1;
        for (int i = 1; i < m; i++) {
            for (int j = 1; j < n; j++) {
                dp[i][j] = dp[i-1][j] + dp[i][j-1];
            }
        }
        return dp[m-1][n-1];
    }
}
""",
    ),
    (
        "On2",
        """import java.util.ArrayList;
import java.util.List;
public class Solution {
    public List<Integer> spiralOrder(int[][] mat) {
        List<Integer> out = new ArrayList<>();
        int top = 0, bottom = mat.length - 1;
        int left = 0, right = mat[0].length - 1;
        while (top <= bottom && left <= right) {
            for (int j = left; j <= right; j++) out.add(mat[top][j]);
            top++;
            for (int i = top; i <= bottom; i++) out.add(mat[i][right]);
            right--;
            if (top <= bottom) {
                for (int j = right; j >= left; j--) out.add(mat[bottom][j]);
                bottom--;
            }
            if (left <= right) {
                for (int i = bottom; i >= top; i--) out.add(mat[i][left]);
                left++;
            }
        }
        return out;
    }
}
""",
    ),
    (
        "On2",
        """public class Solution {
    public void setZeroes(int[][] mat) {
        int m = mat.length, n = mat[0].length;
        boolean[] rows = new boolean[m];
        boolean[] cols = new boolean[n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (mat[i][j] == 0) {
                    rows[i] = true;
                    cols[j] = true;
                }
            }
        }
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (rows[i] || cols[j]) mat[i][j] = 0;
            }
        }
    }
}
""",
    ),
    (
        "On2",
        """public class Solution {
    public boolean isSymmetric(int[][] mat) {
        int n = mat.length;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (mat[i][j] != mat[j][i]) return false;
            }
        }
        return true;
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
    (
        "On2",
        """def all_triples(arr):
    out = []
    for i in range(len(arr)):
        for j in range(len(arr)):
            out.append((arr[i], arr[j]))
    return out
""",
    ),
    (
        "On2",
        """def count_inversions_brute(arr):
    count = 0
    for i in range(len(arr)):
        for j in range(i+1, len(arr)):
            if arr[i] > arr[j]:
                count += 1
    return count
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
        """def permutations(arr):
    if len(arr) <= 1:
        return [arr[:]]
    out = []
    for i in range(len(arr)):
        rest = arr[:i] + arr[i+1:]
        for p in permutations(rest):
            out.append([arr[i]] + p)
    return out
""",
    ),
    (
        "O2n",
        """def letter_combinations(digits, mapping, idx=0, current=''):
    if idx == len(digits):
        return [current] if current else []
    out = []
    for ch in mapping[digits[idx]]:
        out.extend(letter_combinations(digits, mapping, idx+1, current + ch))
    return out
""",
    ),
    (
        "O2n",
        """def word_break_naive(s, dictionary, start=0):
    if start == len(s):
        return True
    for end in range(start+1, len(s)+1):
        if s[start:end] in dictionary and word_break_naive(s, dictionary, end):
            return True
    return False
""",
    ),
    (
        "O2n",
        """def subset_sum(arr, target, idx=0):
    if target == 0:
        return True
    if idx == len(arr) or target < 0:
        return False
    return subset_sum(arr, target - arr[idx], idx + 1) or subset_sum(arr, target, idx + 1)
""",
    ),
    (
        "O2n",
        """def recursive_knapsack(weights, values, capacity, idx=0):
    if idx == len(weights) or capacity == 0:
        return 0
    if weights[idx] > capacity:
        return recursive_knapsack(weights, values, capacity, idx + 1)
    take = values[idx] + recursive_knapsack(weights, values, capacity - weights[idx], idx + 1)
    skip = recursive_knapsack(weights, values, capacity, idx + 1)
    return max(take, skip)
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
    (
        "O2n",
        """def binary_watch(num):
    out = []
    for h in range(12):
        for m in range(60):
            if bin(h).count('1') + bin(m).count('1') == num:
                out.append(f"{h}:{m:02d}")
    return out
""",
    ),
    (
        "O2n",
        """def generate_parens(n, open_c=0, close_c=0, current='', out=None):
    if out is None:
        out = []
    if len(current) == 2 * n:
        out.append(current)
        return out
    if open_c < n:
        generate_parens(n, open_c + 1, close_c, current + '(', out)
    if close_c < open_c:
        generate_parens(n, open_c, close_c + 1, current + ')', out)
    return out
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
        """function allPermutations(arr) {
    if (arr.length <= 1) return [arr.slice()];
    const out = [];
    for (let i = 0; i < arr.length; i++) {
        const rest = [...arr.slice(0, i), ...arr.slice(i + 1)];
        for (const p of allPermutations(rest)) {
            out.push([arr[i], ...p]);
        }
    }
    return out;
}
""",
    ),
    (
        "O2n",
        """function generateParentheses(n, open = 0, close = 0, current = '', out = []) {
    if (current.length === 2 * n) {
        out.push(current);
        return out;
    }
    if (open < n) generateParentheses(n, open + 1, close, current + '(', out);
    if (close < open) generateParentheses(n, open, close + 1, current + ')', out);
    return out;
}
""",
    ),
    (
        "O2n",
        """function subsetsBitmask(arr) {
    const out = [];
    const n = arr.length;
    for (let mask = 0; mask < (1 << n); mask++) {
        const subset = [];
        for (let i = 0; i < n; i++) {
            if (mask & (1 << i)) subset.push(arr[i]);
        }
        out.push(subset);
    }
    return out;
}
""",
    ),
    (
        "O2n",
        """function restoreIpAddresses(s) {
    const out = [];
    function backtrack(start, parts) {
        if (parts.length === 4) {
            if (start === s.length) out.push(parts.join('.'));
            return;
        }
        for (let len = 1; len <= 3 && start + len <= s.length; len++) {
            const seg = s.substring(start, start + len);
            if ((seg.length > 1 && seg[0] === '0') || parseInt(seg) > 255) continue;
            backtrack(start + len, [...parts, seg]);
        }
    }
    backtrack(0, []);
    return out;
}
""",
    ),
    (
        "O2n",
        """function wordBreakNaive(s, wordDict, start = 0) {
    if (start === s.length) return true;
    for (let end = start + 1; end <= s.length; end++) {
        if (wordDict.has(s.substring(start, end)) && wordBreakNaive(s, wordDict, end)) {
            return true;
        }
    }
    return false;
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
        """public class Solution {
    public boolean canPartition(int[] nums, int idx, int sumA, int sumB) {
        if (idx == nums.length) return sumA == sumB;
        return canPartition(nums, idx + 1, sumA + nums[idx], sumB)
            || canPartition(nums, idx + 1, sumA, sumB + nums[idx]);
    }
}
""",
    ),
    (
        "O2n",
        """public class Solution {
    public int knapsack(int[] w, int[] v, int cap, int idx) {
        if (idx == w.length || cap == 0) return 0;
        if (w[idx] > cap) return knapsack(w, v, cap, idx + 1);
        int take = v[idx] + knapsack(w, v, cap - w[idx], idx + 1);
        int skip = knapsack(w, v, cap, idx + 1);
        return Math.max(take, skip);
    }
}
""",
    ),
    (
        "O2n",
        """public class Solution {
    public int countPaths(int[][] grid, int r, int c) {
        if (r >= grid.length || c >= grid[0].length) return 0;
        if (r == grid.length - 1 && c == grid[0].length - 1) return 1;
        return countPaths(grid, r + 1, c) + countPaths(grid, r, c + 1);
    }
}
""",
    ),
    (
        "O2n",
        """public class Solution {
    public int subsetSumCount(int[] nums, int idx, int target) {
        if (target == 0) return 1;
        if (idx == nums.length) return 0;
        int take = nums[idx] <= target
                ? subsetSumCount(nums, idx + 1, target - nums[idx])
                : 0;
        int skip = subsetSumCount(nums, idx + 1, target);
        return take + skip;
    }
}
""",
    ),
    (
        "O2n",
        """public class Solution {
    public int recursiveLongestCommonSubseq(String a, String b, int i, int j) {
        if (i == a.length() || j == b.length()) return 0;
        if (a.charAt(i) == b.charAt(j)) {
            return 1 + recursiveLongestCommonSubseq(a, b, i + 1, j + 1);
        }
        return Math.max(
            recursiveLongestCommonSubseq(a, b, i + 1, j),
            recursiveLongestCommonSubseq(a, b, i, j + 1)
        );
    }
}
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
        temp_df, test_size=0.5, stratify=None, random_state=42
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
