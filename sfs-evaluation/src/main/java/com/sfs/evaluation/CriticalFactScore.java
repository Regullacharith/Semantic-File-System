package com.sfs.evaluation;

import java.util.List;

public record CriticalFactScore(int total, int preserved, List<String> missing) {

    public CriticalFactScore {
        if (total < 0 || preserved < 0) {
            throw new IllegalArgumentException("fact counts must not be negative");
        }
        if (preserved > total) {
            throw new IllegalArgumentException(
                    "preserved critical facts cannot exceed the total");
        }
        missing = missing == null ? List.of() : List.copyOf(missing);
    }

    public double rate() {
        return total == 0 ? 1.0 : (double) preserved / total;
    }
}
