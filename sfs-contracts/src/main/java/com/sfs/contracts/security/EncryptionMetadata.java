package com.sfs.contracts.security;

import java.time.Instant;
import java.util.Objects;

public record EncryptionMetadata(
        String algorithm,
        String keyId,
        String initializationVectorBase64,
        int ciphertextBytes,
        Instant encryptedAt) {

    public EncryptionMetadata {
        Objects.requireNonNull(algorithm, "algorithm must not be null");
        Objects.requireNonNull(keyId, "keyId must not be null");
        Objects.requireNonNull(initializationVectorBase64, "initializationVectorBase64 must not be null");
        Objects.requireNonNull(encryptedAt, "encryptedAt must not be null");
        if (algorithm.isBlank() || keyId.isBlank() || initializationVectorBase64.isBlank()) {
            throw new IllegalArgumentException(
                    "algorithm, keyId and initializationVectorBase64 must not be blank");
        }
        if (ciphertextBytes <= 0) {
            throw new IllegalArgumentException("ciphertextBytes must be positive");
        }
    }

    public static final String AES_256_GCM = "AES-256-GCM";
}
