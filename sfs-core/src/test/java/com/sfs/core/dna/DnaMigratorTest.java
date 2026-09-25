package com.sfs.core.dna;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("DnaMigrator")
class DnaMigratorTest {

    private final DnaMigrator migrator = new DnaMigrator();

    @Test
    @DisplayName("a legacy 0.1 document migrates to a valid 0.2 DNA")
    void migratesLegacy() {
        SemanticDna migrated = migrator.migrate(DnaFixtures.legacyJson());

        assertThat(migrated.schemaVersion()).isEqualTo("sfs-dna/0.2");
        assertThat(migrated.objectId()).isEqualTo("sfs-obj-0002-e5f6a7b8");
        assertThat(migrated.dnaVersion()).isEqualTo(2);
        assertThat(migrated.summary()).contains("database performance");
        assertThat(migrated.entities().getFirst().name()).isEqualTo("PostgreSQL");
        assertThat(migrated.fidelity().extractionConfidence()).isEqualTo(0.89);
        assertThat(new DnaSchemaValidator().validate(migrated)).isEmpty();
    }

    @Test
    @DisplayName("migrated DNA carries seed rules and a migration behaviour profile")
    void migrationDefaults() {
        SemanticDna migrated = migrator.migrate(DnaFixtures.legacyJson());

        assertThat(migrated.reconstructionRules()).isEqualTo(SemanticDnaBuilder.SEED_RULES);
        assertThat(migrated.behaviour().documentType()).isEqualTo("migrated-document");
        assertThat(migrated.identity().engineVersion())
                .isEqualTo(DnaMigrator.MIGRATION_ENGINE_VERSION);
        assertThat(migrated.embedding().present()).isFalse();
    }

    @Test
    @DisplayName("migrated DNA serializes canonically and round-trips")
    void migratedRoundTrips() {
        SemanticDna migrated = migrator.migrate(DnaFixtures.legacyJson());
        assertThat(DnaCanonical.deserialize(DnaCanonical.serialize(migrated)))
                .isEqualTo(migrated);
    }

    @Test
    @DisplayName("current-version payloads are refused by the migrator")
    void currentVersionRefused() {
        assertThatThrownBy(() -> migrator.migrate(DnaCanonical.serialize(DnaFixtures.sample())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sfs-dna/0.1");
    }

    @Test
    @DisplayName("malformed legacy payloads are refused explicitly")
    void malformedRefused() {
        assertThatThrownBy(() -> migrator.migrate("{not json"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
