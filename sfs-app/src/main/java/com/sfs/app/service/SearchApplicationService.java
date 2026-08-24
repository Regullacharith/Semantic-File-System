package com.sfs.app.service;

import com.sfs.app.api.request.SearchApiRequest;
import com.sfs.app.api.response.SearchApiResponse;
import com.sfs.contracts.search.SearchQuery;
import com.sfs.contracts.search.SearchService;

import java.util.Objects;

public class SearchApplicationService {

    private final SearchService searchService;

    public SearchApplicationService(SearchService searchService) {
        this.searchService = Objects.requireNonNull(searchService, "searchService must not be null");
    }

    public SearchApiResponse search(SearchApiRequest request) {
        Objects.requireNonNull(request, "request must not be null");

        SearchQuery query;
        try {
            query = new SearchQuery(request.text(), request.effectiveMaxResults());
        } catch (IllegalArgumentException e) {
            throw ApplicationException.validationFailed(e.getMessage());
        }

        return SearchApiResponse.from(searchService.search(query));
    }
}
