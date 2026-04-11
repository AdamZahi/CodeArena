package com.codearena.module2_battle.util;

import com.codearena.module2_battle.exception.UnsupportedLanguageException;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Maps language keys used in the platform to Judge0 language IDs.
 */
@Component
public class Judge0LanguageMapper {

    private static final Map<String, Integer> LANGUAGE_MAP = Map.of(
            "java", 62,
            "python", 71,
            "javascript", 63,
            "cpp", 54,
            "go", 60,
            "rust", 73
    );

    public int toJudge0Id(String language) {
        Integer id = LANGUAGE_MAP.get(language.toLowerCase());
        if (id == null) {
            throw new UnsupportedLanguageException(language);
        }
        return id;
    }
}
