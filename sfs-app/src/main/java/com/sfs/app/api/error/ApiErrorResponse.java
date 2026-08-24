package com.sfs.app.api.error;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ApiErrorResponse(
        String code,
        String message,
        int status,
        Instant timestamp,
        String path,
        List<FieldIssue> details) {

    private static final int MAX_PATH_LENGTH = 200;

    public ApiErrorResponse {
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(message, "message must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");

        if (code.isBlank()) {
            throw new IllegalArgumentException("code must not be blank");
        }
        if (message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
        if (status < 400 || status > 599) {
            throw new IllegalArgumentException("status must be an HTTP error status, got " + status);
        }

        details = details == null ? List.of() : List.copyOf(details);
    }

    public static ApiErrorResponse of(ApiErrorCode code, String message, String path) {
        return new ApiErrorResponse(
                code.name(), message, code.status(), Instant.now(), sanitize(path), List.of());
    }

    public static ApiErrorResponse of(ApiErrorCode code,
                                      String message,
                                      String path,
                                      List<FieldIssue> details) {
        return new ApiErrorResponse(
                code.name(), message, code.status(), Instant.now(), sanitize(path), details);
    }

    private static String sanitize(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }

        StringBuilder cleaned = new StringBuilder(path.length());
        for (int i = 0; i < path.length() && cleaned.length() < MAX_PATH_LENGTH; i++) {
            char c = path.charAt(i);
            cleaned.append(Character.isISOControl(c) ? ' ' : c);
        }

        return cleaned.toString();
    }

    public record FieldIssue(String field, String issue) {

        public FieldIssue {
            Objects.requireNonNull(field, "field must not be null");
            Objects.requireNonNull(issue, "issue must not be null");

            if (field.isBlank()) {
                throw new IllegalArgumentException("field must not be blank");
            }
            if (issue.isBlank()) {
                throw new IllegalArgumentException("issue must not be blank");
            }
        }
    }
}
