package com.sfs.contracts.file;

import java.time.Instant;
import java.util.Objects;

/**
 * Summary of a file as presented in a list.
 * @param objectId    stable logical identity, never blank
 * @param displayName file name shown to the user, never blank
 * @param status      lifecycle status, never null
 * @param sizeBytes   original size in bytes, never negative
 * @param registeredAt when the file was registered, never null
 * @param analyzedAt  when analysis completed, or null if it has not
 */
public record FileSummary(
        String objectId,
        String displayName,
        FileStatus status,
        long sizeBytes,
        Instant registeredAt,
        Instant analyzedAt) {

    /**
     * Canonical constructor.
     */
    public FileSummary {
        Objects.requireNonNull(objectId, "objectId must not be null");
        Objects.requireNonNull(displayName, "displayName must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(registeredAt, "registeredAt must not be null");

        if (objectId.isBlank()) {
            throw new IllegalArgumentException("objectId must not be blank");
        }
        if (displayName.isBlank()) {
            throw new IllegalArgumentException("displayName must not be blank");
        }
        if (sizeBytes < 0) {
            throw new IllegalArgumentException("sizeBytes must not be negative");
        }
    }

    public boolean isAnalyzed() {
        return analyzedAt != null;
    }
}
