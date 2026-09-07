package com.sfs.lifecycle.model;

import java.time.Instant;
import java.util.Objects;
import java.util.regex.Pattern;

public record FileVersion(
        int number,
        String contentSha256,
        long sizeBytes,
        Instant capturedAt) {

    private static final Pattern SHA256_PATTERN = Pattern.compile("^[0-9a-f]{64}$");

    public FileVersion {
        if (number < 1) {
            throw new IllegalArgumentException("version number must be 1 or greater");
        }
        Objects.requireNonNull(contentSha256, "contentSha256 must not be null");
        if (!SHA256_PATTERN.matcher(contentSha256).matches()) {
            throw new IllegalArgumentException(
                    "contentSha256 must be 64 lowercase hexadecimal characters");
        }
        if (sizeBytes < 0) {
            throw new IllegalArgumentException("sizeBytes must not be negative");
        }
        Objects.requireNonNull(capturedAt, "capturedAt must not be null");
    }
}
