package com.codearena.module1_challenge.service;

import org.springframework.stereotype.Service;

@Service
public class XpCalculatorServiceImpl implements XpCalculatorService {

    @Override
    public int calculateXp(String difficulty, Integer customXp) {
        // Priority: Manual Override on Challenge
        if (customXp != null && customXp > 0) {
            return customXp;
        }

        // Fallback: Default Difficulty-based Mapping
        if (difficulty == null) return 50;
        return switch (difficulty.toUpperCase()) {
            case "EASY" -> 50;
            case "MEDIUM" -> 150;
            case "HARD" -> 450;
            default -> 50;
        };
    }

    @Override
    public void placeholder() {
    }
}
