package com.sfs.app.security;

import com.sfs.contracts.file.DeletionConfirmation;
import com.sfs.contracts.security.Capability;
import com.sfs.contracts.security.Principal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.lang.reflect.RecordComponent;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Security contracts")
class SecurityContractTest {

    private static final Set<String> CREDENTIAL_NAMES = Set.of(
            "password", "secret", "token", "credential", "apikey", "key",
            "passphrase", "hash", "salt", "session");

    @Nested
    @DisplayName("principal")
    class PrincipalContract {

        @Test
        @DisplayName("cannot carry a credential of any kind")
        void carriesNoCredential() {
            for (RecordComponent component : Principal.class.getRecordComponents()) {
                String name = component.getName().toLowerCase(Locale.ROOT);

                assertThat(CREDENTIAL_NAMES)
                        .as("Principal must not expose a credential-shaped component '%s'", name)
                        .doesNotContain(name);
            }
        }

        @Test
        @DisplayName("rejects a blank identity")
        void rejectsBlankIdentity() {
            assertThatThrownBy(() -> new Principal("  ", "Name", Set.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("cannot have its capabilities modified after construction")
        void capabilitiesAreImmutable() {
            Set<Capability> mutable = new HashSet<>();
            mutable.add(Capability.READ);

            Principal principal = new Principal("id", "Name", mutable);
            mutable.add(Capability.PURGE_RAW);

            assertThat(principal.has(Capability.PURGE_RAW))
                    .as("a caller must not be able to grant itself a capability after the fact")
                    .isFalse();
        }

        @Test
        @DisplayName("grants nothing it was not given")
        void grantsNothingImplicitly() {
            Principal reader = new Principal("id", "Reader", Set.of(Capability.READ));

            assertThat(reader.has(Capability.READ)).isTrue();
            assertThat(reader.has(Capability.DELETE_RAW)).isFalse();
            assertThat(reader.has(Capability.PURGE_RAW)).isFalse();
        }
    }

    @Nested
    @DisplayName("capability")
    class CapabilityContract {

        @Test
        @DisplayName("keeps destructive capabilities separate")
        void destructiveCapabilitiesAreSeparate() {
            assertThat(Capability.DELETE_RAW).isNotEqualTo(Capability.PURGE_RAW);
            assertThat(Capability.UNDO_DELETE).isNotEqualTo(Capability.DELETE_RAW);
        }

        @Test
        @DisplayName("marks only purge as irreversible")
        void onlyPurgeIsIrreversible() {
            assertThat(Capability.PURGE_RAW.isIrreversible()).isTrue();
            assertThat(Capability.DELETE_RAW.isIrreversible()).isFalse();
            assertThat(Capability.UNDO_DELETE.isIrreversible()).isFalse();
        }

        @ParameterizedTest
        @EnumSource(Capability.class)
        @DisplayName("labels every capability")
        void labelsEveryCapability(Capability capability) {
            assertThat(capability.getLabel()).isNotBlank();
        }
    }

    @Nested
    @DisplayName("deletion confirmation")
    class Confirmation {

        @Test
        @DisplayName("confirms only the object it names")
        void confirmsOnlyNamedObject() {
            DeletionConfirmation confirmation =
                    new DeletionConfirmation("sfs-obj-0001-a1b2c3d4");

            assertThat(confirmation.confirms("sfs-obj-0001-a1b2c3d4")).isTrue();
            assertThat(confirmation.confirms("sfs-obj-0002-e5f6a7b8")).isFalse();
        }

        @Test
        @DisplayName("cannot be constructed blank")
        void cannotBeBlank() {
            assertThatThrownBy(() -> new DeletionConfirmation("  "))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("is not satisfied by a generic affirmative")
        void notSatisfiedByGenericAffirmative() {
            DeletionConfirmation confirmation = new DeletionConfirmation("true");

            assertThat(confirmation.confirms("sfs-obj-0001-a1b2c3d4")).isFalse();
        }
    }
}
