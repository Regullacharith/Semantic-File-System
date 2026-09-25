package com.sfs.core.dna;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Semantic DNA model")
class SemanticDnaModelTest {

    @Test
    @DisplayName("a complete DNA carries identity, sections, profiles and seed rules")
    void completeDna() {
        SemanticDna dna = DnaFixtures.sample();

        assertThat(dna.objectId()).isEqualTo("sfs-obj-0001-a1b2c3d4");
        assertThat(dna.schemaVersion()).isEqualTo("sfs-dna/0.2");
        assertThat(dna.dnaVersion()).isEqualTo(1);
        assertThat(dna.behaviour().documentType()).isEqualTo("headed-note");
        assertThat(dna.reconstructionRules()).hasSize(4);
        assertThat(dna.security().handlingPolicy()).isEqualTo("unrestricted-v1");
        assertThat(dna.embedding().present()).isTrue();
    }

    @Test
    @DisplayName("required fields are enforced at construction")
    void requiredFieldsEnforced() {
        assertThatThrownBy(() -> DnaFixtures.builder().summary(" ").build())
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DnaFixtures.builder()
                .fidelity(null).build())
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new DnaIdentity("sfs-obj-0001-a1b2c3d4", "sfs-dna/0.2",
                0, "sfs-engine/0.1", DnaFixtures.T0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Fact("statement", true, 1.5))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EmbeddingRef("feature-hashing/0.1", 3,
                List.of(1.0, 2.0)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("metadata separation: SemanticDna carries no file-metadata fields")
    void metadataIsSeparated() {
        Set<String> componentNames = componentNamesOf(SemanticDna.class);

        assertThat(componentNames)
                .contains("identity", "summary", "concepts", "topics", "entities",
                        "facts", "relationships", "structure", "embedding",
                        "behaviour", "reconstructionRules", "fidelity", "security")
                .doesNotContain("fileName", "displayName", "contentType", "sizeBytes",
                        "registeredAt", "metadata", "storageAddress", "path");

        MetadataRef ref = new MetadataRef("sfs-obj-0001-a1b2c3d4", "notes.txt",
                "text/plain", 128, DnaFixtures.T0);
        assertThat(ref.displayName()).isEqualTo("notes.txt");
        assertThat(DnaFixtures.sample()).isNotEqualTo(ref);
    }

    @Test
    @DisplayName("relationships are typed and directional records")
    void relationshipsTypedAndDirectional() {
        Relationship forward = new Relationship("A", "hosts", "B");
        Relationship reverse = new Relationship("B", "hosts", "A");
        assertThat(forward).isNotEqualTo(reverse);
        assertThatThrownBy(() -> new Relationship("A", " ", "B"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("facts and entities are explicit first-class records")
    void factsAndEntitiesExplicit() {
        SemanticDna dna = DnaFixtures.sample();
        assertThat(dna.facts()).isNotEmpty();
        assertThat(dna.entities()).isNotEmpty();
        assertThat(dna.embedding().dimensions()).isEqualTo(4);
    }

    private Set<String> componentNamesOf(Class<?> recordClass) {
        var names = new java.util.HashSet<String>();
        for (RecordComponent component : recordClass.getRecordComponents()) {
            names.add(component.getName());
        }
        return java.util.Collections.unmodifiableSet(names);
    }
}
