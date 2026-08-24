package com.sfs.app.api.request;

import java.util.Objects;

public record SearchApiRequest(String text, Integer maxResults) {

    public static final int MAX_TEXT_LENGTH = 500;
    public static final int MIN_RESULTS = 1;
    public static final int MAX_RESULTS = 100;
    public static final int DEFAULT_RESULTS = 20;

    public SearchApiRequest {
        Objects.requireNonNull(text, "text must not be null");

        String trimmed = text.strip();

        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("text must not be blank");
        }
        if (trimmed.length() > MAX_TEXT_LENGTH) {
            throw new IllegalArgumentException(
                    "text must not exceed " + MAX_TEXT_LENGTH + " characters");
        }
        if (maxResults != null && (maxResults < MIN_RESULTS || maxResults > MAX_RESULTS)) {
            throw new IllegalArgumentException(
                    "maxResults must be between " + MIN_RESULTS + " and " + MAX_RESULTS);
        }

        text = trimmed;
    }

    public int effectiveMaxResults() {
        return maxResults == null ? DEFAULT_RESULTS : maxResults;
    }
}
