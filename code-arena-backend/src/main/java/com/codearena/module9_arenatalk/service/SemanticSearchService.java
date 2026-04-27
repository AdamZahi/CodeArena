package com.codearena.module9_arenatalk.service;

import com.codearena.module9_arenatalk.entity.Message;
import com.codearena.module9_arenatalk.repository.MessageRepository;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SemanticSearchService {

    private final MessageRepository messageRepository;
    private final WebClient webClient = WebClient.create("http://localhost:8000");

    public record MessageDTO(String id, String content, String sender) {}
    public record SearchRequest(String query, List<MessageDTO> messages) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SearchResult(String id, String content, String sender, double score) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SearchResponse(List<SearchResult> results) {}

    public SearchResponse search(String query, Long channelId) {
        List<Message> messages = messageRepository.findByChannelIdOrderBySentAtAsc(channelId);

        List<MessageDTO> dtos = messages.stream()
                .map(m -> new MessageDTO(
                        m.getId().toString(),
                        m.getContent(),
                        m.getSenderName()  // ← changé ici !
                ))
                .toList();

        return webClient.post()
                .uri("/search")
                .bodyValue(new SearchRequest(query, dtos))
                .retrieve()
                .bodyToMono(SearchResponse.class)
                .block();
    }
}