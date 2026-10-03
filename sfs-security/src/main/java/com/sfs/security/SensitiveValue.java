package com.sfs.security;

import com.sfs.contracts.semantic.ProtectedReferenceView.SensitiveType;

import java.util.Objects;

public record SensitiveValue(
        SensitiveType type,
        String exactValue,
        String semanticRole,
        String location,
        boolean credentialAssignment) {

    public SensitiveValue {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(exactValue, "exactValue must not be null");
        Objects.requireNonNull(semanticRole, "semanticRole must not be null");
        Objects.requireNonNull(location, "location must not be null");
        if (exactValue.isBlank()) {
            throw new IllegalArgumentException("exactValue must not be blank");
        }
    }
}
