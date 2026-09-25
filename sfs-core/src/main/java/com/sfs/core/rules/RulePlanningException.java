package com.sfs.core.rules;

import java.util.List;

public final class RulePlanningException extends RuntimeException {

    private final List<String> issues;

    public RulePlanningException(List<String> issues) {
        super("Reconstruction planning refused: " + String.join("; ", issues));
        this.issues = List.copyOf(issues);
    }

    public List<String> issues() {
        return issues;
    }
}
