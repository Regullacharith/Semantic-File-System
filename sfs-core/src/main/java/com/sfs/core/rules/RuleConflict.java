package com.sfs.core.rules;

import java.util.Objects;

public record RuleConflict(Severity severity, String description) {

    public enum Severity { ERROR, WARNING }

    public RuleConflict {
        Objects.requireNonNull(severity, "severity must not be null");
        Objects.requireNonNull(description, "description must not be null");
        if (description.isBlank()) {
            throw new IllegalArgumentException("conflict description must not be blank");
        }
    }

    public static RuleConflict error(String description) {
        return new RuleConflict(Severity.ERROR, description);
    }

    public static RuleConflict warning(String description) {
        return new RuleConflict(Severity.WARNING, description);
    }
}
