package com.sfs.contracts.search;

import java.util.List;
import java.util.Objects;

/**
 * Outcome of a semantic search.
 */
public record SearchResponse(
        String query,
        List<SearchResult> results,
        RetrievalMode retrieval,
        long tookMillis) {

    /**
     * Canonical constructor.
     */
    public SearchResponse {
        Objects.requireNonNull(query, "query must not be null");
        Objects.requireNonNull(results, "results must not be null");
        Objects.requireNonNull(retrieval, "retrieval must not be null");

        if (query.isBlank()) {
            throw new IllegalArgumentException("query must not be blank");
        }
        if (tookMillis < 0) {
            throw new IllegalArgumentException("tookMillis must not be negative");
        }

        results = List.copyOf(results);
    }


    public boolean isEmpty() {
        return results.isEmpty();
    }

    /**
     * Number of matching records.
     */
    public int resultCount() {
        return results.size();
    }

    /**
     * How a set of results was obtained.
     */
    public enum RetrievalMode {

        /**
         * Resolved by exact Object ID, bypassing the vector index entirely.
         */
        OBJECT_ID_LOOKUP("Direct Object ID lookup"),

        /** Retrieved by meaning, through embedding similarity and reranking. */
        SEMANTIC("Semantic similarity search");

        private final String label;

        RetrievalMode(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }
}
