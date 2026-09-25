package com.sfs.core.dna;

import java.util.Objects;

public record Concept(String name) {

    public Concept {
        Objects.requireNonNull(name, "name must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("concept name must not be blank");
        }
    }
}
