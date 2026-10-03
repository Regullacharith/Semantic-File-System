package com.sfs.contracts.security;

import com.sfs.contracts.semantic.ProtectedReferenceView.SensitiveType;

import java.util.Objects;

public record SecretSubmission(
        String referenceId,
        String objectId,
        SensitiveType sensitiveType,
        String plaintextValue) {

    public SecretSubmission {
        Objects.requireNonNull(referenceId, "referenceId must not be null");
        Objects.requireNonNull(objectId, "objectId must not be null");
        Objects.requireNonNull(sensitiveType, "sensitiveType must not be null");
        Objects.requireNonNull(plaintextValue, "plaintextValue must not be null");
        if (referenceId.isBlank() || plaintextValue.isBlank()) {
            throw new IllegalArgumentException(
                    "referenceId and plaintextValue must not be blank");
        }
    }
}
