package com.sfs.core.dna;

import java.util.Objects;

public record Entity(String name, String type, int mentions) {

    public Entity {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(type, "type must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("entity name must not be blank");
        }
        if (type.isBlank()) {
            throw new IllegalArgumentException("entity type must not be blank");
        }
        if (mentions < 1) {
            throw new IllegalArgumentException("mentions must be at least 1");
        }
    }
}
