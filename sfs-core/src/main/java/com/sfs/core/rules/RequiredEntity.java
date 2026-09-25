package com.sfs.core.rules;

import java.util.Objects;

public record RequiredEntity(String name, int minMentions) {

    public RequiredEntity {
        Objects.requireNonNull(name, "name must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("required entity name must not be blank");
        }
        if (minMentions < 1) {
            throw new IllegalArgumentException("minMentions must be at least 1");
        }
    }
}
