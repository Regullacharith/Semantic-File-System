package com.sfs.core.rules;

import java.util.List;
import java.util.Objects;

public record Rule(String ruleId, RuleType type, RulePriority priority,
                   String description, List<Constraint> constraints) {

    public Rule {
        Objects.requireNonNull(ruleId, "ruleId must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(priority, "priority must not be null");
        Objects.requireNonNull(description, "description must not be null");
        Objects.requireNonNull(constraints, "constraints must not be null");
        if (ruleId.isBlank() || !ruleId.matches("[a-z0-9-]+")) {
            throw new IllegalArgumentException(
                    "rule id must be lowercase letters, digits and dashes");
        }
        if (description.isBlank()) {
            throw new IllegalArgumentException("description must not be blank");
        }
        if (constraints.isEmpty()) {
            throw new IllegalArgumentException("a rule needs at least one constraint");
        }
        constraints = List.copyOf(constraints);
    }
}
