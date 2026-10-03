package com.sfs.evaluation;

import java.util.List;

public record EntityScore(int total, int preserved, List<String> missing) {

    public EntityScore {
        if (total < 0 || preserved < 0) {
            throw new IllegalArgumentException("entity counts must not be negative");
        }
        if (preserved > total) {
            throw new IllegalArgumentException(
                    "preserved entities cannot exceed the total");
        }
        missing = missing == null ? List.of() : List.copyOf(missing);
    }

    public double toDimensionScore() {
        return total == 0 ? 1.0 : (double) preserved / total;
    }
}
