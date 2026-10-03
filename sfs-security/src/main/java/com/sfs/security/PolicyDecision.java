package com.sfs.security;

import com.sfs.contracts.security.HandlingPolicy;
import com.sfs.contracts.semantic.ProtectedReferenceView.SensitiveType;

import java.util.Objects;

public record PolicyDecision(
        SensitiveType type,
        HandlingPolicy handling,
        String referenceId,
        String rationale,
        boolean resolvable) {

    public PolicyDecision {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(handling, "handling must not be null");
        Objects.requireNonNull(referenceId, "referenceId must not be null");
        Objects.requireNonNull(rationale, "rationale must not be null");
        if (referenceId.isBlank()) {
            throw new IllegalArgumentException("referenceId must not be blank");
        }
        if (handling.isReversible() && !resolvable) {
            throw new IllegalArgumentException(
                    "a reversible decision must be resolvable under authorization");
        }
    }
}
