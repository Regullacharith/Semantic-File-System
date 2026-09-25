package com.sfs.engine.record;

import com.sfs.contracts.semantic.ProtectedReferenceView;
import com.sfs.contracts.semantic.SemanticDnaView;
import com.sfs.core.dna.DnaCanonical;
import com.sfs.core.dna.DnaSchemaValidator;
import com.sfs.core.dna.SemanticDna;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InMemorySemanticRecordStore")
class InMemorySemanticRecordStoreTest {

    private static final java.time.Instant T0 =
            java.time.Instant.parse("2026-03-15T10:00:00Z");

    private final InMemorySemanticRecordStore store = new InMemorySemanticRecordStore();

    private SemanticDna dna(String objectId, int version) {
        return new SemanticDna(
                new com.sfs.core.dna.DnaIdentity(objectId,
                        DnaSchemaValidator.CURRENT_SCHEMA_VERSION, version,
                        "sfs-engine/0.1", T0),
                "summary of the document",
                List.of(new com.sfs.core.dna.Concept("concept")),
                List.of(new com.sfs.core.dna.Topic("topic")),
                List.of(new com.sfs.core.dna.Entity("Entity", "Named entity", 1)),
                List.of(new com.sfs.core.dna.Fact("A statement.", false, 0.8)),
                List.of(),
                List.of(new com.sfs.core.dna.StructureNode("Body", 1, 0)),
                com.sfs.core.dna.EmbeddingRef.absent(),
                new com.sfs.core.dna.BehaviourProfile("narrative", false, 8,
                        List.of("preserve heading order")),
                com.sfs.core.dna.SemanticDnaBuilder.SEED_RULES,
                new com.sfs.core.dna.FidelityProfile(0.8, 0.8, "sfs-engine/0.1"),
                com.sfs.core.dna.SecurityProfile.unrestricted());
    }

    @Test
    @DisplayName("finds nothing for unknown or blank identifiers")
    void unknownAndBlank() {
        assertThat(store.findSemanticDna("sfs-obj-9999-00000000")).isEmpty();
        assertThat(store.findSemanticDna(" ")).isEmpty();
        assertThat(store.findStored("sfs-obj-9999-00000000")).isEmpty();
    }

    @Test
    @DisplayName("stores authoritative DNA and serves its view")
    void storesAuthoritativeAndServesView() {
        SemanticDna dna = dna("sfs-obj-0001-a1b2c3d4", 1);
        store.save(dna);

        Optional<SemanticDnaView> view = store.findSemanticDna(dna.objectId());
        assertThat(view).isPresent();
        assertThat(view.orElseThrow().objectId()).isEqualTo(dna.objectId());
        assertThat(view.orElseThrow().summary()).isEqualTo("summary of the document");
        assertThat(view.orElseThrow().schemaVersion()).isEqualTo("sfs-dna/0.2");
        assertThat(store.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("stored records keep the canonical integrity hash of their DNA")
    void integrityHashPreserved() {
        SemanticDna dna = dna("sfs-obj-0001-a1b2c3d4", 1);
        var stored = store.save(dna);

        assertThat(stored.canonicalSha256()).isEqualTo(DnaCanonical.integrityHash(dna));
        assertThat(store.findStored(dna.objectId()).orElseThrow()).isEqualTo(stored);
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
    @DisplayName("history exposes the ordered version chain")
    void history() {
        store.save(dna("sfs-obj-0001-a1b2c3d4", 1));
        store.save(dna("sfs-obj-0001-a1b2c3d4", 2));

        var history = store.history("sfs-obj-0001-a1b2c3d4");
        assertThat(history).hasSize(2);
        assertThat(history.get(0).chainsTo(history.get(1))).isFalse();
        assertThat(history.get(1).chainsTo(history.get(0))).isTrue();
        assertThat(store.findSemanticDna("sfs-obj-0001-a1b2c3d4")
                .orElseThrow().dnaVersion()).isEqualTo(2);
    }

    @Test
    @DisplayName("protected references are served as views without values")
    void protectedReferenceViews() {
        SemanticDna guarded = new SemanticDna(
                new com.sfs.core.dna.DnaIdentity("sfs-obj-0004-b3c4d5e6",
                        DnaSchemaValidator.CURRENT_SCHEMA_VERSION, 1,
                        "sfs-engine/0.1", T0),
                "guarded summary",
                List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(new com.sfs.core.dna.StructureNode("Body", 1, 0)),
                com.sfs.core.dna.EmbeddingRef.absent(),
                new com.sfs.core.dna.BehaviourProfile("narrative", false, 8, List.of()),
                com.sfs.core.dna.SemanticDnaBuilder.SEED_RULES,
                new com.sfs.core.dna.FidelityProfile(0.8, 0.8, "sfs-engine/0.1"),
                new com.sfs.core.dna.SecurityProfile("protected-refs-v1",
                        List.of(new com.sfs.core.dna.ProtectedReference(
                                "sfs-ref-abc123", "PASSWORD",
                                "credential assignment", "line 8"))));
        store.save(guarded);

        var view = store.findSemanticDna("sfs-obj-0004-b3c4d5e6").orElseThrow();
        assertThat(view.protectedReferences()).hasSize(1);
        assertThat(view.protectedReferences().getFirst().sensitiveType())
                .isEqualTo(ProtectedReferenceView.SensitiveType.PASSWORD);
        assertThat(view.hasProtectedReferences()).isTrue();
    }
}
