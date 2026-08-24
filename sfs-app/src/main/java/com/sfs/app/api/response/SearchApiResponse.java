package com.sfs.app.api.response;

import com.sfs.contracts.search.SearchResponse;

import java.util.List;
import java.util.Objects;

public record SearchApiResponse(
        String query,
        boolean searched,
        int totalResults,
        String retrievalMode,
        long elapsedMillis,
        List<SearchResultResponse> results) {

    public SearchApiResponse {
        Objects.requireNonNull(query, "query must not be null");
        results = results == null ? List.of() : List.copyOf(results);
    }

    public static SearchApiResponse from(SearchResponse response) {
        Objects.requireNonNull(response, "response must not be null");

        List<SearchResultResponse> results = response.results().stream()
                .map(SearchResultResponse::from)
                .toList();

        return new SearchApiResponse(
                response.query(),
                true,
                results.size(),
                response.retrieval().name(),
                response.tookMillis(),
                results);
    }
}
