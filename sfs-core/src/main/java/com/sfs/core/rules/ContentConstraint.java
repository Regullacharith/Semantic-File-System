package com.sfs.core.rules;

import java.util.Objects;

public record ContentConstraint(boolean preserveSummaryVerbatim,
                                int minConcepts,
                                int minTopics) implements Constraint {

    public ContentConstraint {
        if (minConcepts < 0 || minTopics < 0) {
            throw new IllegalArgumentException("minimum counts must not be negative");
        }
    }
}
