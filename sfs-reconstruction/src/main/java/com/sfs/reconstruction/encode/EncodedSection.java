package com.sfs.reconstruction.encode;

public record EncodedSection(String heading, int level, int order, boolean requiredByRules) {

    public EncodedSection {
        if (heading == null || heading.isBlank()) {
            throw new IllegalArgumentException("section heading must not be blank");
        }
        if (level < 1 || level > 6) {
            throw new IllegalArgumentException("section level must be between 1 and 6");
        }
        if (order < 0) {
            throw new IllegalArgumentException("section order must not be negative");
        }
    }
}
