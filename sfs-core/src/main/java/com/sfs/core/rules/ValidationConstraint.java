package com.sfs.core.rules;

public record ValidationConstraint(boolean forbidInventedFacts,
                                   boolean requireAllCriticalFacts,
                                   double minFactConfidence) implements Constraint {

    public ValidationConstraint {
        if (minFactConfidence < 0.0 || minFactConfidence > 1.0) {
            throw new IllegalArgumentException(
                    "minFactConfidence must be between 0.0 and 1.0");
        }
    }
}
