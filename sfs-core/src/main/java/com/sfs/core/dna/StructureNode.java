package com.sfs.core.dna;

import java.util.Objects;

public record StructureNode(String heading, int level, int order) {

    public StructureNode {
        Objects.requireNonNull(heading, "heading must not be null");
        if (heading.isBlank()) {
            throw new IllegalArgumentException("heading must not be blank");
        }
        if (level < 1 || level > 6) {
            throw new IllegalArgumentException("level must be between 1 and 6");
        }
        if (order < 0) {
            throw new IllegalArgumentException("order must not be negative");
        }
    }
}
