package com.sfs.contracts.semantic;

import java.util.Objects;

public record ProtectedReferenceView(
        String referenceId,
        SensitiveType sensitiveType,
        String semanticRole,
        String location,
        boolean resolvable) {

    public ProtectedReferenceView {
        Objects.requireNonNull(referenceId, "referenceId must not be null");
        Objects.requireNonNull(sensitiveType, "sensitiveType must not be null");
        Objects.requireNonNull(semanticRole, "semanticRole must not be null");
        Objects.requireNonNull(location, "location must not be null");

        if (referenceId.isBlank()) {
            throw new IllegalArgumentException("referenceId must not be blank");
        }
        if (semanticRole.isBlank()) {
            throw new IllegalArgumentException("semanticRole must not be blank");
        }
        if (location.isBlank()) {
            throw new IllegalArgumentException("location must not be blank");
        }
    }

    public enum SensitiveType {

        PASSWORD("Password", false),

        API_KEY("API key", true),

        ACCESS_TOKEN("Access token", true),

        EMAIL_ADDRESS("Email address", true),

        PHONE_NUMBER("Phone number", true),

        POSTAL_ADDRESS("Postal address", true),

        ACCOUNT_IDENTIFIER("Account identifier", true),

        OTHER("Other sensitive value", false);

        private final String label;
        private final boolean reversibleByDefault;

        SensitiveType(String label, boolean reversibleByDefault) {
            this.label = label;
            this.reversibleByDefault = reversibleByDefault;
        }

        public String getLabel() {
            return label;
        }

        public boolean isReversibleByDefault() {
            return reversibleByDefault;
        }
    }
}
