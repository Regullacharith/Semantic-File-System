package com.sfs.reconstruction.encode;

import java.util.Objects;

public record EncodedRelationship(
        String subject, String type, String object, boolean requiredByRules) {

    public EncodedRelationship {
        Objects.requireNonNull(subject, "subject must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(object, "object must not be null");
        if (subject.isBlank() || type.isBlank() || object.isBlank()) {
            throw new IllegalArgumentException("relationship elements must not be blank");
        }
    }
}
