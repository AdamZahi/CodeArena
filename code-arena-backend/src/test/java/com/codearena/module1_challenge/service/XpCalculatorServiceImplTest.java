package com.codearena.module1_challenge.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

class XpCalculatorServiceImplTest {

    private final XpCalculatorServiceImpl xpCalculator = new XpCalculatorServiceImpl();

    @ParameterizedTest(name = "difficulty={0} → xp={1}")
    @CsvSource({
            "EASY,   50",
            "MEDIUM, 150",
            "HARD,   450",
            "easy,   50",
            "Medium, 150",
            "hard,   450"
    })
    @DisplayName("should return correct XP based on difficulty (case-insensitive)")
    void shouldReturnXpByDifficulty(String difficulty, int expectedXp) {
        int result = xpCalculator.calculateXp(difficulty, null);
        assertThat(result).isEqualTo(expectedXp);
    }

    @Test
    @DisplayName("should return default 50 XP when difficulty is null")
    void shouldReturnDefaultWhenNull() {
        int result = xpCalculator.calculateXp(null, null);
        assertThat(result).isEqualTo(50);
    }

    @Test
    @DisplayName("should return default 50 XP for unknown difficulty strings")
    void shouldReturnDefaultForUnknownDifficulty() {
        int result = xpCalculator.calculateXp("NIGHTMARE", null);
        assertThat(result).isEqualTo(50);
    }

    @ParameterizedTest(name = "customXp={0}")
    @ValueSource(ints = {100, 200, 999})
    @DisplayName("should use custom XP when provided and positive")
    void shouldUseCustomXpWhenPositive(int customXp) {
        int result = xpCalculator.calculateXp("EASY", customXp);
        assertThat(result).isEqualTo(customXp);
    }

    @Test
    @DisplayName("should fall back to difficulty-based XP when customXp is 0")
    void shouldFallBackWhenCustomXpIsZero() {
        int result = xpCalculator.calculateXp("HARD", 0);
        assertThat(result).isEqualTo(450);
    }

    @Test
    @DisplayName("should fall back to difficulty-based XP when customXp is negative")
    void shouldFallBackWhenCustomXpIsNegative() {
        int result = xpCalculator.calculateXp("MEDIUM", -10);
        assertThat(result).isEqualTo(150);
    }
}
