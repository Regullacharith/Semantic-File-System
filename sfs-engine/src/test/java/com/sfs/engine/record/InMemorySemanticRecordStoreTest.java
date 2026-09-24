package com.sfs.engine.record;

import com.sfs.contracts.semantic.SemanticDnaView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InMemorySemanticRecordStore")
class InMemorySemanticRecordStoreTest {

    private final InMemorySemanticRecordStore store = new InMemorySemanticRecordStore();

    private SemanticDnaView dna(String objectId, int version) {
        return new SemanticDnaView(objectId, "sfs-dna/0.1", version, "summary",
                List.of("concept"), List.of("topic"),
                List.of(new SemanticDnaView.EntityView("Entity", "Named entity", 1)),
                List.of(new SemanticDnaView.FactView("A statement.", false, 0.8)),
                List.of(), List.of(), List.of(), 64,
                new SemanticDnaView.FidelityProfileView(0.8, 0.8, "sfs-engine/0.1"));
    }

    @Test
    @DisplayName("finds nothing for unknown or blank identifiers")
    void unknownAndBlank() {
        assertThat(store.findSemanticDna("sfs-obj-9999-00000000")).isEmpty();
        assertThat(store.findSemanticDna(" ")).isEmpty();
    }

    @Test
    @DisplayName("saves and retrieves a record by Object ID")
    void saveAndFind() {
        store.save(dna("sfs-obj-0001-a1b2c3d4", 1));
        assertThat(store.findSemanticDna("sfs-obj-0001-a1b2c3d4")).isPresent();
        assertThat(store.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("nextDnaVersion increments per object and starts at one")
    void versioning() {
        assertThat(store.nextDnaVersion("sfs-obj-0001-a1b2c3d4")).isEqualTo(1);
        store.save(dna("sfs-obj-0001-a1b2c3d4", 1));
        assertThat(store.nextDnaVersion("sfs-obj-0001-a1b2c3d4")).isEqualTo(2);
        assertThat(store.nextDnaVersion("sfs-obj-0002-e5f6a7b8")).isEqualTo(1);
    }

    @Test
    @DisplayName("saving again for the same object replaces its record")
    void replacement() {
        store.save(dna("sfs-obj-0001-a1b2c3d4", 1));
        store.save(dna("sfs-obj-0001-a1b2c3d4", 2));
        assertThat(store.findSemanticDna("sfs-obj-0001-a1b2c3d4").orElseThrow().dnaVersion())
                .isEqualTo(2);
        assertThat(store.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("removal reports whether a record existed")
    void removal() {
        store.save(dna("sfs-obj-0001-a1b2c3d4", 1));
        assertThat(store.remove("sfs-obj-0001-a1b2c3d4")).isTrue();
        assertThat(store.remove("sfs-obj-0001-a1b2c3d4")).isFalse();
    }
}
