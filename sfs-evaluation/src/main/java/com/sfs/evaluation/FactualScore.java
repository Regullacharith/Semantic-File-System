package com.sfs.evaluation;

import java.util.List;
import java.util.Objects;

public record FactualScore(int factsTotal, int factsPreserved,
                           CriticalFactScore critical, List<String> missingStatements) {

    public FactualScore {
        if (factsTotal < 0 || factsPreserved < 0) {
            throw new IllegalArgumentException("fact counts must not be negative");
        }
        if (factsPreserved > factsTotal) {
            throw new IllegalArgumentException(
                    "preserved facts cannot exceed the total");
        }
        Objects.requireNonNull(critical, "critical must not be null");
        missingStatements = missingStatements == null
                ? List.of() : List.copyOf(missingStatements);
    }

    public double toDimensionScore() {
        return factsTotal == 0 ? 1.0 : (double) factsPreserved / factsTotal;
    }

}
