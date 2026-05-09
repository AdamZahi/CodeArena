"""Dataset generator for the Score Ranker Optimizer.

Runs code samples through Piston, computes ground-truth scores,
extracts features, and saves everything to training_data.csv.
"""

import csv
import os
import random
import time
from typing import Optional

import requests

from feature_extractor import extract_features

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PISTON_URL = "http://192.168.0.195:2000/api/v2/execute"

PISTON_AVAILABLE: Optional[bool] = None


def check_piston() -> bool:
    """Check if Piston is reachable."""
    global PISTON_AVAILABLE
    if PISTON_AVAILABLE is not None:
        return PISTON_AVAILABLE
    try:
        requests.get("http://192.168.0.195:2000/api/v2/runtimes", timeout=5)
        PISTON_AVAILABLE = True
        print("[generate_dataset] Piston is available — using live execution")
    except Exception:
        PISTON_AVAILABLE = False
        print("[generate_dataset] Piston not available — using synthetic mock data")
    return PISTON_AVAILABLE


def run_on_piston(code: str, language: str, version: str, stdin: str = "") -> dict:
    """Execute code via Piston and return the run result."""
    payload = {
        "language": language,
        "version": version,
        "files": [{"content": code}],
        "stdin": stdin,
        "run_timeout": 3000,
        "run_memory_limit": 256000000,
    }
    try:
        resp = requests.post(PISTON_URL, json=payload, timeout=30)
        data = resp.json()
        run = data.get("run", {})
        # Piston returns memory in bytes — convert to KB
        memory_bytes = run.get("memory", 0) or 0
        memory_kb = memory_bytes / 1024
        # Piston uses cpu_time (ms) instead of "time"
        time_ms = run.get("cpu_time", run.get("wall_time", 9999))
        return {
            "stdout": run.get("stdout", ""),
            "stderr": run.get("stderr", ""),
            "code": run.get("code", 1),
            "time": time_ms if time_ms is not None else 9999,
            "memory": memory_kb,
        }
    except Exception as e:
        return {"stdout": "", "stderr": str(e), "code": 1, "time": 9999, "memory": 999999}


# Realistic synthetic execution profiles keyed by (category, language)
MOCK_PROFILES = {
    "o1":        {"time_range": (5, 30),    "memory_range": (3000, 8000)},
    "on":        {"time_range": (20, 80),   "memory_range": (5000, 15000)},
    "on2":       {"time_range": (200, 900), "memory_range": (20000, 60000)},
    "hashmap":   {"time_range": (15, 60),   "memory_range": (6000, 18000)},
    "inefficient": {"time_range": (400, 1200), "memory_range": (30000, 80000)},
    "partial":   {"time_range": (30, 100),  "memory_range": (5000, 12000)},
    "recursion": {"time_range": (50, 300),  "memory_range": (10000, 40000)},
    "sorting":   {"time_range": (25, 70),   "memory_range": (8000, 20000)},
}


def mock_piston_result(sample: dict) -> dict:
    """Generate a realistic synthetic Piston result based on sample category."""
    category = sample.get("category", "on")
    profile = MOCK_PROFILES.get(category, MOCK_PROFILES["on"])

    t_lo, t_hi = profile["time_range"]
    m_lo, m_hi = profile["memory_range"]

    # Language speed multiplier for realistic variation
    lang_mult = {"python": 1.5, "node": 1.2, "java": 1.0}.get(sample["language"], 1.2)

    exec_time = round(random.uniform(t_lo, t_hi) * lang_mult, 2)
    memory = round(random.uniform(m_lo, m_hi) * lang_mult)

    return {
        "stdout": sample["expected_output"] + "\n",
        "stderr": "",
        "code": 0,
        "time": exec_time,
        "memory": memory,
    }


def calculate_score(time_ms: float, memory_kb: float,
                    tests_passed_ratio: float, exit_code: int) -> float:
    """Compute the ground-truth optimization score (0–100)."""
    if exit_code != 0 or tests_passed_ratio < 1.0:
        return 0.0
    time_score = max(0, 100 - (time_ms / 5000) * 100)
    memory_score = max(0, 100 - (memory_kb / 262144) * 100)
    score = time_score * 0.55 + memory_score * 0.45
    return round(score, 2)


# ---------------------------------------------------------------------------
# Training samples
# ---------------------------------------------------------------------------

SAMPLES = [
    # --- O(1) solutions ---
    {
        "category": "o1",
        "language": "python", "version": "3.12.0",
        "stdin": "10", "expected_output": "55",
        "total_tests": 1, "passed_tests": 1,
        "code": 'n = int(input())\nprint(n * (n + 1) // 2)\n',
    },
    {
        "category": "o1",
        "language": "javascript", "version": "20.11.1",
        "stdin": "10", "expected_output": "55",
        "total_tests": 1, "passed_tests": 1,
        "code": 'const n = parseInt(require("readline").createInterface({input:process.stdin}).on("line",l=>{const n=parseInt(l);console.log(n*(n+1)/2);process.exit();}));\n',
    },
    {
        "category": "o1",
        "language": "javascript", "version": "20.11.1",
        "stdin": "10", "expected_output": "55",
        "total_tests": 1, "passed_tests": 1,
        "code": 'process.stdin.resume();\nprocess.stdin.setEncoding("utf8");\nlet d="";\nprocess.stdin.on("data",c=>d+=c);\nprocess.stdin.on("end",()=>{const n=parseInt(d.trim());console.log(n*(n+1)/2);});\n',
    },
    {
        "category": "o1",
        "language": "java", "version": "15.0.2",
        "stdin": "10", "expected_output": "55",
        "total_tests": 1, "passed_tests": 1,
        "code": 'import java.util.Scanner;\npublic class Main {\n    public static void main(String[] args) {\n        int n = new Scanner(System.in).nextInt();\n        System.out.println(n * (n + 1) / 2);\n    }\n}\n',
    },

    # --- O(n) solutions ---
    {
        "category": "on",
        "language": "python", "version": "3.12.0",
        "stdin": "10", "expected_output": "55",
        "total_tests": 1, "passed_tests": 1,
        "code": 'n = int(input())\ntotal = 0\nfor i in range(1, n + 1):\n    total += i\nprint(total)\n',
    },
    {
        "category": "on",
        "language": "javascript", "version": "20.11.1",
        "stdin": "10", "expected_output": "55",
        "total_tests": 1, "passed_tests": 1,
        "code": 'process.stdin.resume();\nprocess.stdin.setEncoding("utf8");\nlet d="";\nprocess.stdin.on("data",c=>d+=c);\nprocess.stdin.on("end",()=>{\nconst n=parseInt(d.trim());\nlet t=0;\nfor(let i=1;i<=n;i++) t+=i;\nconsole.log(t);\n});\n',
    },
    {
        "category": "on",
        "language": "java", "version": "15.0.2",
        "stdin": "10", "expected_output": "55",
        "total_tests": 1, "passed_tests": 1,
        "code": 'import java.util.Scanner;\npublic class Main {\n    public static void main(String[] args) {\n        int n = new Scanner(System.in).nextInt();\n        int total = 0;\n        for (int i = 1; i <= n; i++) total += i;\n        System.out.println(total);\n    }\n}\n',
    },

    # --- O(n²) solutions ---
    {
        "category": "on2",
        "language": "python", "version": "3.12.0",
        "stdin": "5\n2 7 11 15 9",
        "expected_output": "0 1",
        "total_tests": 1, "passed_tests": 1,
        "code": 'n = int(input())\nnums = list(map(int, input().split()))\nfor i in range(n):\n    for j in range(i+1, n):\n        if nums[i] + nums[j] == 9:\n            print(i, j)\n            break\n    else:\n        continue\n    break\n',
    },
    {
        "category": "on2",
        "language": "javascript", "version": "20.11.1",
        "stdin": "5\n2 7 11 15 9",
        "expected_output": "0 1",
        "total_tests": 1, "passed_tests": 1,
        "code": 'process.stdin.resume();\nprocess.stdin.setEncoding("utf8");\nlet d="";\nprocess.stdin.on("data",c=>d+=c);\nprocess.stdin.on("end",()=>{\nconst lines=d.trim().split("\\n");\nconst n=parseInt(lines[0]);\nconst nums=lines[1].split(" ").map(Number);\nfor(let i=0;i<n;i++){\nfor(let j=i+1;j<n;j++){\nif(nums[i]+nums[j]===9){console.log(i+" "+j);process.exit();}\n}}\n});\n',
    },
    {
        "category": "on2",
        "language": "java", "version": "15.0.2",
        "stdin": "5\n2 7 11 15 9",
        "expected_output": "0 1",
        "total_tests": 1, "passed_tests": 1,
        "code": 'import java.util.Scanner;\npublic class Main {\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        int n = sc.nextInt();\n        int[] nums = new int[n];\n        for (int i = 0; i < n; i++) nums[i] = sc.nextInt();\n        for (int i = 0; i < n; i++)\n            for (int j = i + 1; j < n; j++)\n                if (nums[i] + nums[j] == 9) { System.out.println(i + " " + j); return; }\n    }\n}\n',
    },

    # --- Efficient data structure usage (HashMap) ---
    {
        "category": "hashmap",
        "language": "python", "version": "3.12.0",
        "stdin": "5\n2 7 11 15 9",
        "expected_output": "0 1",
        "total_tests": 1, "passed_tests": 1,
        "code": 'n = int(input())\nnums = list(map(int, input().split()))\nseen = {}\nfor i, num in enumerate(nums):\n    comp = 9 - num\n    if comp in seen:\n        print(seen[comp], i)\n        break\n    seen[num] = i\n',
    },
    {
        "category": "hashmap",
        "language": "javascript", "version": "20.11.1",
        "stdin": "5\n2 7 11 15 9",
        "expected_output": "0 1",
        "total_tests": 1, "passed_tests": 1,
        "code": 'process.stdin.resume();\nprocess.stdin.setEncoding("utf8");\nlet d="";\nprocess.stdin.on("data",c=>d+=c);\nprocess.stdin.on("end",()=>{\nconst lines=d.trim().split("\\n");\nconst n=parseInt(lines[0]);\nconst nums=lines[1].split(" ").map(Number);\nconst seen=new Map();\nfor(let i=0;i<n;i++){\nconst comp=9-nums[i];\nif(seen.has(comp)){console.log(seen.get(comp)+" "+i);process.exit();}\nseen.set(nums[i],i);\n}\n});\n',
    },

    # --- Inefficient duplicate (same problem, worse algo) ---
    {
        "category": "inefficient",
        "language": "python", "version": "3.12.0",
        "stdin": "5\n2 7 11 15 9",
        "expected_output": "0 1",
        "total_tests": 1, "passed_tests": 1,
        "code": 'n = int(input())\nnums = list(map(int, input().split()))\nresult = None\nfor i in range(n):\n    for j in range(n):\n        if i != j and nums[i] + nums[j] == 9:\n            result = (min(i,j), max(i,j))\nif result:\n    print(result[0], result[1])\n',
    },

    # --- Partial pass (fails some tests) ---
    {
        "category": "partial",
        "language": "python", "version": "3.12.0",
        "stdin": "5\n2 7 11 15 9",
        "expected_output": "0 1",
        "total_tests": 3, "passed_tests": 1,
        "code": 'n = int(input())\nnums = list(map(int, input().split()))\nif nums[0] + nums[1] == 9:\n    print(0, 1)\nelse:\n    print(-1, -1)\n',
    },
    {
        "category": "partial",
        "language": "javascript", "version": "20.11.1",
        "stdin": "5\n2 7 11 15 9",
        "expected_output": "0 1",
        "total_tests": 3, "passed_tests": 1,
        "code": 'process.stdin.resume();\nprocess.stdin.setEncoding("utf8");\nlet d="";\nprocess.stdin.on("data",c=>d+=c);\nprocess.stdin.on("end",()=>{\nconst lines=d.trim().split("\\n");\nconst nums=lines[1].split(" ").map(Number);\nif(nums[0]+nums[1]===9) console.log("0 1");\nelse console.log("-1 -1");\n});\n',
    },

    # --- Recursion ---
    {
        "category": "recursion",
        "language": "python", "version": "3.12.0",
        "stdin": "10", "expected_output": "89",
        "total_tests": 1, "passed_tests": 1,
        "code": 'import sys\nsys.setrecursionlimit(10000)\ndef fib(n):\n    if n <= 1:\n        return n\n    return fib(n-1) + fib(n-2)\nprint(fib(int(input())))\n',
    },
    {
        "category": "recursion",
        "language": "java", "version": "15.0.2",
        "stdin": "10", "expected_output": "89",
        "total_tests": 1, "passed_tests": 1,
        "code": 'import java.util.Scanner;\npublic class Main {\n    static long fib(int n) {\n        if (n <= 1) return n;\n        return fib(n-1) + fib(n-2);\n    }\n    public static void main(String[] args) {\n        System.out.println(fib(new Scanner(System.in).nextInt()));\n    }\n}\n',
    },

    # --- Sorting-based ---
    {
        "category": "sorting",
        "language": "python", "version": "3.12.0",
        "stdin": "5\n3 1 4 1 5",
        "expected_output": "1 1 3 4 5",
        "total_tests": 1, "passed_tests": 1,
        "code": 'n = int(input())\nnums = list(map(int, input().split()))\nnums.sort()\nprint(" ".join(map(str, nums)))\n',
    },
    {
        "category": "sorting",
        "language": "javascript", "version": "20.11.1",
        "stdin": "5\n3 1 4 1 5",
        "expected_output": "1 1 3 4 5",
        "total_tests": 1, "passed_tests": 1,
        "code": 'process.stdin.resume();\nprocess.stdin.setEncoding("utf8");\nlet d="";\nprocess.stdin.on("data",c=>d+=c);\nprocess.stdin.on("end",()=>{\nconst lines=d.trim().split("\\n");\nconst nums=lines[1].split(" ").map(Number);\nnums.sort((a,b)=>a-b);\nconsole.log(nums.join(" "));\n});\n',
    },
    {
        "category": "sorting",
        "language": "java", "version": "15.0.2",
        "stdin": "5\n3 1 4 1 5",
        "expected_output": "1 1 3 4 5",
        "total_tests": 1, "passed_tests": 1,
        "code": 'import java.util.*;\npublic class Main {\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        int n = sc.nextInt();\n        int[] nums = new int[n];\n        for (int i = 0; i < n; i++) nums[i] = sc.nextInt();\n        Arrays.sort(nums);\n        StringBuilder sb = new StringBuilder();\n        for (int i = 0; i < n; i++) { if (i > 0) sb.append(" "); sb.append(nums[i]); }\n        System.out.println(sb);\n    }\n}\n',
    },

    # ===================================================================
    # ADVANCED SAMPLES — more complex algorithms for richer training data
    # ===================================================================

    # --- Dynamic Programming: Fibonacci with memoization (O(n)) ---
    {
        "category": "on",
        "language": "python", "version": "3.12.0",
        "stdin": "20", "expected_output": "6765",
        "total_tests": 1, "passed_tests": 1,
        "code": 'import sys\ninput_data = sys.stdin.read().strip()\nn = int(input_data)\ndp = [0] * (n + 1)\ndp[1] = 1\nfor i in range(2, n + 1):\n    dp[i] = dp[i-1] + dp[i-2]\nprint(dp[n])\n',
    },
    {
        "category": "on",
        "language": "javascript", "version": "20.11.1",
        "stdin": "20", "expected_output": "6765",
        "total_tests": 1, "passed_tests": 1,
        "code": 'process.stdin.resume();\nprocess.stdin.setEncoding("utf8");\nlet d="";\nprocess.stdin.on("data",c=>d+=c);\nprocess.stdin.on("end",()=>{\nconst n=parseInt(d.trim());\nconst dp=new Array(n+1).fill(0);\ndp[1]=1;\nfor(let i=2;i<=n;i++) dp[i]=dp[i-1]+dp[i-2];\nconsole.log(dp[n]);\n});\n',
    },
    {
        "category": "on",
        "language": "java", "version": "15.0.2",
        "stdin": "20", "expected_output": "6765",
        "total_tests": 1, "passed_tests": 1,
        "code": 'import java.util.Scanner;\npublic class Main {\n    public static void main(String[] args) {\n        int n = new Scanner(System.in).nextInt();\n        long[] dp = new long[n + 1];\n        dp[1] = 1;\n        for (int i = 2; i <= n; i++) dp[i] = dp[i-1] + dp[i-2];\n        System.out.println(dp[n]);\n    }\n}\n',
    },

    # --- DP: 0/1 Knapsack (O(n*W)) ---
    {
        "category": "on2",
        "language": "python", "version": "3.12.0",
        "stdin": "4 7\n1 3 4 5\n1 4 5 7",
        "expected_output": "9",
        "total_tests": 1, "passed_tests": 1,
        "code": 'import sys\nlines = sys.stdin.read().strip().split("\\n")\nn, W = map(int, lines[0].split())\nweights = list(map(int, lines[1].split()))\nvalues = list(map(int, lines[2].split()))\ndp = [[0]*(W+1) for _ in range(n+1)]\nfor i in range(1, n+1):\n    for w in range(W+1):\n        dp[i][w] = dp[i-1][w]\n        if weights[i-1] <= w:\n            dp[i][w] = max(dp[i][w], dp[i-1][w-weights[i-1]] + values[i-1])\nprint(dp[n][W])\n',
    },
    {
        "category": "on2",
        "language": "javascript", "version": "20.11.1",
        "stdin": "4 7\n1 3 4 5\n1 4 5 7",
        "expected_output": "9",
        "total_tests": 1, "passed_tests": 1,
        "code": 'process.stdin.resume();\nprocess.stdin.setEncoding("utf8");\nlet d="";\nprocess.stdin.on("data",c=>d+=c);\nprocess.stdin.on("end",()=>{\nconst lines=d.trim().split("\\n");\nconst [n,W]=lines[0].split(" ").map(Number);\nconst wt=lines[1].split(" ").map(Number);\nconst val=lines[2].split(" ").map(Number);\nconst dp=Array.from({length:n+1},()=>new Array(W+1).fill(0));\nfor(let i=1;i<=n;i++){\nfor(let w=0;w<=W;w++){\ndp[i][w]=dp[i-1][w];\nif(wt[i-1]<=w) dp[i][w]=Math.max(dp[i][w],dp[i-1][w-wt[i-1]]+val[i-1]);\n}}\nconsole.log(dp[n][W]);\n});\n',
    },
    {
        "category": "on2",
        "language": "java", "version": "15.0.2",
        "stdin": "4 7\n1 3 4 5\n1 4 5 7",
        "expected_output": "9",
        "total_tests": 1, "passed_tests": 1,
        "code": 'import java.util.Scanner;\npublic class Main {\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        int n = sc.nextInt(), W = sc.nextInt();\n        int[] wt = new int[n], val = new int[n];\n        for (int i = 0; i < n; i++) wt[i] = sc.nextInt();\n        for (int i = 0; i < n; i++) val[i] = sc.nextInt();\n        int[][] dp = new int[n+1][W+1];\n        for (int i = 1; i <= n; i++)\n            for (int w = 0; w <= W; w++) {\n                dp[i][w] = dp[i-1][w];\n                if (wt[i-1] <= w) dp[i][w] = Math.max(dp[i][w], dp[i-1][w-wt[i-1]] + val[i-1]);\n            }\n        System.out.println(dp[n][W]);\n    }\n}\n',
    },

    # --- Binary Search (O(log n)) ---
    {
        "category": "o1",
        "language": "python", "version": "3.12.0",
        "stdin": "8\n1 3 5 7 9 11 13 15\n7",
        "expected_output": "3",
        "total_tests": 1, "passed_tests": 1,
        "code": 'import sys\nlines = sys.stdin.read().strip().split("\\n")\nn = int(lines[0])\narr = list(map(int, lines[1].split()))\ntarget = int(lines[2])\nlo, hi = 0, n - 1\nwhile lo <= hi:\n    mid = (lo + hi) // 2\n    if arr[mid] == target:\n        print(mid)\n        break\n    elif arr[mid] < target:\n        lo = mid + 1\n    else:\n        hi = mid - 1\n',
    },
    {
        "category": "o1",
        "language": "javascript", "version": "20.11.1",
        "stdin": "8\n1 3 5 7 9 11 13 15\n7",
        "expected_output": "3",
        "total_tests": 1, "passed_tests": 1,
        "code": 'process.stdin.resume();\nprocess.stdin.setEncoding("utf8");\nlet d="";\nprocess.stdin.on("data",c=>d+=c);\nprocess.stdin.on("end",()=>{\nconst lines=d.trim().split("\\n");\nconst n=parseInt(lines[0]);\nconst arr=lines[1].split(" ").map(Number);\nconst target=parseInt(lines[2]);\nlet lo=0,hi=n-1;\nwhile(lo<=hi){\nconst mid=Math.floor((lo+hi)/2);\nif(arr[mid]===target){console.log(mid);break;}\nelse if(arr[mid]<target) lo=mid+1;\nelse hi=mid-1;\n}\n});\n',
    },
    {
        "category": "o1",
        "language": "java", "version": "15.0.2",
        "stdin": "8\n1 3 5 7 9 11 13 15\n7",
        "expected_output": "3",
        "total_tests": 1, "passed_tests": 1,
        "code": 'import java.util.Scanner;\npublic class Main {\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        int n = sc.nextInt();\n        int[] arr = new int[n];\n        for (int i = 0; i < n; i++) arr[i] = sc.nextInt();\n        int target = sc.nextInt();\n        int lo = 0, hi = n - 1;\n        while (lo <= hi) {\n            int mid = (lo + hi) / 2;\n            if (arr[mid] == target) { System.out.println(mid); return; }\n            else if (arr[mid] < target) lo = mid + 1;\n            else hi = mid - 1;\n        }\n    }\n}\n',
    },

    # --- BFS: Shortest path in unweighted grid (O(n*m)) ---
    {
        "category": "on2",
        "language": "python", "version": "3.12.0",
        "stdin": "4 4\n0 0 0 0\n0 1 1 0\n0 0 0 0\n0 1 0 0",
        "expected_output": "6",
        "total_tests": 1, "passed_tests": 1,
        "code": 'import sys\nfrom collections import deque\nlines = sys.stdin.read().strip().split("\\n")\nR, C = map(int, lines[0].split())\ngrid = []\nfor i in range(1, R+1):\n    grid.append(list(map(int, lines[i].split())))\ndist = [[-1]*C for _ in range(R)]\ndist[0][0] = 0\nq = deque([(0, 0)])\nwhile q:\n    r, c = q.popleft()\n    for dr, dc in [(-1,0),(1,0),(0,-1),(0,1)]:\n        nr, nc = r+dr, c+dc\n        if 0 <= nr < R and 0 <= nc < C and grid[nr][nc] == 0 and dist[nr][nc] == -1:\n            dist[nr][nc] = dist[r][c] + 1\n            q.append((nr, nc))\nprint(dist[R-1][C-1])\n',
    },
    {
        "category": "on2",
        "language": "javascript", "version": "20.11.1",
        "stdin": "4 4\n0 0 0 0\n0 1 1 0\n0 0 0 0\n0 1 0 0",
        "expected_output": "6",
        "total_tests": 1, "passed_tests": 1,
        "code": 'process.stdin.resume();\nprocess.stdin.setEncoding("utf8");\nlet d="";\nprocess.stdin.on("data",c=>d+=c);\nprocess.stdin.on("end",()=>{\nconst lines=d.trim().split("\\n");\nconst [R,C]=lines[0].split(" ").map(Number);\nconst grid=[];\nfor(let i=1;i<=R;i++) grid.push(lines[i].split(" ").map(Number));\nconst dist=Array.from({length:R},()=>new Array(C).fill(-1));\ndist[0][0]=0;\nconst q=[[0,0]];\nlet head=0;\nwhile(head<q.length){\nconst [r,c]=q[head++];\nfor(const [dr,dc] of [[-1,0],[1,0],[0,-1],[0,1]]){\nconst nr=r+dr,nc=c+dc;\nif(nr>=0&&nr<R&&nc>=0&&nc<C&&grid[nr][nc]===0&&dist[nr][nc]===-1){\ndist[nr][nc]=dist[r][c]+1;\nq.push([nr,nc]);\n}}}\nconsole.log(dist[R-1][C-1]);\n});\n',
    },

    # --- Prefix sum: Range sum queries (O(n) build + O(1) query) ---
    {
        "category": "on",
        "language": "python", "version": "3.12.0",
        "stdin": "6\n1 3 5 7 9 11\n3\n0 2\n1 4\n2 5",
        "expected_output": "9\n24\n32",
        "total_tests": 1, "passed_tests": 1,
        "code": 'import sys\nlines = sys.stdin.read().strip().split("\\n")\nn = int(lines[0])\narr = list(map(int, lines[1].split()))\nprefix = [0] * (n + 1)\nfor i in range(n):\n    prefix[i+1] = prefix[i] + arr[i]\nq = int(lines[2])\nresults = []\nfor i in range(q):\n    l, r = map(int, lines[3+i].split())\n    results.append(str(prefix[r+1] - prefix[l]))\nprint("\\n".join(results))\n',
    },
    {
        "category": "on",
        "language": "javascript", "version": "20.11.1",
        "stdin": "6\n1 3 5 7 9 11\n3\n0 2\n1 4\n2 5",
        "expected_output": "9\n24\n32",
        "total_tests": 1, "passed_tests": 1,
        "code": 'process.stdin.resume();\nprocess.stdin.setEncoding("utf8");\nlet d="";\nprocess.stdin.on("data",c=>d+=c);\nprocess.stdin.on("end",()=>{\nconst lines=d.trim().split("\\n");\nconst n=parseInt(lines[0]);\nconst arr=lines[1].split(" ").map(Number);\nconst prefix=[0];\nfor(let i=0;i<n;i++) prefix.push(prefix[i]+arr[i]);\nconst q=parseInt(lines[2]);\nconst res=[];\nfor(let i=0;i<q;i++){\nconst [l,r]=lines[3+i].split(" ").map(Number);\nres.push(prefix[r+1]-prefix[l]);\n}\nconsole.log(res.join("\\n"));\n});\n',
    },
    {
        "category": "on",
        "language": "java", "version": "15.0.2",
        "stdin": "6\n1 3 5 7 9 11\n3\n0 2\n1 4\n2 5",
        "expected_output": "9\n24\n32",
        "total_tests": 1, "passed_tests": 1,
        "code": 'import java.util.Scanner;\npublic class Main {\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        int n = sc.nextInt();\n        long[] prefix = new long[n + 1];\n        for (int i = 0; i < n; i++) { prefix[i+1] = prefix[i] + sc.nextInt(); }\n        int q = sc.nextInt();\n        StringBuilder sb = new StringBuilder();\n        for (int i = 0; i < q; i++) {\n            int l = sc.nextInt(), r = sc.nextInt();\n            if (i > 0) sb.append("\\n");\n            sb.append(prefix[r+1] - prefix[l]);\n        }\n        System.out.println(sb);\n    }\n}\n',
    },

    # --- Sliding window: Max sum subarray of size k (O(n)) ---
    {
        "category": "on",
        "language": "python", "version": "3.12.0",
        "stdin": "8 3\n2 1 5 1 3 2 8 4",
        "expected_output": "13",
        "total_tests": 1, "passed_tests": 1,
        "code": 'import sys\nlines = sys.stdin.read().strip().split("\\n")\nn, k = map(int, lines[0].split())\narr = list(map(int, lines[1].split()))\nwindow = sum(arr[:k])\nbest = window\nfor i in range(k, n):\n    window += arr[i] - arr[i-k]\n    best = max(best, window)\nprint(best)\n',
    },
    {
        "category": "on",
        "language": "javascript", "version": "20.11.1",
        "stdin": "8 3\n2 1 5 1 3 2 8 4",
        "expected_output": "13",
        "total_tests": 1, "passed_tests": 1,
        "code": 'process.stdin.resume();\nprocess.stdin.setEncoding("utf8");\nlet d="";\nprocess.stdin.on("data",c=>d+=c);\nprocess.stdin.on("end",()=>{\nconst lines=d.trim().split("\\n");\nconst [n,k]=lines[0].split(" ").map(Number);\nconst arr=lines[1].split(" ").map(Number);\nlet win=0;\nfor(let i=0;i<k;i++) win+=arr[i];\nlet best=win;\nfor(let i=k;i<n;i++){win+=arr[i]-arr[i-k];best=Math.max(best,win);}\nconsole.log(best);\n});\n',
    },
    {
        "category": "on",
        "language": "java", "version": "15.0.2",
        "stdin": "8 3\n2 1 5 1 3 2 8 4",
        "expected_output": "13",
        "total_tests": 1, "passed_tests": 1,
        "code": 'import java.util.Scanner;\npublic class Main {\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        int n = sc.nextInt(), k = sc.nextInt();\n        int[] arr = new int[n];\n        for (int i = 0; i < n; i++) arr[i] = sc.nextInt();\n        int win = 0;\n        for (int i = 0; i < k; i++) win += arr[i];\n        int best = win;\n        for (int i = k; i < n; i++) { win += arr[i] - arr[i-k]; best = Math.max(best, win); }\n        System.out.println(best);\n    }\n}\n',
    },

    # --- Greedy: Activity selection / interval scheduling (O(n log n)) ---
    {
        "category": "sorting",
        "language": "python", "version": "3.12.0",
        "stdin": "6\n1 3 0 5 8 5\n2 4 6 7 9 9",
        "expected_output": "4",
        "total_tests": 1, "passed_tests": 1,
        "code": 'import sys\nlines = sys.stdin.read().strip().split("\\n")\nn = int(lines[0])\nstarts = list(map(int, lines[1].split()))\nends = list(map(int, lines[2].split()))\nactivities = sorted(zip(ends, starts))\ncount = 0\nlast_end = -1\nfor end, start in activities:\n    if start >= last_end:\n        count += 1\n        last_end = end\nprint(count)\n',
    },
    {
        "category": "sorting",
        "language": "javascript", "version": "20.11.1",
        "stdin": "6\n1 3 0 5 8 5\n2 4 6 7 9 9",
        "expected_output": "4",
        "total_tests": 1, "passed_tests": 1,
        "code": 'process.stdin.resume();\nprocess.stdin.setEncoding("utf8");\nlet d="";\nprocess.stdin.on("data",c=>d+=c);\nprocess.stdin.on("end",()=>{\nconst lines=d.trim().split("\\n");\nconst n=parseInt(lines[0]);\nconst starts=lines[1].split(" ").map(Number);\nconst ends=lines[2].split(" ").map(Number);\nconst acts=[];\nfor(let i=0;i<n;i++) acts.push([ends[i],starts[i]]);\nacts.sort((a,b)=>a[0]-b[0]);\nlet count=0,lastEnd=-1;\nfor(const [e,s] of acts){if(s>=lastEnd){count++;lastEnd=e;}}\nconsole.log(count);\n});\n',
    },
    {
        "category": "sorting",
        "language": "java", "version": "15.0.2",
        "stdin": "6\n1 3 0 5 8 5\n2 4 6 7 9 9",
        "expected_output": "4",
        "total_tests": 1, "passed_tests": 1,
        "code": 'import java.util.*;\npublic class Main {\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        int n = sc.nextInt();\n        int[][] acts = new int[n][2];\n        for (int i = 0; i < n; i++) acts[i][1] = sc.nextInt();\n        for (int i = 0; i < n; i++) acts[i][0] = sc.nextInt();\n        Arrays.sort(acts, (a, b) -> a[0] - b[0]);\n        int count = 0, lastEnd = -1;\n        for (int[] a : acts) { if (a[1] >= lastEnd) { count++; lastEnd = a[0]; } }\n        System.out.println(count);\n    }\n}\n',
    },

    # --- String: Palindrome check (O(n)) ---
    {
        "category": "on",
        "language": "python", "version": "3.12.0",
        "stdin": "racecar",
        "expected_output": "True",
        "total_tests": 1, "passed_tests": 1,
        "code": 's = input().strip()\nprint(s == s[::-1])\n',
    },
    {
        "category": "on",
        "language": "javascript", "version": "20.11.1",
        "stdin": "racecar",
        "expected_output": "true",
        "total_tests": 1, "passed_tests": 1,
        "code": 'process.stdin.resume();\nprocess.stdin.setEncoding("utf8");\nlet d="";\nprocess.stdin.on("data",c=>d+=c);\nprocess.stdin.on("end",()=>{\nconst s=d.trim();\nconsole.log(s===s.split("").reverse().join(""));\n});\n',
    },

    # --- String: Longest common subsequence (O(n*m) DP) ---
    {
        "category": "on2",
        "language": "python", "version": "3.12.0",
        "stdin": "abcde\nace",
        "expected_output": "3",
        "total_tests": 1, "passed_tests": 1,
        "code": 'import sys\nlines = sys.stdin.read().strip().split("\\n")\na, b = lines[0], lines[1]\nm, n = len(a), len(b)\ndp = [[0]*(n+1) for _ in range(m+1)]\nfor i in range(1, m+1):\n    for j in range(1, n+1):\n        if a[i-1] == b[j-1]:\n            dp[i][j] = dp[i-1][j-1] + 1\n        else:\n            dp[i][j] = max(dp[i-1][j], dp[i][j-1])\nprint(dp[m][n])\n',
    },
    {
        "category": "on2",
        "language": "javascript", "version": "20.11.1",
        "stdin": "abcde\nace",
        "expected_output": "3",
        "total_tests": 1, "passed_tests": 1,
        "code": 'process.stdin.resume();\nprocess.stdin.setEncoding("utf8");\nlet d="";\nprocess.stdin.on("data",c=>d+=c);\nprocess.stdin.on("end",()=>{\nconst lines=d.trim().split("\\n");\nconst a=lines[0],b=lines[1];\nconst m=a.length,n=b.length;\nconst dp=Array.from({length:m+1},()=>new Array(n+1).fill(0));\nfor(let i=1;i<=m;i++)\nfor(let j=1;j<=n;j++)\ndp[i][j]=a[i-1]===b[j-1]?dp[i-1][j-1]+1:Math.max(dp[i-1][j],dp[i][j-1]);\nconsole.log(dp[m][n]);\n});\n',
    },
    {
        "category": "on2",
        "language": "java", "version": "15.0.2",
        "stdin": "abcde\nace",
        "expected_output": "3",
        "total_tests": 1, "passed_tests": 1,
        "code": 'import java.util.Scanner;\npublic class Main {\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        String a = sc.next(), b = sc.next();\n        int m = a.length(), n = b.length();\n        int[][] dp = new int[m+1][n+1];\n        for (int i = 1; i <= m; i++)\n            for (int j = 1; j <= n; j++)\n                dp[i][j] = a.charAt(i-1) == b.charAt(j-1) ? dp[i-1][j-1]+1 : Math.max(dp[i-1][j], dp[i][j-1]);\n        System.out.println(dp[m][n]);\n    }\n}\n',
    },

    # --- Matrix multiplication check: Multiply 2x2 matrices ---
    {
        "category": "on2",
        "language": "python", "version": "3.12.0",
        "stdin": "2\n1 2\n3 4\n5 6\n7 8",
        "expected_output": "19 22\n43 50",
        "total_tests": 1, "passed_tests": 1,
        "code": 'import sys\nlines = sys.stdin.read().strip().split("\\n")\nn = int(lines[0])\nA = [list(map(int, lines[1+i].split())) for i in range(n)]\nB = [list(map(int, lines[1+n+i].split())) for i in range(n)]\nC = [[0]*n for _ in range(n)]\nfor i in range(n):\n    for j in range(n):\n        for k in range(n):\n            C[i][j] += A[i][k] * B[k][j]\nfor row in C:\n    print(" ".join(map(str, row)))\n',
    },
    {
        "category": "on2",
        "language": "java", "version": "15.0.2",
        "stdin": "2\n1 2\n3 4\n5 6\n7 8",
        "expected_output": "19 22\n43 50",
        "total_tests": 1, "passed_tests": 1,
        "code": 'import java.util.Scanner;\npublic class Main {\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        int n = sc.nextInt();\n        int[][] A = new int[n][n], B = new int[n][n], C = new int[n][n];\n        for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) A[i][j] = sc.nextInt();\n        for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) B[i][j] = sc.nextInt();\n        for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) for (int k = 0; k < n; k++) C[i][j] += A[i][k] * B[k][j];\n        StringBuilder sb = new StringBuilder();\n        for (int i = 0; i < n; i++) {\n            for (int j = 0; j < n; j++) { if (j > 0) sb.append(" "); sb.append(C[i][j]); }\n            sb.append("\\n");\n        }\n        System.out.print(sb);\n    }\n}\n',
    },

    # --- GCD / LCM (Euclidean algorithm) ---
    {
        "category": "o1",
        "language": "python", "version": "3.12.0",
        "stdin": "12 18",
        "expected_output": "6 36",
        "total_tests": 1, "passed_tests": 1,
        "code": 'a, b = map(int, input().split())\ndef gcd(x, y):\n    while y:\n        x, y = y, x % y\n    return x\ng = gcd(a, b)\nprint(g, a * b // g)\n',
    },
    {
        "category": "o1",
        "language": "javascript", "version": "20.11.1",
        "stdin": "12 18",
        "expected_output": "6 36",
        "total_tests": 1, "passed_tests": 1,
        "code": 'process.stdin.resume();\nprocess.stdin.setEncoding("utf8");\nlet d="";\nprocess.stdin.on("data",c=>d+=c);\nprocess.stdin.on("end",()=>{\nconst [a,b]=d.trim().split(" ").map(Number);\nfunction gcd(x,y){while(y){[x,y]=[y,x%y];}return x;}\nconst g=gcd(a,b);\nconsole.log(g+" "+a*b/g);\n});\n',
    },

    # --- Sieve of Eratosthenes: Count primes up to n (O(n log log n)) ---
    {
        "category": "on",
        "language": "python", "version": "3.12.0",
        "stdin": "50",
        "expected_output": "15",
        "total_tests": 1, "passed_tests": 1,
        "code": 'n = int(input())\nif n < 2:\n    print(0)\nelse:\n    sieve = [True] * (n + 1)\n    sieve[0] = sieve[1] = False\n    for i in range(2, int(n**0.5) + 1):\n        if sieve[i]:\n            for j in range(i*i, n + 1, i):\n                sieve[j] = False\n    print(sum(sieve))\n',
    },
    {
        "category": "on",
        "language": "javascript", "version": "20.11.1",
        "stdin": "50",
        "expected_output": "15",
        "total_tests": 1, "passed_tests": 1,
        "code": 'process.stdin.resume();\nprocess.stdin.setEncoding("utf8");\nlet d="";\nprocess.stdin.on("data",c=>d+=c);\nprocess.stdin.on("end",()=>{\nconst n=parseInt(d.trim());\nif(n<2){console.log(0);return;}\nconst sieve=new Array(n+1).fill(true);\nsieve[0]=sieve[1]=false;\nfor(let i=2;i*i<=n;i++) if(sieve[i]) for(let j=i*i;j<=n;j+=i) sieve[j]=false;\nconsole.log(sieve.filter(Boolean).length);\n});\n',
    },
    {
        "category": "on",
        "language": "java", "version": "15.0.2",
        "stdin": "50",
        "expected_output": "15",
        "total_tests": 1, "passed_tests": 1,
        "code": 'import java.util.Scanner;\npublic class Main {\n    public static void main(String[] args) {\n        int n = new Scanner(System.in).nextInt();\n        boolean[] sieve = new boolean[n + 1];\n        java.util.Arrays.fill(sieve, true);\n        sieve[0] = sieve[1] = false;\n        for (int i = 2; i * i <= n; i++) if (sieve[i]) for (int j = i*i; j <= n; j += i) sieve[j] = false;\n        int count = 0;\n        for (boolean b : sieve) if (b) count++;\n        System.out.println(count);\n    }\n}\n',
    },

    # --- Two pointer: Remove duplicates from sorted array (O(n)) ---
    {
        "category": "on",
        "language": "python", "version": "3.12.0",
        "stdin": "8\n1 1 2 3 3 3 4 5",
        "expected_output": "1 2 3 4 5",
        "total_tests": 1, "passed_tests": 1,
        "code": 'import sys\nlines = sys.stdin.read().strip().split("\\n")\nn = int(lines[0])\narr = list(map(int, lines[1].split()))\nresult = [arr[0]]\nfor i in range(1, n):\n    if arr[i] != arr[i-1]:\n        result.append(arr[i])\nprint(" ".join(map(str, result)))\n',
    },
    {
        "category": "on",
        "language": "javascript", "version": "20.11.1",
        "stdin": "8\n1 1 2 3 3 3 4 5",
        "expected_output": "1 2 3 4 5",
        "total_tests": 1, "passed_tests": 1,
        "code": 'process.stdin.resume();\nprocess.stdin.setEncoding("utf8");\nlet d="";\nprocess.stdin.on("data",c=>d+=c);\nprocess.stdin.on("end",()=>{\nconst lines=d.trim().split("\\n");\nconst arr=lines[1].split(" ").map(Number);\nconst res=[arr[0]];\nfor(let i=1;i<arr.length;i++) if(arr[i]!==arr[i-1]) res.push(arr[i]);\nconsole.log(res.join(" "));\n});\n',
    },

    # --- Kadane's algorithm: Max subarray sum (O(n)) ---
    {
        "category": "on",
        "language": "python", "version": "3.12.0",
        "stdin": "9\n-2 1 -3 4 -1 2 1 -5 4",
        "expected_output": "6",
        "total_tests": 1, "passed_tests": 1,
        "code": 'import sys\nlines = sys.stdin.read().strip().split("\\n")\nn = int(lines[0])\narr = list(map(int, lines[1].split()))\nmax_sum = cur = arr[0]\nfor i in range(1, n):\n    cur = max(arr[i], cur + arr[i])\n    max_sum = max(max_sum, cur)\nprint(max_sum)\n',
    },
    {
        "category": "on",
        "language": "javascript", "version": "20.11.1",
        "stdin": "9\n-2 1 -3 4 -1 2 1 -5 4",
        "expected_output": "6",
        "total_tests": 1, "passed_tests": 1,
        "code": 'process.stdin.resume();\nprocess.stdin.setEncoding("utf8");\nlet d="";\nprocess.stdin.on("data",c=>d+=c);\nprocess.stdin.on("end",()=>{\nconst lines=d.trim().split("\\n");\nconst arr=lines[1].split(" ").map(Number);\nlet maxSum=arr[0],cur=arr[0];\nfor(let i=1;i<arr.length;i++){cur=Math.max(arr[i],cur+arr[i]);maxSum=Math.max(maxSum,cur);}\nconsole.log(maxSum);\n});\n',
    },
    {
        "category": "on",
        "language": "java", "version": "15.0.2",
        "stdin": "9\n-2 1 -3 4 -1 2 1 -5 4",
        "expected_output": "6",
        "total_tests": 1, "passed_tests": 1,
        "code": 'import java.util.Scanner;\npublic class Main {\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        int n = sc.nextInt();\n        int[] arr = new int[n];\n        for (int i = 0; i < n; i++) arr[i] = sc.nextInt();\n        int maxSum = arr[0], cur = arr[0];\n        for (int i = 1; i < n; i++) { cur = Math.max(arr[i], cur + arr[i]); maxSum = Math.max(maxSum, cur); }\n        System.out.println(maxSum);\n    }\n}\n',
    },

    # --- Counting sort (O(n+k)) ---
    {
        "category": "on",
        "language": "python", "version": "3.12.0",
        "stdin": "10\n4 2 2 8 3 3 1 7 5 4",
        "expected_output": "1 2 2 3 3 4 4 5 7 8",
        "total_tests": 1, "passed_tests": 1,
        "code": 'import sys\nlines = sys.stdin.read().strip().split("\\n")\nn = int(lines[0])\narr = list(map(int, lines[1].split()))\nmax_val = max(arr)\ncount = [0] * (max_val + 1)\nfor x in arr:\n    count[x] += 1\nresult = []\nfor i in range(max_val + 1):\n    result.extend([i] * count[i])\nprint(" ".join(map(str, result)))\n',
    },

    # --- Stack: Valid parentheses (O(n)) ---
    {
        "category": "on",
        "language": "python", "version": "3.12.0",
        "stdin": "({[]})",
        "expected_output": "True",
        "total_tests": 1, "passed_tests": 1,
        "code": 's = input().strip()\nstack = []\nmatch = {")": "(", "]": "[", "}": "{"}\nvalid = True\nfor c in s:\n    if c in "([{":\n        stack.append(c)\n    elif c in ")]}":\n        if not stack or stack[-1] != match[c]:\n            valid = False\n            break\n        stack.pop()\nprint(valid and not stack)\n',
    },
    {
        "category": "on",
        "language": "javascript", "version": "20.11.1",
        "stdin": "({[]})",
        "expected_output": "true",
        "total_tests": 1, "passed_tests": 1,
        "code": 'process.stdin.resume();\nprocess.stdin.setEncoding("utf8");\nlet d="";\nprocess.stdin.on("data",c=>d+=c);\nprocess.stdin.on("end",()=>{\nconst s=d.trim();\nconst stack=[];\nconst match={")":"(","]":"[","}":"{"};\nlet valid=true;\nfor(const c of s){\nif("([{".includes(c)) stack.push(c);\nelse if(")]}".includes(c)){if(!stack.length||stack[stack.length-1]!==match[c]){valid=false;break;}stack.pop();}\n}\nconsole.log(valid&&stack.length===0);\n});\n',
    },

    # --- DP: Longest increasing subsequence (O(n²)) ---
    {
        "category": "on2",
        "language": "python", "version": "3.12.0",
        "stdin": "8\n10 9 2 5 3 7 101 18",
        "expected_output": "4",
        "total_tests": 1, "passed_tests": 1,
        "code": 'import sys\nlines = sys.stdin.read().strip().split("\\n")\nn = int(lines[0])\narr = list(map(int, lines[1].split()))\ndp = [1] * n\nfor i in range(1, n):\n    for j in range(i):\n        if arr[j] < arr[i]:\n            dp[i] = max(dp[i], dp[j] + 1)\nprint(max(dp))\n',
    },
    {
        "category": "on2",
        "language": "javascript", "version": "20.11.1",
        "stdin": "8\n10 9 2 5 3 7 101 18",
        "expected_output": "4",
        "total_tests": 1, "passed_tests": 1,
        "code": 'process.stdin.resume();\nprocess.stdin.setEncoding("utf8");\nlet d="";\nprocess.stdin.on("data",c=>d+=c);\nprocess.stdin.on("end",()=>{\nconst lines=d.trim().split("\\n");\nconst n=parseInt(lines[0]);\nconst arr=lines[1].split(" ").map(Number);\nconst dp=new Array(n).fill(1);\nfor(let i=1;i<n;i++) for(let j=0;j<i;j++) if(arr[j]<arr[i]) dp[i]=Math.max(dp[i],dp[j]+1);\nconsole.log(Math.max(...dp));\n});\n',
    },
    {
        "category": "on2",
        "language": "java", "version": "15.0.2",
        "stdin": "8\n10 9 2 5 3 7 101 18",
        "expected_output": "4",
        "total_tests": 1, "passed_tests": 1,
        "code": 'import java.util.*;\npublic class Main {\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        int n = sc.nextInt();\n        int[] arr = new int[n], dp = new int[n];\n        Arrays.fill(dp, 1);\n        for (int i = 0; i < n; i++) arr[i] = sc.nextInt();\n        for (int i = 1; i < n; i++) for (int j = 0; j < i; j++) if (arr[j] < arr[i]) dp[i] = Math.max(dp[i], dp[j]+1);\n        int max = 0; for (int v : dp) max = Math.max(max, v);\n        System.out.println(max);\n    }\n}\n',
    },

    # --- Merge sort implementation (O(n log n)) ---
    {
        "category": "sorting",
        "language": "python", "version": "3.12.0",
        "stdin": "7\n38 27 43 3 9 82 10",
        "expected_output": "3 9 10 27 38 43 82",
        "total_tests": 1, "passed_tests": 1,
        "code": 'import sys\ndef merge_sort(arr):\n    if len(arr) <= 1:\n        return arr\n    mid = len(arr) // 2\n    left = merge_sort(arr[:mid])\n    right = merge_sort(arr[mid:])\n    merged = []\n    i = j = 0\n    while i < len(left) and j < len(right):\n        if left[i] <= right[j]:\n            merged.append(left[i]); i += 1\n        else:\n            merged.append(right[j]); j += 1\n    merged.extend(left[i:])\n    merged.extend(right[j:])\n    return merged\nlines = sys.stdin.read().strip().split("\\n")\nn = int(lines[0])\narr = list(map(int, lines[1].split()))\nprint(" ".join(map(str, merge_sort(arr))))\n',
    },
    {
        "category": "sorting",
        "language": "java", "version": "15.0.2",
        "stdin": "7\n38 27 43 3 9 82 10",
        "expected_output": "3 9 10 27 38 43 82",
        "total_tests": 1, "passed_tests": 1,
        "code": 'import java.util.*;\npublic class Main {\n    static void mergeSort(int[] a, int l, int r) {\n        if (r - l <= 1) return;\n        int m = (l + r) / 2;\n        mergeSort(a, l, m);\n        mergeSort(a, m, r);\n        int[] tmp = new int[r - l];\n        int i = l, j = m, k = 0;\n        while (i < m && j < r) tmp[k++] = a[i] <= a[j] ? a[i++] : a[j++];\n        while (i < m) tmp[k++] = a[i++];\n        while (j < r) tmp[k++] = a[j++];\n        System.arraycopy(tmp, 0, a, l, tmp.length);\n    }\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        int n = sc.nextInt();\n        int[] a = new int[n];\n        for (int i = 0; i < n; i++) a[i] = sc.nextInt();\n        mergeSort(a, 0, n);\n        StringBuilder sb = new StringBuilder();\n        for (int i = 0; i < n; i++) { if (i > 0) sb.append(" "); sb.append(a[i]); }\n        System.out.println(sb);\n    }\n}\n',
    },

    # --- Frequency count with HashMap (O(n)) ---
    {
        "category": "hashmap",
        "language": "python", "version": "3.12.0",
        "stdin": "10\n1 2 3 2 1 3 3 2 1 4",
        "expected_output": "1:3 2:3 3:3 4:1",
        "total_tests": 1, "passed_tests": 1,
        "code": 'import sys\nlines = sys.stdin.read().strip().split("\\n")\nn = int(lines[0])\narr = list(map(int, lines[1].split()))\nfreq = {}\nfor x in arr:\n    freq[x] = freq.get(x, 0) + 1\nprint(" ".join(f"{k}:{v}" for k, v in sorted(freq.items())))\n',
    },
    {
        "category": "hashmap",
        "language": "javascript", "version": "20.11.1",
        "stdin": "10\n1 2 3 2 1 3 3 2 1 4",
        "expected_output": "1:3 2:3 3:3 4:1",
        "total_tests": 1, "passed_tests": 1,
        "code": 'process.stdin.resume();\nprocess.stdin.setEncoding("utf8");\nlet d="";\nprocess.stdin.on("data",c=>d+=c);\nprocess.stdin.on("end",()=>{\nconst lines=d.trim().split("\\n");\nconst arr=lines[1].split(" ").map(Number);\nconst freq=new Map();\nfor(const x of arr) freq.set(x,(freq.get(x)||0)+1);\nconst keys=[...freq.keys()].sort((a,b)=>a-b);\nconsole.log(keys.map(k=>k+":"+freq.get(k)).join(" "));\n});\n',
    },
    {
        "category": "hashmap",
        "language": "java", "version": "15.0.2",
        "stdin": "10\n1 2 3 2 1 3 3 2 1 4",
        "expected_output": "1:3 2:3 3:3 4:1",
        "total_tests": 1, "passed_tests": 1,
        "code": 'import java.util.*;\npublic class Main {\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        int n = sc.nextInt();\n        TreeMap<Integer,Integer> freq = new TreeMap<>();\n        for (int i = 0; i < n; i++) { int x = sc.nextInt(); freq.merge(x, 1, Integer::sum); }\n        StringBuilder sb = new StringBuilder();\n        for (var e : freq.entrySet()) { if (sb.length() > 0) sb.append(" "); sb.append(e.getKey()).append(":").append(e.getValue()); }\n        System.out.println(sb);\n    }\n}\n',
    },

    # --- DP: Coin change — minimum coins (O(n*amount)) ---
    {
        "category": "on2",
        "language": "python", "version": "3.12.0",
        "stdin": "3\n1 3 4\n6",
        "expected_output": "2",
        "total_tests": 1, "passed_tests": 1,
        "code": 'import sys\nlines = sys.stdin.read().strip().split("\\n")\nn = int(lines[0])\ncoins = list(map(int, lines[1].split()))\namount = int(lines[2])\ndp = [float("inf")] * (amount + 1)\ndp[0] = 0\nfor i in range(1, amount + 1):\n    for c in coins:\n        if c <= i and dp[i - c] + 1 < dp[i]:\n            dp[i] = dp[i - c] + 1\nprint(dp[amount] if dp[amount] != float("inf") else -1)\n',
    },
    {
        "category": "on2",
        "language": "javascript", "version": "20.11.1",
        "stdin": "3\n1 3 4\n6",
        "expected_output": "2",
        "total_tests": 1, "passed_tests": 1,
        "code": 'process.stdin.resume();\nprocess.stdin.setEncoding("utf8");\nlet d="";\nprocess.stdin.on("data",c=>d+=c);\nprocess.stdin.on("end",()=>{\nconst lines=d.trim().split("\\n");\nconst coins=lines[1].split(" ").map(Number);\nconst amount=parseInt(lines[2]);\nconst dp=new Array(amount+1).fill(Infinity);\ndp[0]=0;\nfor(let i=1;i<=amount;i++) for(const c of coins) if(c<=i) dp[i]=Math.min(dp[i],dp[i-c]+1);\nconsole.log(dp[amount]===Infinity?-1:dp[amount]);\n});\n',
    },
    {
        "category": "on2",
        "language": "java", "version": "15.0.2",
        "stdin": "3\n1 3 4\n6",
        "expected_output": "2",
        "total_tests": 1, "passed_tests": 1,
        "code": 'import java.util.*;\npublic class Main {\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        int n = sc.nextInt();\n        int[] coins = new int[n];\n        for (int i = 0; i < n; i++) coins[i] = sc.nextInt();\n        int amount = sc.nextInt();\n        int[] dp = new int[amount + 1];\n        Arrays.fill(dp, Integer.MAX_VALUE);\n        dp[0] = 0;\n        for (int i = 1; i <= amount; i++) for (int c : coins) if (c <= i && dp[i-c] != Integer.MAX_VALUE) dp[i] = Math.min(dp[i], dp[i-c]+1);\n        System.out.println(dp[amount] == Integer.MAX_VALUE ? -1 : dp[amount]);\n    }\n}\n',
    },

    # --- Brute-force permutation generator (O(n!)) — intentionally slow ---
    {
        "category": "inefficient",
        "language": "python", "version": "3.12.0",
        "stdin": "4",
        "expected_output": "24",
        "total_tests": 1, "passed_tests": 1,
        "code": 'n = int(input())\ndef permute(arr, l, r, count):\n    if l == r:\n        count[0] += 1\n        return\n    for i in range(l, r + 1):\n        arr[l], arr[i] = arr[i], arr[l]\n        permute(arr, l + 1, r, count)\n        arr[l], arr[i] = arr[i], arr[l]\ncount = [0]\npermute(list(range(n)), 0, n - 1, count)\nprint(count[0])\n',
    },
    {
        "category": "inefficient",
        "language": "javascript", "version": "20.11.1",
        "stdin": "4",
        "expected_output": "24",
        "total_tests": 1, "passed_tests": 1,
        "code": 'process.stdin.resume();\nprocess.stdin.setEncoding("utf8");\nlet d="";\nprocess.stdin.on("data",c=>d+=c);\nprocess.stdin.on("end",()=>{\nconst n=parseInt(d.trim());\nlet count=0;\nfunction permute(arr,l,r){\nif(l===r){count++;return;}\nfor(let i=l;i<=r;i++){[arr[l],arr[i]]=[arr[i],arr[l]];permute(arr,l+1,r);[arr[l],arr[i]]=[arr[i],arr[l]];}}\npermute([...Array(n).keys()],0,n-1);\nconsole.log(count);\n});\n',
    },

    # --- Recursion: Power function (O(log n) — fast exponent) ---
    {
        "category": "recursion",
        "language": "python", "version": "3.12.0",
        "stdin": "2 20",
        "expected_output": "1048576",
        "total_tests": 1, "passed_tests": 1,
        "code": 'def fast_pow(base, exp):\n    if exp == 0:\n        return 1\n    half = fast_pow(base, exp // 2)\n    if exp % 2 == 0:\n        return half * half\n    else:\n        return half * half * base\na, b = map(int, input().split())\nprint(fast_pow(a, b))\n',
    },
    {
        "category": "recursion",
        "language": "javascript", "version": "20.11.1",
        "stdin": "2 20",
        "expected_output": "1048576",
        "total_tests": 1, "passed_tests": 1,
        "code": 'process.stdin.resume();\nprocess.stdin.setEncoding("utf8");\nlet d="";\nprocess.stdin.on("data",c=>d+=c);\nprocess.stdin.on("end",()=>{\nconst [a,b]=d.trim().split(" ").map(Number);\nfunction fastPow(base,exp){\nif(exp===0) return 1;\nconst half=fastPow(base,Math.floor(exp/2));\nreturn exp%2===0?half*half:half*half*base;\n}\nconsole.log(fastPow(a,b));\n});\n',
    },
    {
        "category": "recursion",
        "language": "java", "version": "15.0.2",
        "stdin": "2 20",
        "expected_output": "1048576",
        "total_tests": 1, "passed_tests": 1,
        "code": 'import java.util.Scanner;\npublic class Main {\n    static long fastPow(long base, int exp) {\n        if (exp == 0) return 1;\n        long half = fastPow(base, exp / 2);\n        return exp % 2 == 0 ? half * half : half * half * base;\n    }\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        long a = sc.nextLong();\n        int b = sc.nextInt();\n        System.out.println(fastPow(a, b));\n    }\n}\n',
    },

    # --- Partial pass: Off-by-one bug in binary search ---
    {
        "category": "partial",
        "language": "python", "version": "3.12.0",
        "stdin": "5\n1 3 5 7 9\n9",
        "expected_output": "4",
        "total_tests": 3, "passed_tests": 2,
        "code": 'import sys\nlines = sys.stdin.read().strip().split("\\n")\nn = int(lines[0])\narr = list(map(int, lines[1].split()))\ntarget = int(lines[2])\nlo, hi = 0, n - 2\nwhile lo <= hi:\n    mid = (lo + hi) // 2\n    if arr[mid] == target:\n        print(mid)\n        break\n    elif arr[mid] < target:\n        lo = mid + 1\n    else:\n        hi = mid - 1\nelse:\n    print(-1)\n',
    },
    {
        "category": "partial",
        "language": "java", "version": "15.0.2",
        "stdin": "5\n1 3 5 7 9\n9",
        "expected_output": "4",
        "total_tests": 3, "passed_tests": 2,
        "code": 'import java.util.Scanner;\npublic class Main {\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        int n = sc.nextInt();\n        int[] arr = new int[n];\n        for (int i = 0; i < n; i++) arr[i] = sc.nextInt();\n        int target = sc.nextInt();\n        int lo = 0, hi = n - 2;\n        while (lo <= hi) {\n            int mid = (lo + hi) / 2;\n            if (arr[mid] == target) { System.out.println(mid); return; }\n            else if (arr[mid] < target) lo = mid + 1;\n            else hi = mid - 1;\n        }\n        System.out.println(-1);\n    }\n}\n',
    },
]


def main() -> None:
    """Run all samples through Piston, extract features, and save to CSV."""
    use_piston = check_piston()
    random.seed(42)  # Reproducible mock data

    rows: list[dict] = []
    total = len(SAMPLES)

    for i, sample in enumerate(SAMPLES, 1):
        code = sample["code"]
        language = sample["language"]
        version = sample["version"]
        stdin = sample["stdin"]
        expected = sample["expected_output"]
        total_tests = sample["total_tests"]
        passed_tests = sample["passed_tests"]

        if use_piston:
            result = run_on_piston(code, language, version, stdin)
        else:
            result = mock_piston_result(sample)

        # Verify correctness
        actual_stdout = result["stdout"].strip()
        correct = actual_stdout == expected
        if not correct:
            passed_tests = 0

        time_ms = float(result.get("time", 9999))
        memory_kb = float(result.get("memory", 999999))
        exit_code = int(result.get("code", 1))
        ratio = passed_tests / max(total_tests, 1)

        score = calculate_score(time_ms, memory_kb, ratio, exit_code)

        features = extract_features(code, result, language, total_tests, passed_tests)
        features["optimization_score"] = score
        features["language"] = language

        rows.append(features)

        print(f"[generate_dataset] [{i}/{total}] {language} | "
              f"time={time_ms}ms | memory={memory_kb}KB | "
              f"correct={correct} | score={score}")

        if use_piston:
            time.sleep(0.5)

    # Write CSV
    csv_path = os.path.join(SCRIPT_DIR, "training_data.csv")
    if rows:
        fieldnames = list(rows[0].keys())
        with open(csv_path, "w", newline="") as f:
            writer = csv.DictWriter(f, fieldnames=fieldnames)
            writer.writeheader()
            writer.writerows(rows)
        print(f"\n[generate_dataset] Saved {len(rows)} rows to {csv_path}")
    else:
        print("[generate_dataset] No rows generated!")


if __name__ == "__main__":
    main()
