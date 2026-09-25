package com.sfs.core.dna;

import java.util.Objects;

public record FidelityProfile(
        double extractionConfidence,
        double structuralCompleteness,
        String analyzerVersion) {

    public FidelityProfile {
        Objects.requireNonNull(analyzerVersion, "analyzerVersion must not be null");
        if (analyzerVersion.isBlank()) {
            throw new IllegalArgumentException("analyzerVersion must not be blank");
        }
        if (extractionConfidence < 0.0 || extractionConfidence > 1.0) {
            throw new IllegalArgumentException(
                    "extractionConfidence must be between 0.0 and 1.0");
        }
        if (structuralCompleteness < 0.0 || structuralCompleteness > 1.0) {
            throw new IllegalArgumentException(
                    "structuralCompleteness must be between 0.0 and 1.0");
        }
    }
}
