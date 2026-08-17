package com.sfs.contracts.search;

/**
 * Application-facing contract for semantic search.
 */
public interface SearchService {

    /**
     * Executes a semantic search, or a direct lookup when the query is an exact Object ID.
     */
    SearchResponse search(SearchQuery query);
}
