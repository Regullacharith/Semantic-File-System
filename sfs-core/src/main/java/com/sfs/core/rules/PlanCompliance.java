package com.sfs.core.rules;

import java.util.List;
import java.util.Objects;

public record PlanCompliance(List<String> violations, List<String> warnings) {

    public PlanCompliance {
        Objects.requireNonNull(violations, "violations must not be null");
        Objects.requireNonNull(warnings, "warnings must not be null");
        violations = List.copyOf(violations);
        warnings = List.copyOf(warnings);
    }

    public static PlanCompliance clean() {
        return new PlanCompliance(List.of(), List.of());
    }

    public boolean satisfied() {
        return violations.isEmpty();
    }
}
