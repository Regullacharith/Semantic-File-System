package com.sfs.contracts.security;

import java.util.List;
import java.util.Objects;

/**
 * The security and privacy configuration are presented for inspection.
 */
public record SecuritySettingsView(
        List<SensitiveTypePolicy> typePolicies,
        boolean embeddingsExcludeSecrets,
        boolean logsExcludeSecrets,
        boolean dnaExcludeSecrets,
        boolean authorizationRequired,
        String keyStorageDescription,
        List<AuditEventView> auditEvents) {

    public SecuritySettingsView {
        Objects.requireNonNull(keyStorageDescription, "keyStorageDescription must not be null");
        Objects.requireNonNull(typePolicies, "typePolicies must not be null");

        if (typePolicies.isEmpty()) {
            throw new IllegalArgumentException("at least one type policy must be configured");
        }
        if (keyStorageDescription.isBlank()) {
            throw new IllegalArgumentException("keyStorageDescription must not be blank");
        }

        if (!embeddingsExcludeSecrets) {
            throw new IllegalArgumentException(
                    "embeddings must never be generated from plaintext secrets");
        }
        if (!logsExcludeSecrets) {
            throw new IllegalArgumentException("logs must never contain plaintext secrets");
        }
        if (!dnaExcludeSecrets) {
            throw new IllegalArgumentException(
                    "unrestricted Semantic DNA must never contain plaintext secrets");
        }
        if (!authorizationRequired) {
            throw new IllegalArgumentException(
                    "resolving a protected reference must always require authorization");
        }

        typePolicies = List.copyOf(typePolicies);
        auditEvents = List.copyOf(
                Objects.requireNonNull(auditEvents, "auditEvents must not be null"));
    }

    public boolean allPoliciesNonReversible() {
        return typePolicies.stream().noneMatch(policy -> policy.handling().isReversible());
    }

    public record AuditEventView(String timestamp, String eventType, String detail, boolean permitted) {

        public AuditEventView {
            Objects.requireNonNull(timestamp, "timestamp must not be null");
            Objects.requireNonNull(eventType, "eventType must not be null");
            Objects.requireNonNull(detail, "detail must not be null");

            if (timestamp.isBlank() || eventType.isBlank() || detail.isBlank()) {
                throw new IllegalArgumentException("audit event fields must not be blank");
            }
        }
    }
}
