package com.sfs.app.api.request;

import java.util.Objects;

public record FileImportApiRequest(String fileName, String content, String contentType) {

    private static final int MAX_NAME_LENGTH = 255;

    public FileImportApiRequest {
        Objects.requireNonNull(fileName, "fileName must not be null");
        Objects.requireNonNull(content, "content must not be null");

        String trimmedName = fileName.strip();

        if (trimmedName.isEmpty()) {
            throw new IllegalArgumentException("fileName must not be blank");
        }
        if (trimmedName.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "fileName must not exceed " + MAX_NAME_LENGTH + " characters");
        }
        if (trimmedName.contains("/") || trimmedName.contains("\\")) {
            throw new IllegalArgumentException("fileName must not contain a path separator");
        }
        if (trimmedName.contains("..")) {
            throw new IllegalArgumentException("fileName must not contain a parent-directory reference");
        }
        if (content.isBlank()) {
            throw new IllegalArgumentException("content must not be blank");
        }

        fileName = trimmedName;
    }
}
