package com.sfs.security;

import com.sfs.contracts.security.Capability;
import com.sfs.contracts.security.HandlingPolicy;
import com.sfs.contracts.security.SecuritySettingsView;
import com.sfs.contracts.security.SensitiveTypePolicy;
import com.sfs.contracts.semantic.ProtectedReferenceView.SensitiveType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Authentication, authorization and real security settings (13.6, 13.8, 13.9)")
class IdentityAndSettingsTest {

    @Test
    @DisplayName("bootstrap identities authenticate with their recorded capabilities")
    void identities() {
        SecurityAuditLog auditLog = new SecurityAuditLog();
        IdentityAuthenticationService authentication =
                new IdentityAuthenticationService(auditLog);

        var custodian = authentication.authenticate("custodian").orElseThrow();
        var operator = authentication.authenticate("OPERATOR").orElseThrow();
        var reader = authentication.authenticate("reader").orElseThrow();

        assertThat(custodian.has(Capability.RESOLVE_SECRET)).isTrue();
        assertThat(custodian.has(Capability.PURGE_RAW)).isTrue();
        assertThat(operator.has(Capability.RESOLVE_SECRET)).isFalse();
        assertThat(operator.has(Capability.WRITE)).isTrue();
        assertThat(reader.capabilities()).containsExactly(Capability.READ);
    }

    @Test
    @DisplayName("unknown and blank credentials fail closed and are audited")
    void unknownCredentials() {
        SecurityAuditLog auditLog = new SecurityAuditLog();
        IdentityAuthenticationService authentication =
                new IdentityAuthenticationService(auditLog);

        assertThat(authentication.authenticate("ghost")).isEmpty();
        assertThat(authentication.authenticate(null)).isEmpty();
        assertThat(authentication.authenticate("  ")).isEmpty();
        assertThat(auditLog.events()).anySatisfy(event ->
                assertThat(event.eventType()).isEqualTo("Authentication failed"));
    }

    @Test
    @DisplayName("no plaintext credential is retained by the identity table")
    void noPlaintextCredentials() {
        SecurityAuditLog auditLog = new SecurityAuditLog();
        IdentityAuthenticationService authentication =
                new IdentityAuthenticationService(auditLog);

        authentication.register("temp", "plain-secret-value", "Temporary",
                java.util.Set.of(Capability.READ));

        String serviceState = authentication.toString();
        assertThat(serviceState).doesNotContain("plain-secret-value");
    }

    @Test
    @DisplayName("settings report the live policy table, mandatory protections and audit")
    void settings() {
        SecurityAuditLog auditLog = new SecurityAuditLog();
        auditLog.record("Sensitive value detected",
                "API key detected and stored as sfs-ref-aaaaaaaa0100.", true);
        PolicySecuritySettingsService service = new PolicySecuritySettingsService(
                SecurityPolicyEngine.v1(), auditLog,
                "master key separate from ciphertext");

        SecuritySettingsView settings = service.getSettings();

        assertThat(settings.embeddingsExcludeSecrets()).isTrue();
        assertThat(settings.logsExcludeSecrets()).isTrue();
        assertThat(settings.dnaExcludeSecrets()).isTrue();
        assertThat(settings.authorizationRequired()).isTrue();
        assertThat(settings.keyStorageDescription()).contains("separate");
        assertThat(settings.typePolicies())
                .extracting(SensitiveTypePolicy::sensitiveType)
                .containsExactlyInAnyOrder(SensitiveType.values());
        assertThat(settings.typePolicies())
                .filteredOn(policy -> policy.sensitiveType() == SensitiveType.PASSWORD)
                .allSatisfy(policy -> {
                    assertThat(policy.handling()).isEqualTo(HandlingPolicy.REDACT);
                    assertThat(policy.locked()).isTrue();
                });
        assertThat(settings.auditEvents()).hasSize(1);
        assertThat(settings.allPoliciesNonReversible()).isFalse();
    }
}
