package com.sfs.core.rules;

import java.util.Objects;

public record RequiredFact(String statement) {

    public RequiredFact {
        Objects.requireNonNull(statement, "statement must not be null");
        if (statement.isBlank()) {
            throw new IllegalArgumentException("required fact statement must not be blank");
        }
    }
}
