package com.sfs.reconstruction.engine;

import java.util.Objects;

public record ReconstructionMetadata(
        String objectId,
        String dnaVersion,
        String dnaSha256,
        String rulesVersion,
        String modelId,
        long planNanos,
        long reconstructNanos,
        long verifyNanos,
        int dnaBytes,
        int artifactBytes) {

    public ReconstructionMetadata {
        Objects.requireNonNull(objectId, "objectId must not be null");
        Objects.requireNonNull(dnaVersion, "dnaVersion must not be null");
        Objects.requireNonNull(dnaSha256, "dnaSha256 must not be null");
        Objects.requireNonNull(rulesVersion, "rulesVersion must not be null");
        Objects.requireNonNull(modelId, "modelId must not be null");
        if (objectId.isBlank() || dnaVersion.isBlank() || modelId.isBlank()) {
            throw new IllegalArgumentException(
                    "objectId, dnaVersion and modelId must be recorded");
        }
        if (planNanos < 0 || reconstructNanos < 0 || verifyNanos < 0) {
            throw new IllegalArgumentException("nanos must not be negative");
        }
        if (dnaBytes <= 0) {
            throw new IllegalArgumentException("dnaBytes must be positive");
        }
        if (artifactBytes <= 0) {
            throw new IllegalArgumentException("artifactBytes must be positive");
        }
    }

    public double knowledgePreservationDensity() {
        return (double) dnaBytes / artifactBytes;
    }

    public long totalNanos() {
        return planNanos + reconstructNanos + verifyNanos;
    }
}
