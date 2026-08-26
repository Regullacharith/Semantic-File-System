package com.sfs.app.api.response;

import com.sfs.contracts.security.SecuritySettingsView;
import com.sfs.contracts.security.SensitiveTypePolicy;

import java.util.List;
import java.util.Objects;

public record SecuritySettingsResponse(
        List<TypePolicyResponse> typePolicies,
        boolean embeddingsExcludeSecrets,
        boolean logsExcludeSecrets,
        boolean dnaExcludeSecrets,
        boolean authorizationRequired,
        String keyStorageDescription,
        List<AuditEventResponse> auditEvents,
        boolean enforced,
        String enforcementNote) {

    private static final String NOT_ENFORCED_NOTE =
            "Reported configuration only. No detector, policy engine, encrypted store, "
                    + "key management or audit trail is implemented in V1. "
                    + "Enforcement arrives with Milestone 13.";

    public SecuritySettingsResponse {
        typePolicies = typePolicies == null ? List.of() : List.copyOf(typePolicies);
        auditEvents = auditEvents == null ? List.of() : List.copyOf(auditEvents);
    }

    public static SecuritySettingsResponse from(SecuritySettingsView settings) {
        Objects.requireNonNull(settings, "settings must not be null");

        return new SecuritySettingsResponse(
                settings.typePolicies().stream().map(TypePolicyResponse::from).toList(),
                settings.embeddingsExcludeSecrets(),
                settings.logsExcludeSecrets(),
                settings.dnaExcludeSecrets(),
                settings.authorizationRequired(),
                settings.keyStorageDescription(),
                settings.auditEvents().stream().map(AuditEventResponse::from).toList(),
                false,
                NOT_ENFORCED_NOTE);
    }

    public record TypePolicyResponse(
            String sensitiveType,
            String sensitiveTypeLabel,
            String handling,
            String handlingLabel,
            boolean reversible,
            boolean locked,
            String rationale) {

        public static TypePolicyResponse from(SensitiveTypePolicy policy) {
            return new TypePolicyResponse(
                    policy.sensitiveType().name(),
                    policy.sensitiveType().getLabel(),
                    policy.handling().name(),
                    policy.handling().getLabel(),
                    policy.handling().isReversible(),
                    policy.locked(),
                    policy.rationale());
        }
    }

    public record AuditEventResponse(
            String timestamp,
            String eventType,
            String detail,
            boolean permitted) {

        public static AuditEventResponse from(SecuritySettingsView.AuditEventView event) {
            return new AuditEventResponse(
                    event.timestamp(), event.eventType(), event.detail(), event.permitted());
        }
    }
}
