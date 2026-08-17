package com.sfs.contracts.search;

import java.util.Objects;

/**
 * A semantic search request.
 */
public record SearchQuery(String text, int maxResults) {

    /** Default result count when the caller does not specify one. */
    public static final int DEFAULT_MAX_RESULTS = 20;

    /** Upper bound, guarding against a request for an unbounded result set. */
    public static final int MAX_ALLOWED_RESULTS = 100;

    /** Longest accepted query, guarding against pathological input. */
    public static final int MAX_QUERY_LENGTH = 500;

    /**
     * Object ID shape produced by the current identifier scheme.
     */
    private static final String OBJECT_ID_PATTERN = "^sfs-obj-[0-9]{4}-[a-zA-Z0-9]+$";

    /**
     * Canonical constructor.
     */
    public SearchQuery {
        Objects.requireNonNull(text, "text must not be null");

        text = text.strip();

        if (text.isBlank()) {
            throw new IllegalArgumentException("query text must not be blank");
        }
        if (text.length() > MAX_QUERY_LENGTH) {
            throw new IllegalArgumentException(
                    "query text must not exceed " + MAX_QUERY_LENGTH + " characters");
        }
        if (maxResults < 1 || maxResults > MAX_ALLOWED_RESULTS) {
            throw new IllegalArgumentException(
                    "maxResults must be between 1 and " + MAX_ALLOWED_RESULTS);
        }
    }

    /**
     * Creates a query with the default result limit.
     */
    public static SearchQuery of(String text) {
        return new SearchQuery(text, DEFAULT_MAX_RESULTS);
    }


    public boolean isObjectIdLookup() {
        return text.matches(OBJECT_ID_PATTERN);
    }
}
