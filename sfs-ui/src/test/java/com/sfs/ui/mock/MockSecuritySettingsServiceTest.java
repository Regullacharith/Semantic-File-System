package com.sfs.ui.mock;

import com.sfs.contracts.security.HandlingPolicy;
import com.sfs.contracts.security.SecuritySettingsView;
import com.sfs.contracts.security.SensitiveTypePolicy;
import com.sfs.contracts.semantic.ProtectedReferenceView.SensitiveType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the intended security defaults.
 */
@DisplayName("Mock security settings service")
class MockSecuritySettingsServiceTest {

    private SecuritySettingsView settings;

    @BeforeEach
    void setUp() {
        settings = new MockSecuritySettingsService().getSettings();
    }

    @Test
    @DisplayName("enables every mandatory protection")
    void enablesMandatoryProtections() {
        assertThat(settings.embeddingsExcludeSecrets()).isTrue();
        assertThat(settings.logsExcludeSecrets()).isTrue();
        assertThat(settings.dnaExcludeSecrets()).isTrue();
        assertThat(settings.authorizationRequired()).isTrue();
    }

    @Test
    @DisplayName("configures a policy for every sensitive type")
    void coversEverySensitiveType() {
        assertThat(settings.typePolicies())
                .extracting(SensitiveTypePolicy::sensitiveType)
                .containsExactlyInAnyOrder(SensitiveType.values());
    }

    @Test
    @DisplayName("fixes passwords to a non-reversible policy")
    void passwordIsNonReversibleAndLocked() {
        SensitiveTypePolicy password = settings.typePolicies().stream()
                .filter(policy -> policy.sensitiveType() == SensitiveType.PASSWORD)
                .findFirst()
                .orElseThrow();

        assertThat(password.handling().isReversible()).isFalse();
        assertThat(password.locked()).isTrue();
    }

    @Test
    @DisplayName("defaults unclassified sensitive data to redaction")
    void unclassifiedDataFailsClosed() {
        SensitiveTypePolicy other = settings.typePolicies().stream()
                .filter(policy -> policy.sensitiveType() == SensitiveType.OTHER)
                .findFirst()
                .orElseThrow();

        assertThat(other.handling()).isEqualTo(HandlingPolicy.REDACT);
        assertThat(other.locked()).isTrue();
    }

    @Test
    @DisplayName("keeps reversible storage the exception rather than the default")
    void reversibleStorageIsTheException() {
        long reversible = settings.typePolicies().stream()
                .filter(policy -> policy.handling().isReversible())
                .count();

        assertThat(reversible)
                .as("most categories should not retain a recoverable exact value")
                .isLessThan(settings.typePolicies().size() / 2);
    }

    @Test
    @DisplayName("explains every policy")
    void everyPolicyHasARationale() {
        assertThat(settings.typePolicies())
                .allSatisfy(policy -> assertThat(policy.rationale()).isNotBlank());
    }

    @Test
    @DisplayName("keeps encryption keys away from the data they protect")
    void keysAreHeldSeparately() {
        assertThat(settings.keyStorageDescription())
                .contains("separately")
                .contains("never stored in the Memory Database");
    }

    @Test
    @DisplayName("records refusals as well as permitted actions")
    void auditRecordsRefusals() {
        assertThat(settings.auditEvents()).isNotEmpty();
        assertThat(settings.auditEvents()).anyMatch(event -> !event.permitted());
    }

    @Test
    @DisplayName("keeps sensitive values out of every audit entry")
    void auditContainsNoSecrets() {
        for (SecuritySettingsView.AuditEventView event : settings.auditEvents()) {
            assertThat(event.detail())
                    .doesNotContain("sk-", "Bearer ", "password=", "AKIA");
        }
    }
}
