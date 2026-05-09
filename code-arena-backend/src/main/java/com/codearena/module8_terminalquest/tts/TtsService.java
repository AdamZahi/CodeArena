package com.codearena.module8_terminalquest.tts;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class TtsService {

    private static final String GEMINI_TTS_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-preview-tts:generateContent";

    private static final int SAMPLE_RATE   = 24000;
    private static final int BITS_PER_SAMPLE = 16;
    private static final int CHANNELS      = 1;

    @Value("${gemini.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public byte[] generateSpeech(TtsRequest request) {
        try {
            String spokenText = (request.getStyle() != null && !request.getStyle().isBlank())
                    ? "Say in a " + request.getStyle() + ": " + request.getText()
                    : request.getText();

            String voiceName = (request.getVoiceName() != null && !request.getVoiceName().isBlank())
                    ? request.getVoiceName()
                    : "Charon";

            Map<String, Object> body = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(Map.of("text", spokenText)))
                    ),
                    "generationConfig", Map.of(
                            "responseModalities", List.of("AUDIO"),
                            "speechConfig", Map.of(
                                    "voiceConfig", Map.of(
                                            "prebuiltVoiceConfig", Map.of("voiceName", voiceName)
                                    )
                            )
                    )
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-goog-api-key", apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    GEMINI_TTS_URL, HttpMethod.POST, entity, Map.class);

            if (response.getBody() == null) return new byte[0];

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> candidates =
                    (List<Map<String, Object>>) response.getBody().get("candidates");
            @SuppressWarnings("unchecked")
            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            @SuppressWarnings("unchecked")
            Map<String, Object> inlineData = (Map<String, Object>) parts.get(0).get("inlineData");
            String base64Audio = (String) inlineData.get("data");

            byte[] pcm = Base64.getDecoder().decode(base64Audio);
            return buildWav(pcm);

        } catch (Exception e) {
            log.error("TTS generation failed: {}", e.getMessage());
            return new byte[0];
        }
    }

    private byte[] buildWav(byte[] pcm) {
        int byteRate  = SAMPLE_RATE * CHANNELS * BITS_PER_SAMPLE / 8;
        int blockAlign = CHANNELS * BITS_PER_SAMPLE / 8;
        int dataSize  = pcm.length;
        int chunkSize = 36 + dataSize;

        ByteBuffer buf = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN);

        // RIFF header
        buf.put(new byte[]{'R','I','F','F'});
        buf.putInt(chunkSize);
        buf.put(new byte[]{'W','A','V','E'});

        // fmt sub-chunk
        buf.put(new byte[]{'f','m','t',' '});
        buf.putInt(16);                // sub-chunk size
        buf.putShort((short) 1);       // PCM format
        buf.putShort((short) CHANNELS);
        buf.putInt(SAMPLE_RATE);
        buf.putInt(byteRate);
        buf.putShort((short) blockAlign);
        buf.putShort((short) BITS_PER_SAMPLE);

        // data sub-chunk
        buf.put(new byte[]{'d','a','t','a'});
        buf.putInt(dataSize);
        buf.put(pcm);

        return buf.array();
    }
}
