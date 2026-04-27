package com.codearena.module8_terminalquest.tts;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/terminal-quest/tts")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class TtsController {

    private final TtsService ttsService;

    @PostMapping
    public ResponseEntity<byte[]> generateSpeech(@RequestBody TtsRequest request) {
        byte[] audio = ttsService.generateSpeech(request);
        if (audio.length == 0) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new byte[0]);
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/wav"))
                .body(audio);
    }
}
