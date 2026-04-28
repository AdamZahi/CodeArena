package com.codearena.module8_terminalquest.skill;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/terminal-quest/skill")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class SkillEngineController {

    private final SkillEngineService skillEngineService;

    @GetMapping("/analyze/me")
    public SkillAnalysisResponse analyze(@AuthenticationPrincipal Jwt jwt) {
        return skillEngineService.analyzePlayer(jwt.getSubject());
    }

    @GetMapping("/profile/me")
    public Map<String, Double> profile(@AuthenticationPrincipal Jwt jwt) {
        return skillEngineService.analyzePlayer(jwt.getSubject()).getSkillProfile();
    }
}
