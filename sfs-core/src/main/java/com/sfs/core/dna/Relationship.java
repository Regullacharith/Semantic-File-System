package com.sfs.core.dna;

import java.util.Objects;

public record Relationship(String subject, String type, String object) {

    public Relationship {
        Objects.requireNonNull(subject, "subject must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(object, "object must not be null");
        if (subject.isBlank() || type.isBlank() || object.isBlank()) {
            throw new IllegalArgumentException("relationship parts must not be blank");
        }
    }
}
