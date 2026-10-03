package com.sfs.contracts.security;

import com.sfs.contracts.semantic.ProtectedReferenceView.SensitiveType;

import java.util.Objects;

public record SecretRecord(
        String referenceId,
        String objectId,
        SensitiveType sensitiveType,
        String reversible,
        String ciphertextBase64,
        EncryptionMetadata metadata) {

    public SecretRecord {
        Objects.requireNonNull(referenceId, "referenceId must not be null");
        Objects.requireNonNull(objectId, "objectId must not be null");
        Objects.requireNonNull(sensitiveType, "sensitiveType must not be null");
        Objects.requireNonNull(reversible, "reversible must not be null");
        Objects.requireNonNull(ciphertextBase64, "ciphertextBase64 must not be null");
        Objects.requireNonNull(metadata, "metadata must not be null");
        if (referenceId.isBlank() || ciphertextBase64.isBlank() || reversible.isBlank()) {
            throw new IllegalArgumentException(
                    "referenceId, reversible and ciphertextBase64 must not be blank");
        }
        if (!sensitiveType.isReversibleByDefault()) {
            throw new IllegalArgumentException(
                    sensitiveType + " is never reversibly stored; refusing to "
                            + "record it as a secret");
        }
    }
}
