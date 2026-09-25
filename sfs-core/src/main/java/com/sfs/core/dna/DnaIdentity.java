package com.sfs.core.dna;

import java.time.Instant;
import java.util.Objects;

public record DnaIdentity(
        String objectId,
        String schemaVersion,
        int dnaVersion,
        String engineVersion,
        Instant generatedAt) {

    public DnaIdentity {
        Objects.requireNonNull(objectId, "objectId must not be null");
        Objects.requireNonNull(schemaVersion, "schemaVersion must not be null");
        Objects.requireNonNull(engineVersion, "engineVersion must not be null");
        Objects.requireNonNull(generatedAt, "generatedAt must not be null");
        if (objectId.isBlank()) {
            throw new IllegalArgumentException("objectId must not be blank");
        }
        if (schemaVersion.isBlank()) {
            throw new IllegalArgumentException("schemaVersion must not be blank");
        }
        if (engineVersion.isBlank()) {
            throw new IllegalArgumentException("engineVersion must not be blank");
        }
        if (dnaVersion < 1) {
            throw new IllegalArgumentException("dnaVersion must be at least 1");
        }
    }
}
