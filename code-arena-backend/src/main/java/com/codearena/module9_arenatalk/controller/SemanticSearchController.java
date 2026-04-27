package com.codearena.module9_arenatalk.controller;

import com.codearena.module9_arenatalk.service.SemanticSearchService;
import com.codearena.module9_arenatalk.service.SemanticSearchService.SearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SemanticSearchController {

    private final SemanticSearchService semanticSearchService;

    public record SemanticSearchRequest(String query, Long channelId) {}

    @PostMapping("/semantic")
    public SearchResponse semanticSearch(@RequestBody SemanticSearchRequest request) {
        return semanticSearchService.search(request.query(), request.channelId());
    }
}