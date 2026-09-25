package com.sfs.core.dna;

import java.util.Objects;

public record Fact(String statement, boolean critical, double confidence) {

    public Fact {
        Objects.requireNonNull(statement, "statement must not be null");
        if (statement.isBlank()) {
            throw new IllegalArgumentException("fact statement must not be blank");
        }
        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("confidence must be between 0.0 and 1.0");
        }
    }
}
