package com.sfs.security;

import com.sfs.contracts.security.Principal;
import com.sfs.contracts.security.SecretSubmission;
import com.sfs.contracts.semantic.ProtectedReferenceView.SensitiveType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Secret resolution under authorization (13.6)")
class SecretResolutionServiceTest {

    @TempDir
    Path root;

    private FileEncryptedSecureStore secureStore;
    private SecurityAuditLog auditLog;
    private SecretResolutionService service;
    private Principal custodian;
    private Principal operator;
    private Principal reader;

    @BeforeEach
    void setUp() {
        secureStore = new FileEncryptedSecureStore(
                root.resolve("secure"), new FileKeyManager(root.resolve("keys")));
        auditLog = new SecurityAuditLog();
        SecurityPolicyEngine policyEngine = SecurityPolicyEngine.v1();
        IdentityAuthenticationService authentication =
                new IdentityAuthenticationService(auditLog);
        custodian = authentication.authenticate("custodian").orElseThrow();
        operator = authentication.authenticate("operator").orElseThrow();
        reader = authentication.authenticate("reader").orElseThrow();
        service = new SecretResolutionService(secureStore,
                new PolicyAuthorizationService(auditLog), policyEngine, auditLog);
        secureStore.encryptAndStore(new SecretSubmission(
                "sfs-ref-aaaaaaaa0100", "sfs-obj-7002-bbbbbbbb",
                SensitiveType.API_KEY, "sk-live-9f8e7d6c5b4a"));
    }

    @Test
    @DisplayName("the custodian resolves a permitted reference and the access is audited")
    void custodianResolves() {
        String value = service.resolve("sfs-ref-aaaaaaaa0100", custodian);

        assertThat(value).isEqualTo("sk-live-9f8e7d6c5b4a");
        assertThat(auditLog.events()).anySatisfy(event -> {
            assertThat(event.eventType()).isEqualTo("Reference resolution permitted");
            assertThat(event.permitted()).isTrue();
        });
    }

    @Test
    @DisplayName("operators and readers are denied and the denial is audited")
    void othersDenied() {
        assertThatThrownBy(() -> service.resolve("sfs-ref-aaaaaaaa0100", operator))
                .isInstanceOf(SecretResolutionDeniedException.class)
                .hasMessageContaining("resolve capability");
        assertThatThrownBy(() -> service.resolve("sfs-ref-aaaaaaaa0100", reader))
                .isInstanceOf(SecretResolutionDeniedException.class);

        assertThat(auditLog.events()).anySatisfy(event ->
                assertThat(event.eventType())
                        .isEqualTo("Reference resolution denied"));
    }

    @Test
    @DisplayName("an unknown reference is denied explicitly")
    void unknownReferenceDenied() {
        assertThatThrownBy(() -> service.resolve("sfs-ref-aaaaaaaa9999", custodian))
                .isInstanceOf(SecretResolutionDeniedException.class)
                .hasMessageContaining("No stored secret");
    }

    @Test
    @DisplayName("an anonymous principal is refused without audit side effects on values")
    void anonymousRefused() {
        Principal anonymous = new Principal("anon", "Anonymous", Set.of());

        assertThatThrownBy(() -> service.resolve("sfs-ref-aaaaaaaa0100", anonymous))
                .isInstanceOf(SecretResolutionDeniedException.class);
    }

    @Test
    @DisplayName("policy resolvability follows the stored record's type")
    void policyResolvability() {
        assertThat(service.isPolicyResolvable("sfs-ref-aaaaaaaa0100")).isTrue();
        assertThat(service.isPolicyResolvable("sfs-ref-aaaaaaaa9999")).isFalse();
    }
}
