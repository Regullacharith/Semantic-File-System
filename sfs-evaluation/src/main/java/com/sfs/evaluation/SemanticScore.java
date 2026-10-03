package com.sfs.evaluation;

import java.util.Objects;

public record SemanticScore(double f1, int referenceTokens, int matchedTokens,
                            int artifactOnlyTokens) {

    public SemanticScore {
        Objects.requireNonNull(referenceTokens, "referenceTokens must not be null");
        if (f1 < 0.0 || f1 > 1.0) {
            throw new IllegalArgumentException("f1 must be between 0.0 and 1.0");
        }
        if (referenceTokens < 0 || matchedTokens < 0 || artifactOnlyTokens < 0) {
            throw new IllegalArgumentException("token counts must not be negative");
        }
    }

    public double toDimensionScore() {
        return f1;
    }
}
