package com.codearena.module2_battle.util;

import com.codearena.module2_battle.exception.UnsupportedLanguageException;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Maps platform language keys to Piston runtime language names and versions.
 * Only languages installed on the self-hosted Piston instance are supported.
 */
@Component
public class PistonLanguageMapper {

    /**
     * Record holding the Piston language name and installed version.
     */
    public record PistonLang(String language, String version) {}

    private static final Map<String, PistonLang> LANGUAGE_MAP = Map.ofEntries(
            Map.entry("python",     new PistonLang("python",     "3.12.0")),
            Map.entry("javascript", new PistonLang("javascript", "20.11.1")),
            Map.entry("java",       new PistonLang("java",       "15.0.2")),
            Map.entry("go",         new PistonLang("go",         "1.16.2")),
            Map.entry("rust",       new PistonLang("rust",       "1.50.0")),
            Map.entry("csharp",     new PistonLang("csharp.net", "5.0.201")),
            Map.entry("php",        new PistonLang("php",        "8.2.3")),
            Map.entry("bash",       new PistonLang("bash",       "5.2.0"))
    );

    public PistonLang toPistonLang(String language) {
        PistonLang lang = LANGUAGE_MAP.get(language.toLowerCase());
        if (lang == null) {
            throw new UnsupportedLanguageException(language);
        }
        return lang;
    }

    public static Map<String, PistonLang> getSupportedLanguages() {
        return LANGUAGE_MAP;
    }
}
