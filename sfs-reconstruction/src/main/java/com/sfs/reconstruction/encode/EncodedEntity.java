package com.sfs.reconstruction.encode;

import java.util.Objects;

public record EncodedEntity(String name, String type, int mentions, int minMentions) {

    public EncodedEntity {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(type, "type must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("entity name must not be blank");
        }
        if (type.isBlank()) {
            throw new IllegalArgumentException("entity type must not be blank");
        }
        if (mentions < 0 || minMentions < 0) {
            throw new IllegalArgumentException("mention counts must not be negative");
        }
    }

    public int requiredEmissions() {
        return Math.max(1, minMentions);
    }
}
