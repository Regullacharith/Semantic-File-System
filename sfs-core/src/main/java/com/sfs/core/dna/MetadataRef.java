package com.sfs.core.dna;

import java.time.Instant;
import java.util.Objects;

public record MetadataRef(
        String objectId,
        String displayName,
        String contentType,
        long sizeBytes,
        Instant registeredAt) {

    public MetadataRef {
        Objects.requireNonNull(objectId, "objectId must not be null");
        Objects.requireNonNull(displayName, "displayName must not be null");
        Objects.requireNonNull(contentType, "contentType must not be null");
        Objects.requireNonNull(registeredAt, "registeredAt must not be null");
        if (objectId.isBlank()) {
            throw new IllegalArgumentException("objectId must not be blank");
        }
        if (displayName.isBlank()) {
            throw new IllegalArgumentException("displayName must not be blank");
        }
        if (contentType.isBlank()) {
            throw new IllegalArgumentException("contentType must not be blank");
        }
        if (sizeBytes < 0) {
            throw new IllegalArgumentException("sizeBytes must not be negative");
        }
    }
}
