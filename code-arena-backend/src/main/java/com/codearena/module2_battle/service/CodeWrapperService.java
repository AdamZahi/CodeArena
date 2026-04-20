package com.codearena.module2_battle.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Wraps user-submitted code so that it behaves like a function-based submission
 * (similar to LeetCode). For each test case the wrapper:
 *   1. Suppresses any stdout the user's own code produces (e.g. hardcoded test prints).
 *   2. Calls the detected main function with the test-case arguments.
 *   3. Prints only the function's return value.
 *
 * Returns {@code null} when the language is unsupported or the function cannot be detected,
 * so the caller can fall back to the raw stdin-based approach.
 */
@Slf4j
@Service
public class CodeWrapperService {

    /**
     * @return wrapped source code, or {@code null} if wrapping is not possible.
     */
    public String wrapCode(String code, String language, String testCaseInput) {
        if (code == null || testCaseInput == null || testCaseInput.isBlank()) {
            return null;
        }
        return switch (language.toLowerCase()) {
            case "python"     -> wrapPython(code, testCaseInput);
            case "javascript" -> wrapJavaScript(code, testCaseInput);
            case "java"       -> wrapJava(code, testCaseInput);
            case "cpp"        -> wrapCpp(code, testCaseInput);
            case "go"         -> wrapGo(code, testCaseInput);
            default           -> null;
        };
    }

    // ── Python ──────────────────────────────────────────────────────────

    private String wrapPython(String code, String testCaseInput) {
        String funcName = detectPythonFunction(code);
        if (funcName == null) {
            log.debug("Could not detect Python function name");
            return null;
        }
        // Convert "x = 121, y = [1,2,3]" → "121, [1,2,3]" so we don't depend on the user's
        // parameter names matching the test-case variable names.
        String positionalArgs = kwargsToPositionalArgs(testCaseInput, false);
        if (positionalArgs == null) return null;

        // Redirect stdout during the user code (suppresses their hardcoded prints), call the
        // function, and emit the result in LeetCode-style format: bools as true/false (lowercase),
        // lists without spaces, everything else via str(...).
        return "import sys as __cw_sys, io as __cw_io, json as __cw_json\n"
             + "__cw_real = __cw_sys.stdout\n"
             + "__cw_sys.stdout = __cw_io.StringIO()\n"
             + "\n"
             + code + "\n"
             + "\n"
             + "__cw_sys.stdout = __cw_real\n"
             + "__cw_r = " + funcName + "(" + positionalArgs + ")\n"
             + "if isinstance(__cw_r, bool):\n"
             + "    print('true' if __cw_r else 'false')\n"
             + "elif isinstance(__cw_r, (list, tuple, dict)):\n"
             + "    print(__cw_json.dumps(__cw_r, separators=(',', ':')))\n"
             + "elif __cw_r is None:\n"
             + "    print('null')\n"
             + "else:\n"
             + "    print(__cw_r)\n";
    }

    static String detectPythonFunction(String code) {
        Pattern p = Pattern.compile("^def\\s+(\\w+)\\s*\\(", Pattern.MULTILINE);
        Matcher m = p.matcher(code);
        // Pick the first top-level function (most challenges have exactly one).
        return m.find() ? m.group(1) : null;
    }

    // ── JavaScript ──────────────────────────────────────────────────────

    private String wrapJavaScript(String code, String testCaseInput) {
        String funcName = detectJavaScriptFunction(code);
        if (funcName == null) {
            log.debug("Could not detect JavaScript function name");
            return null;
        }
        String jsArgs = kwargsToPositionalArgs(testCaseInput, true);
        if (jsArgs == null) return null;

        return "const __cw_log = console.log;\n"
             + "console.log = () => {};\n"
             + "\n"
             + code + "\n"
             + "\n"
             + "console.log = __cw_log;\n"
             + "const __cw_r = " + funcName + "(" + jsArgs + ");\n"
             + "console.log(Array.isArray(__cw_r) ? JSON.stringify(__cw_r) : __cw_r);\n";
    }

    static String detectJavaScriptFunction(String code) {
        // function declaration
        Pattern fp = Pattern.compile("^function\\s+(\\w+)\\s*\\(", Pattern.MULTILINE);
        Matcher fm = fp.matcher(code);
        if (fm.find()) return fm.group(1);
        // const / let / var arrow
        Pattern ap = Pattern.compile("^(?:const|let|var)\\s+(\\w+)\\s*=", Pattern.MULTILINE);
        Matcher am = ap.matcher(code);
        return am.find() ? am.group(1) : null;
    }

    // ── Java ────────────────────────────────────────────────────────────

    private String wrapJava(String code, String testCaseInput) {
        String methodName = detectJavaMethod(code);
        String className  = detectJavaClass(code);
        if (methodName == null || className == null) {
            log.debug("Could not detect Java class/method");
            return null;
        }
        // The runner class is named Main (matches Piston's filename convention). To avoid the
        // "public class must match filename" rule, strip the `public` modifier from the user's
        // class declaration. javac then accepts both classes in one Main.java file.
        String userCode = code.replaceAll("\\bpublic\\s+class\\s+" + java.util.regex.Pattern.quote(className),
                                          "class " + className);

        List<String[]> params = parseKwargs(testCaseInput);
        if (params.isEmpty()) return null;

        List<String[]> methodParams = detectJavaMethodParams(code, methodName);
        if (methodParams.isEmpty() || methodParams.size() != params.size()) return null;

        StringBuilder main = new StringBuilder();
        main.append(userCode).append("\n\n");
        main.append("public class Main {\n");
        main.append("    public static void main(String[] args) {\n");
        main.append("        ").append(className).append(" __cw_sol = new ").append(className).append("();\n");

        StringBuilder argList = new StringBuilder();
        for (int i = 0; i < params.size(); i++) {
            String type  = methodParams.get(i)[0];
            String name  = params.get(i)[0];
            String value = params.get(i)[1];
            String javaVal = pythonValueToJava(value, type);
            main.append("        ").append(type).append(" __cw_").append(name).append(" = ").append(javaVal).append(";\n");
            if (i > 0) argList.append(", ");
            argList.append("__cw_").append(name);
        }

        main.append("        System.out.println(__cwFmt(__cw_sol.").append(methodName).append("(").append(argList).append(")));\n");
        main.append("    }\n\n");
        main.append("    static String __cwFmt(Object o) {\n");
        main.append("        if (o == null) return \"null\";\n");
        main.append("        if (o instanceof int[])     return java.util.Arrays.toString((int[]) o).replace(\" \", \"\");\n");
        main.append("        if (o instanceof long[])    return java.util.Arrays.toString((long[]) o).replace(\" \", \"\");\n");
        main.append("        if (o instanceof double[])  return java.util.Arrays.toString((double[]) o).replace(\" \", \"\");\n");
        main.append("        if (o instanceof boolean[]) return java.util.Arrays.toString((boolean[]) o).replace(\" \", \"\");\n");
        main.append("        if (o instanceof char[])    return java.util.Arrays.toString((char[]) o).replace(\" \", \"\");\n");
        main.append("        if (o instanceof Object[]) {\n");
        main.append("            Object[] arr = (Object[]) o;\n");
        main.append("            if (arr.length > 0 && arr[0] != null && arr[0].getClass().isArray()) {\n");
        main.append("                return java.util.Arrays.deepToString(arr).replace(\" \", \"\");\n");
        main.append("            }\n");
        main.append("            return java.util.Arrays.toString(arr).replace(\" \", \"\");\n");
        main.append("        }\n");
        main.append("        if (o instanceof java.util.Collection) return o.toString().replace(\" \", \"\");\n");
        main.append("        if (o instanceof java.util.Map)        return o.toString().replace(\" \", \"\");\n");
        main.append("        return String.valueOf(o);\n");
        main.append("    }\n");
        main.append("}\n");
        return main.toString();
    }

    public static String detectJavaClass(String code) {
        Pattern p = Pattern.compile("class\\s+(\\w+)\\s*\\{", Pattern.MULTILINE);
        Matcher m = p.matcher(code);
        return m.find() ? m.group(1) : null;
    }

    static String detectJavaMethod(String code) {
        // Match first public method that isn't main
        Pattern p = Pattern.compile("public\\s+\\w[\\w\\[\\]<>,\\s]*\\s+(\\w+)\\s*\\(", Pattern.MULTILINE);
        Matcher m = p.matcher(code);
        while (m.find()) {
            String name = m.group(1);
            if (!"main".equals(name)) return name;
        }
        return null;
    }

    static List<String[]> detectJavaMethodParams(String code, String methodName) {
        // Find the method signature and extract parameter types + names
        Pattern p = Pattern.compile(methodName + "\\s*\\(([^)]*)\\)");
        Matcher m = p.matcher(code);
        if (!m.find()) return List.of();
        String paramStr = m.group(1).trim();
        if (paramStr.isEmpty()) return List.of();

        List<String[]> result = new ArrayList<>();
        for (String param : paramStr.split(",")) {
            param = param.trim();
            int lastSpace = param.lastIndexOf(' ');
            if (lastSpace < 0) return List.of();
            result.add(new String[]{param.substring(0, lastSpace).trim(), param.substring(lastSpace + 1).trim()});
        }
        return result;
    }

    private String pythonValueToJava(String value, String javaType) {
        value = value.trim();
        if (javaType.contains("[]")) {
            // Array: [1,3,5,6] → new int[]{1,3,5,6}
            String baseType = javaType.replace("[]", "").trim();
            String inner = value;
            if (inner.startsWith("[")) inner = inner.substring(1);
            if (inner.endsWith("]")) inner = inner.substring(0, inner.length() - 1);
            return "new " + baseType + "[]{" + inner + "}";
        }
        // Primitives / String pass through
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return value.toLowerCase();
        }
        return value;
    }

    // ── C++ ─────────────────────────────────────────────────────────────

    private String wrapCpp(String code, String testCaseInput) {
        String funcName = detectCppFunction(code);
        if (funcName == null) {
            log.debug("Could not detect C++ function name");
            return null;
        }
        // For C++ the test input is passed as positional args.
        String args = kwargsToPositionalArgs(testCaseInput, false);
        if (args == null) return null;

        // Convert Python list syntax [1,3,5,6] → C++ vector initializer {1,3,5,6}
        args = args.replace('[', '{').replace(']', '}');

        return "#include <bits/stdc++.h>\n"
             + "using namespace std;\n\n"
             + code + "\n\n"
             + "int main() {\n"
             + "    cout << " + funcName + "(" + args + ") << endl;\n"
             + "    return 0;\n"
             + "}\n";
    }

    static String detectCppFunction(String code) {
        // Match function definitions, skip main
        Pattern p = Pattern.compile("^\\w[\\w:<>,\\s\\*&]*\\s+(\\w+)\\s*\\([^)]*\\)\\s*\\{", Pattern.MULTILINE);
        Matcher m = p.matcher(code);
        while (m.find()) {
            String name = m.group(1);
            if (!"main".equals(name)) return name;
        }
        return null;
    }

    // ── Go ──────────────────────────────────────────────────────────────

    private String wrapGo(String code, String testCaseInput) {
        // Go wrapping is complex (typed, compiled) — skip for now
        return null;
    }

    // ── Shared utilities ────────────────────────────────────────────────

    /**
     * Parses "name1 = value1, name2 = value2" into pairs.
     * Handles commas inside brackets (e.g. arrays) correctly.
     */
    static List<String[]> parseKwargs(String input) {
        if (input == null || input.isBlank()) return List.of();
        // Split on commas that are followed by an identifier and '='
        String[] parts = input.split(",\\s*(?=\\w+\\s*=)");
        List<String[]> result = new ArrayList<>();
        for (String part : parts) {
            part = part.trim();
            int eq = part.indexOf('=');
            if (eq < 0) continue;
            result.add(new String[]{part.substring(0, eq).trim(), part.substring(eq + 1).trim()});
        }
        return result;
    }

    /**
     * Converts "name = val, name2 = val2" → "val, val2" (positional args).
     */
    static String kwargsToPositionalArgs(String input, boolean pyToJs) {
        List<String[]> pairs = parseKwargs(input);
        if (pairs.isEmpty()) return null;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pairs.size(); i++) {
            if (i > 0) sb.append(", ");
            String val = pairs.get(i)[1];
            if (pyToJs) {
                val = val.replace("True", "true").replace("False", "false").replace("None", "null");
            }
            sb.append(val);
        }
        return sb.toString();
    }
}
