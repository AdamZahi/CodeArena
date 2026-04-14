package com.codearena.module9_arenatalk.controller;

import com.codearena.module9_arenatalk.dto.ReactionResponseDTO;
import com.codearena.module9_arenatalk.service.MessageReactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/arenatalk")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class MessageReactionController {

    private final MessageReactionService reactionService;

    @PostMapping("/messages/{messageId}/reactions")
    public ResponseEntity<ReactionResponseDTO> toggleReaction(
            @PathVariable Long messageId,
            @RequestParam String emoji,
            @RequestParam String keycloakId) {
        return ResponseEntity.ok(reactionService.toggleReaction(messageId, emoji, keycloakId));
    }

    @GetMapping("/messages/{messageId}/reactions")
    public ResponseEntity<ReactionResponseDTO> getReactions(
            @PathVariable Long messageId,
            @RequestParam String keycloakId) {
        return ResponseEntity.ok(reactionService.getReactions(messageId, keycloakId));
    }

    @PostMapping("/messages/reactions/batch")
    public ResponseEntity<Map<Long, ReactionResponseDTO>> getBatchReactions(
            @RequestBody List<Long> messageIds,
            @RequestParam String keycloakId) {
        return ResponseEntity.ok(reactionService.getReactionsForChannel(messageIds, keycloakId));
    }
}