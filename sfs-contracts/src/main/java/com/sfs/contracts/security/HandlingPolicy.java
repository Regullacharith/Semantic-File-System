package com.sfs.contracts.security;

/**
 * handle detected sensitive value.
 */
public enum HandlingPolicy {

    REDACT("Redact",
            "Replace with a semantic placeholder. The exact value is discarded permanently.",
            false),

    TOKENIZE("Tokenize",
            "Replace with a one-way token. Occurrences stay consistent; the value cannot be derived.",
            false),

    ENCRYPT("Encrypt and store",
            "Hold the exact value in the encrypted secure store, resolvable only under authorization.",
            true);

    private final String label;
    private final String description;
    private final boolean reversible;

    HandlingPolicy(String label, String description, boolean reversible) {
        this.label = label;
        this.description = description;
        this.reversible = reversible;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public boolean isReversible() {
        return reversible;
    }
}
