package com.sfs.contracts.security;

import com.sfs.contracts.semantic.ProtectedReferenceView.SensitiveType;

import java.util.Objects;
import java.util.Set;

/**
 * The configured handling policy for category of sensitive value.
 */
public record SensitiveTypePolicy(
        SensitiveType sensitiveType,
        HandlingPolicy handling,
        boolean locked,
        String rationale) {
    private static final Set<SensitiveType> NEVER_REVERSIBLE = Set.of(SensitiveType.PASSWORD);

    public SensitiveTypePolicy {
        Objects.requireNonNull(sensitiveType, "sensitiveType must not be null");
        Objects.requireNonNull(handling, "handling must not be null");
        Objects.requireNonNull(rationale, "rationale must not be null");

        if (rationale.isBlank()) {
            throw new IllegalArgumentException("rationale must not be blank");
        }

        if (NEVER_REVERSIBLE.contains(sensitiveType) && handling.isReversible()) {
            throw new IllegalArgumentException(
                    sensitiveType + " must use a non-reversible policy; "
                            + handling + " is reversible and is not permitted for this type");
        }
    }

    public static boolean mayBeReversible(SensitiveType sensitiveType) {
        return !NEVER_REVERSIBLE.contains(sensitiveType);
    }

    public Set<HandlingPolicy> permittedOptions() {
        if (!mayBeReversible(sensitiveType)) {
            return Set.of(HandlingPolicy.REDACT, HandlingPolicy.TOKENIZE);
        }
        return Set.of(HandlingPolicy.values());
    }
}
