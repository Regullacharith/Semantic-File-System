package com.sfs.core.dna;

import java.util.List;
import java.util.Objects;

public record EmbeddingRef(String algorithm, int dimensions, List<Double> vector) {

    public EmbeddingRef {
        Objects.requireNonNull(algorithm, "algorithm must not be null");
        Objects.requireNonNull(vector, "vector must not be null");
        if (algorithm.isBlank()) {
            throw new IllegalArgumentException("algorithm must not be blank");
        }
        if (dimensions < 0) {
            throw new IllegalArgumentException("dimensions must not be negative");
        }
        if (vector.size() != dimensions) {
            throw new IllegalArgumentException(
                    "vector length must match the declared dimensions");
        }
        for (double value : vector) {
            if (!Double.isFinite(value)) {
                throw new IllegalArgumentException("vector values must be finite");
            }
        }
        vector = List.copyOf(vector);
    }

    public static EmbeddingRef absent() {
        return new EmbeddingRef("none", 0, List.of());
    }

    public boolean present() {
        return dimensions > 0;
    }
}
