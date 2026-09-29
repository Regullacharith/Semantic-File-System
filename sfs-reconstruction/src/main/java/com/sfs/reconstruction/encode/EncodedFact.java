package com.sfs.reconstruction.encode;

import java.util.Objects;

public record EncodedFact(
        String statement, boolean critical, double confidence, boolean requiredByRules) {

    public static final String CRITICAL_MARKER = "critical";

    public EncodedFact {
        Objects.requireNonNull(statement, "statement must not be null");
        if (statement.isBlank()) {
            throw new IllegalArgumentException("fact statement must not be blank");
        }
        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("confidence must be between 0.0 and 1.0");
        }
    }
}
