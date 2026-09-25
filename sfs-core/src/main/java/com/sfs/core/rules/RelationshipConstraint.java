package com.sfs.core.rules;

import java.util.Objects;

public record RelationshipConstraint(String subject, String type, String object,
                                     boolean failIfMissing) implements Constraint {

    public RelationshipConstraint {
        Objects.requireNonNull(subject, "subject must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(object, "object must not be null");
        if (subject.isBlank() || type.isBlank() || object.isBlank()) {
            throw new IllegalArgumentException("relationship constraint parts must not be blank");
        }
    }
}
