package com.sfs.ui.controller;

import com.sfs.contracts.search.SearchQuery;
import com.sfs.contracts.search.SearchResponse;
import com.sfs.contracts.search.SearchService;
import com.sfs.ui.view.NavigationItem;
import com.sfs.ui.view.PageViewModel;
import com.sfs.ui.view.SearchResultViewModel;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Semantic search view.
 */
@Controller
public class SearchController {

    private static final String VIEW_SEARCH = "search";

    private static final String ATTR_PAGE = "page";
    private static final String ATTR_QUERY = "query";
    private static final String ATTR_RESULTS = "results";
    private static final String ATTR_SEARCHED = "searched";
    private static final String ATTR_RETRIEVAL_MODE = "retrievalMode";
    private static final String ATTR_TOOK_MILLIS = "tookMillis";
    private static final String ATTR_ERROR = "error";

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/search")
    public String search(@RequestParam(name = "q", required = false) String queryText,
                         Model model) {

        model.addAttribute(ATTR_PAGE, PageViewModel.of("Search", NavigationItem.SEARCH));
        model.addAttribute(ATTR_QUERY, queryText == null ? "" : queryText);

        // First visit: 
        if (queryText == null || queryText.isBlank()) {
            model.addAttribute(ATTR_SEARCHED, false);
            model.addAttribute(ATTR_RESULTS, List.of());
            return VIEW_SEARCH;
        }

        model.addAttribute(ATTR_SEARCHED, true);

        final SearchQuery query;
        try {
            query = SearchQuery.of(queryText);
        } catch (IllegalArgumentException e) {
            // Rejected input produces an explicit message rather than an empty result set,
            // so the user is never left unsure whether the search ran.
            model.addAttribute(ATTR_ERROR, "Query rejected: " + e.getMessage());
            model.addAttribute(ATTR_RESULTS, List.of());
            return VIEW_SEARCH;
        }

        SearchResponse response = searchService.search(query);

        model.addAttribute(ATTR_RESULTS,
                response.results().stream().map(SearchResultViewModel::from).toList());
        model.addAttribute(ATTR_RETRIEVAL_MODE, response.retrieval().getLabel());
        model.addAttribute(ATTR_TOOK_MILLIS, response.tookMillis());

        return VIEW_SEARCH;
    }
}
