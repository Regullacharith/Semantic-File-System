package com.sfs.core.dna;

import java.util.Objects;

public record Topic(String name) {

    public Topic {
        Objects.requireNonNull(name, "name must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("topic name must not be blank");
        }
    }
}
