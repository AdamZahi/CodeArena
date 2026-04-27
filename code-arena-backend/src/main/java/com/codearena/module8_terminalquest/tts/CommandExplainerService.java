package com.codearena.module8_terminalquest.tts;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
public class CommandExplainerService {

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

    private static final String FALLBACK =
            "Explanation unavailable. Try reviewing the command documentation.";

    @Value("${gemini.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public CommandExplainResponse explainCommand(CommandExplainRequest req) {
        try {
            String prompt = buildPrompt(req);

            Map<String, Object> body = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(Map.of("text", prompt)))
                    )
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-goog-api-key", apiKey);

            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.exchange(
                    GEMINI_URL, HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    Map.class);

            Map<?, ?> responseBody = response.getBody();
            if (responseBody == null) return new CommandExplainResponse(FALLBACK);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> candidates =
                    (List<Map<String, Object>>) responseBody.get("candidates");
            @SuppressWarnings("unchecked")
            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            String text = (String) parts.get(0).get("text");

            return new CommandExplainResponse(text != null ? text.trim() : FALLBACK);

        } catch (Exception e) {
            log.error("Command explanation failed: {}", e.getMessage());
            return new CommandExplainResponse(FALLBACK);
        }
    }

    private String buildPrompt(CommandExplainRequest req) {
        if (req.isCorrect()) {
            return "You are a Linux command expert and teacher. " +
                    "The student successfully completed a DevOps mission. " +
                    "Mission objective: " + req.getMissionTask() + ". " +
                    "The student used this command: " + req.getCommand() + ". " +
                    "Explain in 3-4 short sentences: " +
                    "1) What this command does exactly, " +
                    "2) What each flag or argument means, " +
                    "3) One alternative command that would also work. " +
                    "Keep it concise, technical but beginner-friendly. " +
                    "Do not use markdown formatting, just plain text with line breaks.";
        } else {
            return "You are a Linux command expert and patient teacher. " +
                    "The student is stuck on a DevOps mission. " +
                    "Mission objective: " + req.getMissionTask() + ". " +
                    "Context: " + req.getMissionContext() + ". " +
                    "The student tried this command: " + req.getCommand() + ". " +
                    "This command does not solve the objective. " +
                    "Explain in 2-3 short sentences: " +
                    "1) What their command actually does, " +
                    "2) Why it does not achieve the objective, " +
                    "3) A subtle hint pointing toward the right direction without revealing the answer. " +
                    "Be encouraging. Do not use markdown formatting, just plain text.";
        }
    }
}
