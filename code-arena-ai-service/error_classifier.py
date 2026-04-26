"""
Rule-based + pattern-matching error classifier for code submissions.
Uses regex patterns and AST-like heuristics to detect common coding mistakes
without requiring external API calls.
"""
import re
from typing import List, Dict

# ═══════ ERROR PATTERN DEFINITIONS ═══════
ERROR_PATTERNS = {
    "JAVA": [
        {"type": "Missing semicolon", "category": "Syntax Error",
         "pattern": r'(?:int|String|double|float|boolean|char|long|short|byte|var|return|System\.out)\s+[^;{}\n]*[^;{}\s]\s*$',
         "severity": "low", "message": "You may be missing a semicolon at the end of a statement."},
        {"type": "Missing semicolon", "category": "Syntax Error",
         "pattern": r'(?:System\.out\.println|System\.out\.print)\s*\([^)]*\)\s*$',
         "severity": "low", "message": "Missing semicolon after print statement."},
        {"type": "Incorrect inheritance", "category": "OOP Error",
         "pattern": r'class\s+\w+\s+implement\s', "severity": "medium",
         "message": "Use 'implements' (not 'implement') for interfaces in Java."},
        {"type": "Incorrect inheritance", "category": "OOP Error",
         "pattern": r'class\s+\w+\s+extend\s', "severity": "medium",
         "message": "Use 'extends' (not 'extend') for class inheritance in Java."},
        {"type": "Polymorphism confusion", "category": "OOP Error",
         "pattern": r'@override', "severity": "medium",
         "message": "Use '@Override' (capital O) for method overriding annotation."},
        {"type": "Off-by-one error", "category": "Logic Error",
         "pattern": r'for\s*\(\s*int\s+\w+\s*=\s*0\s*;\s*\w+\s*<=\s*\w+\.length\s*;',
         "severity": "high", "message": "Potential off-by-one: use < instead of <= with .length."},
        {"type": "Off-by-one error", "category": "Logic Error",
         "pattern": r'for\s*\(\s*int\s+\w+\s*=\s*0\s*;\s*\w+\s*<=\s*\w+\.size\(\)\s*;',
         "severity": "high", "message": "Potential off-by-one: use < instead of <= with .size()."},
        {"type": "Null pointer risk", "category": "Logic Error",
         "pattern": r'\w+\.\w+\(.*\)\s*(?:==|!=)\s*null',
         "severity": "medium", "message": "Check for null before calling methods to avoid NullPointerException."},
        {"type": "String comparison error", "category": "Logic Error",
         "pattern": r'(?:String|string)\s+\w+.*==\s*"',
         "severity": "high", "message": "Use .equals() instead of == for String comparison in Java."},
        {"type": "Poor variable naming", "category": "Code Style",
         "pattern": r'(?:int|String|double|float|boolean)\s+[a-z]\s*[=;]',
         "severity": "low", "message": "Single-letter variable names reduce readability."},
        {"type": "Missing access modifier", "category": "Code Style",
         "pattern": r'^\s*(?:static\s+)?(?:void|int|String|double|float|boolean)\s+\w+\s*\(',
         "severity": "low", "message": "Consider adding an access modifier (public/private/protected)."},
        {"type": "Empty catch block", "category": "Error Handling",
         "pattern": r'catch\s*\([^)]*\)\s*\{\s*\}',
         "severity": "high", "message": "Empty catch blocks silently swallow exceptions. Add logging or handling."},
        {"type": "Array index error", "category": "Logic Error",
         "pattern": r'\[\s*\w+\.length\s*\]',
         "severity": "high", "message": "Array index at .length is out of bounds. Use .length - 1 for last element."},
    ],
    "JAVASCRIPT": [
        {"type": "var usage", "category": "Code Style",
         "pattern": r'\bvar\s+\w+', "severity": "low",
         "message": "Prefer 'let' or 'const' over 'var' for better scoping."},
        {"type": "Triple equals", "category": "Logic Error",
         "pattern": r'[^=!]==[^=]', "severity": "medium",
         "message": "Use === instead of == to avoid type coercion bugs."},
        {"type": "Missing semicolon", "category": "Syntax Error",
         "pattern": r'(?:const|let|var|return)\s+[^;{}\n]*[^;{}\s]\s*$',
         "severity": "low", "message": "Missing semicolon at end of statement."},
        {"type": "Callback hell", "category": "Code Style",
         "pattern": r'function\s*\([^)]*\)\s*\{\s*\n\s*\w+\([^)]*function',
         "severity": "medium", "message": "Nested callbacks detected. Consider using Promises or async/await."},
        {"type": "Off-by-one error", "category": "Logic Error",
         "pattern": r'for\s*\(\s*(?:let|var|const)\s+\w+\s*=\s*0\s*;\s*\w+\s*<=\s*\w+\.length\s*;',
         "severity": "high", "message": "Off-by-one: use < instead of <= with .length."},
        {"type": "Undefined check", "category": "Logic Error",
         "pattern": r'==\s*undefined|undefined\s*==',
         "severity": "medium", "message": "Use === undefined or typeof check to avoid coercion issues."},
        {"type": "Console left in code", "category": "Code Style",
         "pattern": r'console\.log\(', "severity": "low",
         "message": "Remove console.log statements before production."},
        {"type": "Poor variable naming", "category": "Code Style",
         "pattern": r'(?:let|const|var)\s+[a-z]\s*[=;]',
         "severity": "low", "message": "Single-letter variable names reduce code readability."},
    ],
    "PYTHON": [
        {"type": "Mutable default argument", "category": "Logic Error",
         "pattern": r'def\s+\w+\s*\([^)]*(?:\[\]|\{\}|list\(\)|dict\(\))\s*(?:,|\))',
         "severity": "high", "message": "Mutable default arguments are shared between calls. Use None instead."},
        {"type": "Bare except", "category": "Error Handling",
         "pattern": r'except\s*:', "severity": "medium",
         "message": "Bare except catches all exceptions including SystemExit. Specify exception types."},
        {"type": "Off-by-one error", "category": "Logic Error",
         "pattern": r'range\s*\(\s*1\s*,\s*len\(',
         "severity": "medium", "message": "Check range boundaries — range(1, len(x)) skips the first element."},
        {"type": "Type comparison", "category": "Logic Error",
         "pattern": r'type\(\w+\)\s*==', "severity": "medium",
         "message": "Use isinstance() instead of type() == for type checking."},
        {"type": "Poor variable naming", "category": "Code Style",
         "pattern": r'^[a-z]\s*=\s*', "severity": "low",
         "message": "Single-letter variable names reduce readability."},
        {"type": "Global variable usage", "category": "Code Style",
         "pattern": r'\bglobal\s+\w+', "severity": "medium",
         "message": "Avoid global variables. Use function parameters or class attributes instead."},
        {"type": "String concatenation in loop", "category": "Performance",
         "pattern": r'for\s+.*:\s*\n\s+\w+\s*\+=\s*["\']',
         "severity": "medium", "message": "String concatenation in loops is slow. Use join() or list append."},
        {"type": "Unused import", "category": "Code Style",
         "pattern": r'^import\s+\w+\s*$', "severity": "low",
         "message": "Check if this import is actually used in the code."},
    ],
    "SQL": [
        {"type": "SELECT star", "category": "Performance",
         "pattern": r'SELECT\s+\*', "severity": "medium",
         "message": "Avoid SELECT * — specify only needed columns for better performance."},
        {"type": "Missing WHERE clause", "category": "Logic Error",
         "pattern": r'(?:UPDATE|DELETE)\s+(?:FROM\s+)?\w+\s*(?:SET|$)',
         "severity": "high", "message": "UPDATE/DELETE without WHERE affects all rows. Add a WHERE clause."},
        {"type": "Incorrect JOIN usage", "category": "SQL Error",
         "pattern": r'JOIN\s+\w+\s+(?:WHERE|AND)\s+\w+\.\w+\s*=',
         "severity": "medium", "message": "Use ON clause (not WHERE) for JOIN conditions."},
        {"type": "Incorrect JOIN usage", "category": "SQL Error",
         "pattern": r'JOIN\s+\w+\s*$',
         "severity": "high", "message": "JOIN is missing an ON condition."},
        {"type": "SQL injection risk", "category": "Security",
         "pattern": r'["\'].*\+\s*\w+\s*\+\s*["\']',
         "severity": "high", "message": "String concatenation in queries risks SQL injection. Use parameterized queries."},
        {"type": "N+1 query", "category": "Performance",
         "pattern": r'(?:SELECT.*FROM.*WHERE.*IN\s*\(\s*SELECT)',
         "severity": "medium", "message": "Subquery in IN clause may cause performance issues. Consider a JOIN."},
    ],
    "ANGULAR": [
        {"type": "Any type usage", "category": "Code Style",
         "pattern": r':\s*any\b', "severity": "low",
         "message": "Avoid using 'any' type. Define proper interfaces for type safety."},
        {"type": "Memory leak risk", "category": "Logic Error",
         "pattern": r'\.subscribe\s*\(', "severity": "medium",
         "message": "Ensure subscriptions are unsubscribed in ngOnDestroy to prevent memory leaks."},
        {"type": "Direct DOM manipulation", "category": "Code Style",
         "pattern": r'document\.getElementById|document\.querySelector',
         "severity": "medium", "message": "Use Angular's ViewChild/Renderer2 instead of direct DOM manipulation."},
        {"type": "Missing async pipe", "category": "Code Style",
         "pattern": r'this\.\w+\.subscribe\(\s*(?:data|res|result)',
         "severity": "low", "message": "Consider using the async pipe in templates for automatic subscription management."},
    ],
}


def classify_errors(code: str, language: str) -> List[Dict]:
    """Analyze code and return detected errors with classifications."""
    detected_errors = []
    lang = language.upper()

    stripped_code = code.strip()
    
    minimal_keywords = {
        "JAVA": ["class", "public", "private", "protected", "import", "interface", "void", "int", "String", "boolean", "System.out"],
        "PYTHON": ["def", "class", "import", "from", "print", "return", "if", "for", "while", "=", "(", "["],
        "JAVASCRIPT": ["function", "const", "let", "var", "import", "export", "class", "console.log", "=>", "=", "{", "("],
        "ANGULAR": ["@Component", "@Injectable", "export class", "import", "constructor", "ngOnInit", "{", "("],
        "SQL": ["SELECT", "UPDATE", "DELETE", "INSERT", "CREATE", "ALTER", "DROP"]
    }
    
    has_symbols = any(c in stripped_code for c in "(){}[];=<>+-%*/")
    keywords = minimal_keywords.get(lang, [])
    has_keywords = any(kw in stripped_code for kw in keywords) if keywords else True

    if stripped_code and not (has_symbols or has_keywords):
        detected_errors.append({
            "line": 1,
            "error_type": "Invalid Code Structure",
            "category": "Syntax Error",
            "severity": "high",
            "message": "The submitted code lacks basic programming syntax or keywords for " + lang + " and appears incomplete or invalid.",
            "code_line": stripped_code[:50]
        })

    # Get language-specific patterns + always check generic ones
    patterns = ERROR_PATTERNS.get(lang, [])

    lines = code.split('\n')
    for i, line in enumerate(lines):
        stripped = line.strip()
        if not stripped or stripped.startswith('//') or stripped.startswith('#'):
            continue

        for pattern_def in patterns:
            try:
                if re.search(pattern_def["pattern"], stripped, re.IGNORECASE | re.MULTILINE):
                    detected_errors.append({
                        "line": i + 1,
                        "error_type": pattern_def["type"],
                        "category": pattern_def["category"],
                        "severity": pattern_def["severity"],
                        "message": pattern_def["message"],
                        "code_line": stripped,
                    })
            except re.error:
                continue

    # Deduplicate by (line, error_type)
    seen = set()
    unique_errors = []
    for err in detected_errors:
        key = (err["line"], err["error_type"])
        if key not in seen:
            seen.add(key)
            unique_errors.append(err)

    return unique_errors


def get_recommendations(errors: List[Dict], language: str) -> List[str]:
    """Generate personalized recommendations based on detected errors."""
    recommendations = []
    error_counts = {}
    for e in errors:
        error_counts[e["error_type"]] = error_counts.get(e["error_type"], 0) + 1

    for error_type, count in sorted(error_counts.items(), key=lambda x: -x[1]):
        if error_type == "Missing semicolon":
            recommendations.append(f"You forgot semicolons {count} time(s). Practice syntax drills in {language}.")
        elif error_type == "Off-by-one error":
            recommendations.append(f"Off-by-one errors detected ({count}x). Practice loop boundary problems.")
        elif error_type == "Incorrect JOIN usage":
            recommendations.append(f"SQL JOIN mistakes detected ({count}x). Review JOIN syntax and ON clauses.")
        elif error_type == "Incorrect inheritance":
            recommendations.append(f"OOP inheritance issues ({count}x). Review extends/implements keywords.")
        elif error_type == "String comparison error":
            recommendations.append(f"String comparison bug ({count}x). Always use .equals() in Java.")
        elif error_type == "Empty catch block":
            recommendations.append(f"Empty catch blocks ({count}x). Always handle or log exceptions.")
        elif error_type == "Poor variable naming":
            recommendations.append(f"Variable naming issues ({count}x). Use descriptive, meaningful names.")
        elif "SQL injection" in error_type:
            recommendations.append(f"Security risk: SQL injection ({count}x). Use parameterized queries.")
        else:
            recommendations.append(f"{error_type} detected ({count}x). Review best practices for {language}.")

    if not recommendations:
        recommendations.append("Great job! No common mistakes detected in this submission.")

    return recommendations
