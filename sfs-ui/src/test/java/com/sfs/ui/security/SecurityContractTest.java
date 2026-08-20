package com.sfs.ui.security;

import com.sfs.contracts.security.HandlingPolicy;
import com.sfs.contracts.security.SecuritySettingsView;
import com.sfs.contracts.security.SecuritySettingsView.AuditEventView;
import com.sfs.contracts.security.SensitiveTypePolicy;
import com.sfs.contracts.semantic.ProtectedReferenceView.SensitiveType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.lang.reflect.RecordComponent;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies the security contract invariants.
 */
@DisplayName("Security contracts")
class SecurityContractTest {

    private static SensitiveTypePolicy policy(SensitiveType type, HandlingPolicy handling) {
        return new SensitiveTypePolicy(type, handling, false, "Test rationale.");
    }

    private static SecuritySettingsView settings(boolean embeddings, boolean logs,
                                                 boolean dna, boolean authorization) {
        return new SecuritySettingsView(
                List.of(policy(SensitiveType.EMAIL_ADDRESS, HandlingPolicy.TOKENIZE)),
                embeddings, logs, dna, authorization,
                "Keys are held separately from ciphertext.",
                List.of());
    }

    @Nested
    @DisplayName("password policy")
    class PasswordPolicy {

        @Test
        @DisplayName("cannot be constructed with a reversible policy")
        void passwordCannotBeReversible() {
            assertThatThrownBy(() -> policy(SensitiveType.PASSWORD, HandlingPolicy.ENCRYPT))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("non-reversible");
        }

        @Test
        @DisplayName("accepts non-reversible handling")
        void passwordAcceptsNonReversible() {
            assertThatCode(() -> policy(SensitiveType.PASSWORD, HandlingPolicy.REDACT))
                    .doesNotThrowAnyException();
            assertThatCode(() -> policy(SensitiveType.PASSWORD, HandlingPolicy.TOKENIZE))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("is reported as never eligible for reversible handling")
        void passwordIsNeverReversible() {
            assertThat(SensitiveTypePolicy.mayBeReversible(SensitiveType.PASSWORD)).isFalse();
            assertThat(SensitiveTypePolicy.mayBeReversible(SensitiveType.API_KEY)).isTrue();
        }

        @Test
        @DisplayName("offers only non-reversible options for a password")
        void passwordOffersOnlySafeOptions() {
            assertThat(policy(SensitiveType.PASSWORD, HandlingPolicy.REDACT).permittedOptions())
                    .containsExactlyInAnyOrder(HandlingPolicy.REDACT, HandlingPolicy.TOKENIZE)
                    .doesNotContain(HandlingPolicy.ENCRYPT);
        }
    }

    @Nested
    @DisplayName("handling policies")
    class Handling {

        @Test
        @DisplayName("offers no plaintext retention option")
        void noPlaintextOption() {
            List<String> names = java.util.Arrays.stream(HandlingPolicy.values())
                    .map(Enum::name)
                    .map(name -> name.toLowerCase(Locale.ROOT))
                    .toList();

            assertThat(names).doesNotContain("plaintext", "store_plain", "none", "keep");
        }

        @Test
        @DisplayName("treats only encryption as reversible")
        void onlyEncryptionIsReversible() {
            assertThat(HandlingPolicy.REDACT.isReversible()).isFalse();
            assertThat(HandlingPolicy.TOKENIZE.isReversible()).isFalse();
            assertThat(HandlingPolicy.ENCRYPT.isReversible()).isTrue();
        }

        @Test
        @DisplayName("lists the most protective option first so a fallback fails closed")
        void mostProtectiveOptionIsFirst() {
            assertThat(HandlingPolicy.values()[0]).isEqualTo(HandlingPolicy.REDACT);
        }

        @ParameterizedTest
        @EnumSource(HandlingPolicy.class)
        @DisplayName("declares a label and description")
        void declaresMetadata(HandlingPolicy handling) {
            assertThat(handling.getLabel()).isNotBlank();
            assertThat(handling.getDescription()).isNotBlank();
        }
    }

    @Nested
    @DisplayName("mandatory protections")
    class MandatoryProtections {

        @Test
        @DisplayName("rejects settings that would allow secrets into embeddings")
        void rejectsSecretsInEmbeddings() {
            assertThatThrownBy(() -> settings(false, true, true, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("embeddings");
        }

        @Test
        @DisplayName("rejects settings that would allow secrets into logs")
        void rejectsSecretsInLogs() {
            assertThatThrownBy(() -> settings(true, false, true, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("logs");
        }

        @Test
        @DisplayName("rejects settings that would allow secrets into Semantic DNA")
        void rejectsSecretsInDna() {
            assertThatThrownBy(() -> settings(true, true, false, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Semantic DNA");
        }

        @Test
        @DisplayName("rejects settings that would allow unauthorized reference resolution")
        void rejectsUnauthorizedResolution() {
            assertThatThrownBy(() -> settings(true, true, true, false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("authorization");
        }

        @Test
        @DisplayName("requires at least one configured policy")
        void requiresAtLeastOnePolicy() {
            assertThatThrownBy(() -> new SecuritySettingsView(
                    List.of(), true, true, true, true, "Keys held separately.", List.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("audit events")
    class Audit {

        @Test
        @DisplayName("carries no field capable of holding a sensitive value")
        void carriesNoValueField() {
            List<String> names = java.util.Arrays.stream(
                            AuditEventView.class.getRecordComponents())
                    .map(RecordComponent::getName)
                    .map(name -> name.toLowerCase(Locale.ROOT))
                    .toList();

            assertThat(names).doesNotContain("value", "plaintext", "secret", "payload");
        }

        @Test
        @DisplayName("rejects a blank field")
        void rejectsBlankFields() {
            assertThatThrownBy(() -> new AuditEventView("  ", "type", "detail", true))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("settings view")
    class Settings {

        @Test
        @DisplayName("reports whether every policy is non-reversible")
        void reportsAllNonReversible() {
            SecuritySettingsView allSafe = new SecuritySettingsView(
                    List.of(policy(SensitiveType.EMAIL_ADDRESS, HandlingPolicy.TOKENIZE)),
                    true, true, true, true, "Keys held separately.", List.of());

            SecuritySettingsView withReversible = new SecuritySettingsView(
                    List.of(policy(SensitiveType.API_KEY, HandlingPolicy.ENCRYPT)),
                    true, true, true, true, "Keys held separately.", List.of());

            assertThat(allSafe.allPoliciesNonReversible()).isTrue();
            assertThat(withReversible.allPoliciesNonReversible()).isFalse();
        }

        @Test
        @DisplayName("defensively copies its collections")
        void copiesCollections() {
            List<SensitiveTypePolicy> mutable = new java.util.ArrayList<>();
            mutable.add(policy(SensitiveType.EMAIL_ADDRESS, HandlingPolicy.TOKENIZE));

            SecuritySettingsView view = new SecuritySettingsView(
                    mutable, true, true, true, true, "Keys held separately.", List.of());
            mutable.add(policy(SensitiveType.PHONE_NUMBER, HandlingPolicy.REDACT));

            assertThat(view.typePolicies()).hasSize(1);
        }
    }
}
