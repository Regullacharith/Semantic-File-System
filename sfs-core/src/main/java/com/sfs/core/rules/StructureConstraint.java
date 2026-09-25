package com.sfs.core.rules;

import java.util.List;
import java.util.Objects;

public record StructureConstraint(int minHeadings, int maxHeadings,
                                  List<String> requiredHeadings) implements Constraint {

    public StructureConstraint {
        Objects.requireNonNull(requiredHeadings, "requiredHeadings must not be null");
        if (minHeadings < 0 || maxHeadings < minHeadings) {
            throw new IllegalArgumentException(
                    "heading bounds are invalid: min " + minHeadings + ", max " + maxHeadings);
        }
        requiredHeadings = List.copyOf(requiredHeadings);
    }
}
