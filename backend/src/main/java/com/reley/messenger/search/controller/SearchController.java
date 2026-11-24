package com.reley.messenger.search.controller;

import com.reley.messenger.search.dto.SearchResponse;
import com.reley.messenger.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/search")
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    public Mono<SearchResponse> search(@RequestParam String q) {
        return searchService.search(q);
    }
}