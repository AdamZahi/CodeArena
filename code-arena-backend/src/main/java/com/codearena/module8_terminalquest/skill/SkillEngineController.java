package com.codearena.module8_terminalquest.skill;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/terminal-quest/skill")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class SkillEngineController {

    private final SkillEngineService skillEngineService;

    @GetMapping("/analyze/{userId}")
    public SkillAnalysisResponse analyze(@PathVariable String userId) {
        return skillEngineService.analyzePlayer(userId);
    }

    @GetMapping("/profile/{userId}")
    public Map<String, Double> profile(@PathVariable String userId) {
        return skillEngineService.analyzePlayer(userId).getSkillProfile();
    }
}
