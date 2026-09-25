package com.sfs.core.dna;

import java.util.Objects;

public record ProtectedReference(
        String referenceId,
        String sensitiveType,
        String semanticRole,
        String location) {

    public ProtectedReference {
        Objects.requireNonNull(referenceId, "referenceId must not be null");
        Objects.requireNonNull(sensitiveType, "sensitiveType must not be null");
        Objects.requireNonNull(semanticRole, "semanticRole must not be null");
        Objects.requireNonNull(location, "location must not be null");
        if (referenceId.isBlank() || sensitiveType.isBlank()
                || semanticRole.isBlank() || location.isBlank()) {
            throw new IllegalArgumentException(
                    "protected reference fields must not be blank");
        }
    }
}
