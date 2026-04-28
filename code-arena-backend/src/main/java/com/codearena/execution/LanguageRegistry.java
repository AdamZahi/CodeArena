package com.codearena.execution;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Bidirectional mapping between Judge0 language IDs, platform language names,
 * and Piston language/version pairs.
 *
 * Accepts numeric Judge0 IDs ("62"), canonical names ("java"),
 * and common aliases ("py", "py3", "js", "cpp", "c++", "rs", "kt", "rb", "ts", "golang").
 *
 * Languages not in the Piston manifest return a mapping with
 * {@code isPistonSupported() == false} — these pass through directly to Judge0.
 */
@Component
public class LanguageRegistry {

    private final Map<String, LanguageMapping> registry = new HashMap<>();

    public LanguageRegistry() {
        // Java
        LanguageMapping java = new LanguageMapping("java", "15.0.2", 62, "Main.java", true);
        register(java, "java", "62");

        // Python
        LanguageMapping python = new LanguageMapping("python", "3.12.0", 71, "main.py", true);
        register(python, "python", "py", "py3", "71");

        // JavaScript
        LanguageMapping javascript = new LanguageMapping("javascript", "20.11.1", 63, "main.js", true);
        register(javascript, "javascript", "js", "63");

        // C
        LanguageMapping c = new LanguageMapping("c", "10.2.0", 50, "main.c", true);
        register(c, "c", "50");

        // C++
        LanguageMapping cpp = new LanguageMapping("c++", "10.2.0", 54, "main.cpp", true);
        register(cpp, "cpp", "c++", "54");

        // Go
        LanguageMapping go = new LanguageMapping("go", "1.16.2", 60, "main.go", true);
        register(go, "go", "golang", "60");

        // Rust
        LanguageMapping rust = new LanguageMapping("rust", "1.50.0", 73, "main.rs", true);
        register(rust, "rust", "rs", "73");

        // Kotlin
        LanguageMapping kotlin = new LanguageMapping("kotlin", "1.8.20", 78, "Main.kt", true);
        register(kotlin, "kotlin", "kt", "78");

        // PHP
        LanguageMapping php = new LanguageMapping("php", "8.2.3", 68, "main.php", true);
        register(php, "php", "68");

        // Ruby
        LanguageMapping ruby = new LanguageMapping("ruby", "3.0.1", 72, "main.rb", true);
        register(ruby, "ruby", "rb", "72");

        // TypeScript
        LanguageMapping typescript = new LanguageMapping("typescript", "5.0.3", 74, "main.ts", true);
        register(typescript, "typescript", "ts", "74");
    }

    /**
     * Resolves a language identifier to its mapping.
     * Accepts Judge0 numeric IDs ("62"), canonical names ("java"), and aliases ("py").
     *
     * @param input the language identifier
     * @return the language mapping, or an unsupported mapping for unknown languages
     */
    public LanguageMapping resolve(String input) {
        if (input == null || input.isBlank()) {
            return LanguageMapping.unsupported(input);
        }
        LanguageMapping mapping = registry.get(input.toLowerCase().trim());
        if (mapping != null) {
            return mapping;
        }
        // Unknown language — mark as not Piston-supported, try to parse as Judge0 ID
        return LanguageMapping.unsupported(input);
    }

    private void register(LanguageMapping mapping, String... aliases) {
        for (String alias : aliases) {
            registry.put(alias.toLowerCase(), mapping);
        }
    }

    /**
     * Immutable record holding the mapping between a platform language
     * and its Piston/Judge0 equivalents.
     */
    public record LanguageMapping(
            String pistonLanguage,
            String pistonVersion,
            int judge0Id,
            String fileName,
            boolean isPistonSupported
    ) {
        /**
         * Creates an unsupported mapping for languages not in the Piston manifest.
         * These will pass through directly to Judge0.
         */
        static LanguageMapping unsupported(String input) {
            int judge0Id = -1;
            try {
                judge0Id = Integer.parseInt(input);
            } catch (NumberFormatException ignored) {
                // Not a numeric ID — leave as -1
            }
            return new LanguageMapping(null, null, judge0Id, null, false);
        }
    }
}
