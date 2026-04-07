package com.codearena.module8_terminalquest.tts;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/terminal-quest/explain")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class CommandExplainerController {

    private final CommandExplainerService commandExplainerService;

    @PostMapping
    public ResponseEntity<CommandExplainResponse> explain(@RequestBody CommandExplainRequest request) {
        return ResponseEntity.ok(commandExplainerService.explainCommand(request));
    }
}
