package com.sfs.evaluation;

import java.util.Objects;

public record ImprovementSuggestion(ErrorCategory category, long occurrences,
                                    String advice, boolean correctness) {

    public ImprovementSuggestion {
        Objects.requireNonNull(category, "category must not be null");
        Objects.requireNonNull(advice, "advice must not be null");
        if (occurrences < 0) {
            throw new IllegalArgumentException("occurrences must not be negative");
        }
    }
}
