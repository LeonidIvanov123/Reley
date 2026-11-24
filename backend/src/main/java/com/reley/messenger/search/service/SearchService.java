package com.reley.messenger.search.service;

import com.reley.messenger.search.dto.SearchResponse;
import reactor.core.publisher.Mono;

public interface SearchService {

    Mono<SearchResponse> search(String query);

}
