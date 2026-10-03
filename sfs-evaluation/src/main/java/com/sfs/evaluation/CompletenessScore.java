package com.sfs.evaluation;

public record CompletenessScore(int unitsTotal, int unitsPresent) {

    public CompletenessScore {
        if (unitsTotal < 0 || unitsPresent < 0) {
            throw new IllegalArgumentException("unit counts must not be negative");
        }
        if (unitsPresent > unitsTotal) {
            throw new IllegalArgumentException(
                    "present units cannot exceed the total");
        }
    }

    public double toDimensionScore() {
        return unitsTotal == 0 ? 1.0 : (double) unitsPresent / unitsTotal;
    }
}
