package com.sfs.core.rules;

import java.util.Objects;

public record RequiredFactConstraint(String statement, boolean failIfMissing)
        implements Constraint {

    public RequiredFactConstraint {
        Objects.requireNonNull(statement, "statement must not be null");
        if (statement.isBlank()) {
            throw new IllegalArgumentException("required fact statement must not be blank");
        }
    }
}
