package com.sfs.evaluation;

import com.sfs.core.dna.SemanticDna;

import java.util.Objects;

public record EvaluationInput(String objectId, String originalText,
                              String artifactText, SemanticDna dna) {

    public EvaluationInput {
        Objects.requireNonNull(objectId, "objectId must not be null");
        Objects.requireNonNull(artifactText, "artifactText must not be null");
        Objects.requireNonNull(dna, "dna must not be null");
        if (objectId.isBlank()) {
            throw new IllegalArgumentException("objectId must not be blank");
        }
        if (artifactText.isBlank()) {
            throw new IllegalArgumentException("artifactText must not be blank");
        }
    }

    public static EvaluationInput of(String objectId, String originalText,
                                     String artifactText, SemanticDna dna) {
        return new EvaluationInput(objectId, originalText, artifactText, dna);
    }

    public boolean hasOriginal() {
        return originalText != null && !originalText.isBlank();
    }
}
