package com.sfs.evaluation;

public record StructuralScore(int headingsTotal, int headingsPreserved,
                              boolean orderPreserved, boolean summaryVerbatim) {

    public StructuralScore {
        if (headingsTotal < 0 || headingsPreserved < 0) {
            throw new IllegalArgumentException("heading counts must not be negative");
        }
        if (headingsPreserved > headingsTotal) {
            throw new IllegalArgumentException(
                    "preserved headings cannot exceed the total");
        }
    }

    public double toDimensionScore() {
        double headings = headingsTotal == 0
                ? 1.0
                : (double) headingsPreserved / headingsTotal;
        double summary = summaryVerbatim ? 1.0 : 0.0;
        double order = orderPreserved ? 1.0 : 0.5;
        return (headings + summary + order) / 3.0;
    }
}
