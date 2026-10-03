package com.sfs.security;

import com.sfs.contracts.security.HandlingPolicy;
import com.sfs.contracts.security.SensitiveTypePolicy;
import com.sfs.contracts.semantic.ProtectedReferenceView.SensitiveType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class SecurityPolicyEngine {

    private final Map<SensitiveType, SensitiveTypePolicy> policies =
            new LinkedHashMap<>();

    public SecurityPolicyEngine(List<SensitiveTypePolicy> policies) {
        Objects.requireNonNull(policies, "policies must not be null");
        for (SensitiveTypePolicy policy : policies) {
            this.policies.put(policy.sensitiveType(), policy);
        }
        for (SensitiveType type : SensitiveType.values()) {
            if (!this.policies.containsKey(type)) {
                throw new IllegalArgumentException(
                        "the policy table must cover every sensitive type; missing: "
                                + type);
            }
        }
    }

    public static SecurityPolicyEngine v1() {
        return new SecurityPolicyEngine(List.of(
                new SensitiveTypePolicy(SensitiveType.PASSWORD, HandlingPolicy.REDACT,
                        true,
                        "Passwords are never retained in recoverable form. A password's "
                                + "exact value has no legitimate reconstruction use, so "
                                + "storing it reversibly would add risk without benefit."),
                new SensitiveTypePolicy(SensitiveType.API_KEY, HandlingPolicy.ENCRYPT,
                        false,
                        "An exact API key may need to be recovered by an authorized "
                                + "operator, so it is held in the encrypted secure store "
                                + "behind a protected reference."),
                new SensitiveTypePolicy(SensitiveType.ACCESS_TOKEN,
                        HandlingPolicy.TOKENIZE, false,
                        "Tokens are short-lived, so recovering an expired value has "
                                + "little value and retaining it carries real risk."),
                new SensitiveTypePolicy(SensitiveType.EMAIL_ADDRESS,
                        HandlingPolicy.TOKENIZE, false,
                        "Tokenizing keeps repeated mentions of the same person "
                                + "consistent across a document without retaining the "
                                + "address itself."),
                new SensitiveTypePolicy(SensitiveType.PHONE_NUMBER,
                        HandlingPolicy.TOKENIZE, false,
                        "Preserves the semantic role of a contact number without "
                                + "retaining it."),
                new SensitiveTypePolicy(SensitiveType.POSTAL_ADDRESS,
                        HandlingPolicy.REDACT, false,
                        "Replaced with a placeholder describing its role in the "
                                + "document."),
                new SensitiveTypePolicy(SensitiveType.ACCOUNT_IDENTIFIER,
                        HandlingPolicy.TOKENIZE, false,
                        "Account identifiers are high-entropy and cannot be "
                                + "semantically inferred, so a consistent token preserves "
                                + "structure without retaining the value."),
                new SensitiveTypePolicy(SensitiveType.OTHER, HandlingPolicy.REDACT,
                        true,
                        "Anything detected as sensitive but not classified is redacted. "
                                + "Unclassified data fails closed.")));
    }

    public PolicyDecision decide(SensitiveValue value) {
        Objects.requireNonNull(value, "value must not be null");
        SensitiveTypePolicy policy = policies.get(value.type());
        String referenceId = referenceIdFor(value.exactValue());
        return new PolicyDecision(value.type(), policy.handling(), referenceId,
                policy.rationale(), policy.handling().isReversible());
    }

    public String referenceIdFor(String exactValue) {
        String normalized = exactValue.strip().toLowerCase(Locale.ROOT);
        return "sfs-ref-" + com.sfs.core.identity.Digests.sha256Hex(normalized)
                .substring(0, 12);
    }

    public List<SensitiveTypePolicy> policies() {
        return List.copyOf(policies.values());
    }

    public SensitiveTypePolicy policyFor(SensitiveType type) {
        return policies.get(type);
    }
}
