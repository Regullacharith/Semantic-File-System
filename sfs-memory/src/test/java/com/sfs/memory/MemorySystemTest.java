package com.sfs.memory;

import com.sfs.core.dna.DnaCanonical;
import com.sfs.core.dna.DnaSchemaValidator;
import com.sfs.core.dna.EmbeddingRef;
import com.sfs.core.dna.Fact;
import com.sfs.core.dna.FidelityProfile;
import com.sfs.core.dna.BehaviourProfile;
import com.sfs.core.dna.StructureNode;
import com.sfs.core.dna.SemanticDna;
import com.sfs.core.dna.StoredDna;
import com.sfs.core.dna.SemanticDnaBuilder;
import com.sfs.core.dna.SecurityProfile;
import com.sfs.core.dna.DnaIdentity;
import com.sfs.core.rules.RuleSetCanonical;
import com.sfs.lifecycle.model.FileVersion;
import com.sfs.core.identity.ObjectId;
import com.sfs.lifecycle.model.LifecycleEvent;
import com.sfs.lifecycle.model.LifecycleEventType;
import com.sfs.lifecycle.model.SemanticFile;
import com.sfs.lifecycle.state.FileState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Memory System")
class MemorySystemTest {

    private static final Instant T0 = Instant.parse("2026-03-15T10:00:00Z");

    @Nested
    @DisplayName("VectorIndex")
    class VectorIndexTest {

        @Test
        @DisplayName("ranks by cosine similarity and limits to k")
        void searchTopK() {
            VectorIndex index = new VectorIndex();
            index.upsert("a", List.of(1.0, 0.0, 0.0));
            index.upsert("b", List.of(0.0, 1.0, 0.0));
            index.upsert("c", List.of(0.9, 0.1, 0.0));

            List<VectorIndex.Candidate> top = index.searchTopK(List.of(1.0, 0.0, 0.0), 2);

            assertThat(top).hasSize(2);
            assertThat(top.get(0).objectId()).isEqualTo("a");
            assertThat(top.get(0).similarity()).isCloseTo(1.0,
                    org.assertj.core.data.Offset.offset(1e-9));
            assertThat(top).extracting(VectorIndex.Candidate::objectId)
                    .containsExactly("a", "c");
        }

        @Test
        @DisplayName("remove and rebuild manage the index deterministically")
        void removeAndRebuild() {
            VectorIndex index = new VectorIndex();
            index.upsert("a", List.of(1.0, 0.0));
            index.upsert("b", List.of(0.0, 1.0));
            index.remove("a");
            assertThat(index.size()).isEqualTo(1);
            assertThat(index.vectorOf("a")).isEmpty();

            index.rebuild(Map.of("x", List.of(0.5, 0.5)));
            assertThat(index.size()).isEqualTo(1);
            assertThat(index.vectorOf("x")).isPresent();
        }

        @Test
        @DisplayName("dimension mismatches and invalid input are refused")
        void validation() {
            VectorIndex index = new VectorIndex();
            index.upsert("a", List.of(1.0, 0.0));
            assertThatThrownBy(() -> index.searchTopK(List.of(1.0, 0.0, 0.0), 3))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> index.searchTopK(List.of(1.0), 0))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("H2 Memory DB")
    class Database {

        @Test
        @DisplayName("DNA round-trips through the relational store with versions and hash chains")
        void dnaPersistence(@TempDir Path dir) {
            try (H2MemoryDatabase db = new H2MemoryDatabase(
                    "jdbc:h2:file:" + dir.resolve("mem").toAbsolutePath() + ";AUTO_SERVER=FALSE")) {
                db.initialize();
                SemanticDna v1 = sampleDna(1);
                StoredDna stored = db.saveSemanticDna(v1, T0);

                assertThat(stored.canonicalSha256()).isEqualTo(DnaCanonical.integrityHash(v1));
                assertThat(stored.previousSha256()).isNull();
                assertThat(db.findStored(v1.objectId()).orElseThrow().dna()).isEqualTo(v1);
                assertThat(db.nextDnaVersion(v1.objectId())).isEqualTo(2);

                SemanticDna v2 = sampleDna(2);
                StoredDna stored2 = db.saveSemanticDna(v2, T0);
                assertThat(stored2.chainsTo(stored)).isTrue();
                assertThat(db.history(v1.objectId())).hasSize(2);
                assertThatThrownBy(() -> db.saveSemanticDna(v1, T0))
                        .isInstanceOf(IllegalArgumentException.class);
                assertThat(db.dnaObjectCount()).isEqualTo(1);
            }
        }

        @Test
        @DisplayName("graph rows, embedding and storage accounting are written")
        void graphAndAccounting(@TempDir Path dir) {
            try (H2MemoryDatabase db = new H2MemoryDatabase(
                    "jdbc:h2:file:" + dir.resolve("mem").toAbsolutePath() + ";AUTO_SERVER=FALSE")) {
                db.initialize();
                db.saveSemanticDna(sampleDna(1), T0);

                Map<String, Object> stats = db.storageStats();
                assertThat((long) stats.get("dnaVersions")).isEqualTo(1L);
                assertThat((long) stats.get("facts")).isEqualTo(1L);
                assertThat((long) stats.get("entities")).isEqualTo(1L);
                assertThat((long) stats.get("structureNodes")).isEqualTo(1L);
                assertThat((long) stats.get("embeddings")).isEqualTo(1L);
                assertThat((long) stats.get("canonicalBytes")).isGreaterThan(0);

                db.storeRaw("sfs-obj-0001-a1b2c3d4", "raw bytes".getBytes());
                assertThat(db.containsRaw("sfs-obj-0001-a1b2c3d4")).isTrue();
                assertThat(new String(db.retrieveRaw("sfs-obj-0001-a1b2c3d4").orElseThrow()))
                        .isEqualTo("raw bytes");
                assertThat(db.releaseRaw("sfs-obj-0001-a1b2c3d4")).isTrue();
                assertThat(db.containsRaw("sfs-obj-0001-a1b2c3d4")).isFalse();
            }
        }

        @Test
        @DisplayName("object states, file versions and lifecycle events persist and reload")
        void lifecyclePersistence(@TempDir Path dir) {
            try (H2MemoryDatabase db = new H2MemoryDatabase(
                    "jdbc:h2:file:" + dir.resolve("mem").toAbsolutePath() + ";AUTO_SERVER=FALSE")) {
                db.initialize();
                SemanticFile file = memorizedFile();
                db.saveObjectState(file);
                db.saveLifecycleEvent(registrationEvent());

                List<SemanticFile> files = db.loadObjectStates();
                assertThat(files).hasSize(1);
                SemanticFile restored = files.getFirst();
                assertThat(restored.state()).isEqualTo(FileState.MEMORIZED);
                assertThat(restored.metadata().fileName()).isEqualTo("notes.txt");
                assertThat(restored.versions()).hasSize(1);
                assertThat(restored.certifiedDnaVersion()).isEqualTo("sfs-dna/0.2 v1");

                List<LifecycleEvent> events = db.loadLifecycleEvents();
                assertThat(events).hasSize(1);
                assertThat(events.getFirst().type()).isEqualTo(LifecycleEventType.REGISTRATION_RECORDED);
            }
        }

        @Test
        @DisplayName("rule sets persist and reload canonically")
        void rulePersistence(@TempDir Path dir) {
            try (H2MemoryDatabase db = new H2MemoryDatabase(
                    "jdbc:h2:file:" + dir.resolve("mem").toAbsolutePath() + ";AUTO_SERVER=FALSE")) {
                db.initialize();
                com.sfs.core.rules.RuleSet set = new com.sfs.core.rules.RuleDeriver()
                        .derive(sampleDna(1));
                db.saveRuleSet(set.objectId(), set.dnaVersion(),
                        RuleSetCanonical.serialize(set),
                        RuleSetCanonical.integrityHash(set), T0);

                assertThat(db.loadRuleSets()).containsExactly(set);
            }
        }
    }

    @Test
    @DisplayName("Restart acceptance: the deleted (memorized) Semantic Record survives")
    void deletedRecordSurvivesRestart(@TempDir Path dir) {
        String url = "jdbc:h2:file:" + dir.resolve("restart").toAbsolutePath()
                + ";AUTO_SERVER=FALSE";
        String objectId = "sfs-obj-0001-a1b2c3d4";

        try (H2MemoryDatabase db = new H2MemoryDatabase(url)) {
            db.initialize();
            db.saveSemanticDna(sampleDna(1), T0);
            db.saveObjectState(memorizedFile());
            db.storeRaw(objectId, "stale bytes".getBytes());
            db.releaseRaw(objectId);
        }

        try (H2MemoryDatabase reopened = new H2MemoryDatabase(url)) {
            reopened.initialize();
            assertThat(reopened.findStored(objectId)).isPresent();
            assertThat(reopened.findStored(objectId).orElseThrow().dna().summary())
                    .isEqualTo("Overview of the platform for Q3 2026.");
            assertThat(reopened.nextDnaVersion(objectId)).isEqualTo(2);
            assertThat(reopened.containsRaw(objectId)).isFalse();

            var loaded = reopened.loadObjectStates();
            assertThat(loaded).hasSize(1);
            assertThat(loaded.getFirst().state()).isEqualTo(FileState.MEMORIZED);

            MemoryDnaRepository repository = new MemoryDnaRepository(
                    reopened, new VectorIndex());
            repository.rebuildVectorIndex();
            assertThat(reopened.storageStats().get("dnaVersions")).isEqualTo(1L);
        }
    }

    private SemanticDna sampleDna(int version) {
        return SemanticDnaBuilder
                .forIdentity(new DnaIdentity("sfs-obj-0001-a1b2c3d4",
                        DnaSchemaValidator.CURRENT_SCHEMA_VERSION, version,
                        "sfs-engine/0.1", T0))
                .summary("Overview of the platform for Q3 2026.")
                .concepts(List.of("platform review"))
                .topics(List.of("platform"))
                .entities(List.of(new com.sfs.core.dna.Entity("PostgreSQL", "Named entity", 2)))
                .facts(List.of(new Fact("Latency decreased by 40 percent.", true, 0.95)))
                .structure(List.of(new StructureNode("Summary", 1, 0)))
                .embedding(new EmbeddingRef("feature-hashing/0.1", 4,
                        List.of(0.5, 0.5, 0.0, 0.7)))
                .fidelity(new FidelityProfile(0.9, 0.8, "sfs-engine/0.1"))
                .build();
    }

    private SemanticFile memorizedFile() {
        Instant t = T0;
        return new SemanticFile(
                ObjectId.of("sfs-obj-0001-a1b2c3d4"),
                new com.sfs.lifecycle.model.FileMetadata("notes.txt", "text/plain", 7,
                        com.sfs.core.identity.Digests.sha256Hex("content"), null, t, t),
                FileState.MEMORIZED,
                null,
                "sfs-dna/0.2 v1",
                List.of(new FileVersion(1,
                        com.sfs.core.identity.Digests.sha256Hex("content"), 7, t)),
                t);
    }

    private LifecycleEvent registrationEvent() {
        return LifecycleEvent.transition("sfs-lfe-000001", "sfs-obj-0001-a1b2c3d4",
                LifecycleEventType.REGISTRATION_RECORDED, null, FileState.REGISTERED,
                "system", T0);
    }
}
