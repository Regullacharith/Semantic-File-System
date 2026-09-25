package com.sfs.core.dna;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("InMemoryDnaRepository")
class InMemoryDnaRepositoryTest {

    private static final Instant T0 = Instant.parse("2026-03-15T10:00:00Z");
    private static final Instant T1 = Instant.parse("2026-03-15T10:01:00Z");

    private final InMemoryDnaRepository repository = new InMemoryDnaRepository();

    private SemanticDna withVersion(SemanticDna dna, int version) {
        return new SemanticDna(
                new DnaIdentity(dna.objectId(), dna.schemaVersion(), version,
                        dna.identity().engineVersion(), dna.identity().generatedAt()),
                dna.summary(), dna.concepts(), dna.topics(), dna.entities(),
                dna.facts(), dna.relationships(), dna.structure(), dna.embedding(),
                dna.behaviour(), dna.reconstructionRules(), dna.fidelity(),
                dna.security());
    }

    @Test
    @DisplayName("saving stores the canonical integrity hash")
    void storesIntegrityHash() {
        SemanticDna dna = DnaFixtures.sample();
        StoredDna stored = repository.save(dna, T0);

        assertThat(stored.canonicalSha256()).isEqualTo(DnaCanonical.integrityHash(dna));
        assertThat(stored.previousSha256()).isNull();
        assertThat(repository.find(dna.objectId())).contains(stored);
    }

    @Test
    @DisplayName("versions chain through previous hashes and increase monotonically")
    void versionChain() {
        SemanticDna v1 = DnaFixtures.sample();
        SemanticDna v2 = withVersion(v1, 2);
        StoredDna first = repository.save(v1, T0);
        StoredDna second = repository.save(v2, T1);

        assertThat(second.chainsTo(first)).isTrue();
        assertThat(repository.history(v1.objectId())).containsExactly(first, second);
        assertThat(repository.nextDnaVersion(v1.objectId())).isEqualTo(3);
    }

    @Test
    @DisplayName("a non-increasing version is refused")
    void nonIncreasingVersionRefused() {
        SemanticDna v1 = DnaFixtures.sample();
        repository.save(v1, T0);
        assertThatThrownBy(() -> repository.save(v1, T1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must increase");
    }

    @Test
    @DisplayName("unknown objects have no history and version one next")
    void unknownObject() {
        assertThat(repository.find("sfs-obj-9999-00000000")).isEmpty();
        assertThat(repository.history("sfs-obj-9999-00000000")).isEmpty();
        assertThat(repository.nextDnaVersion("sfs-obj-9999-00000000")).isEqualTo(1);
        assertThat(repository.remove("sfs-obj-9999-00000000")).isFalse();
    }

    @Test
    @DisplayName("different objects hash independently")
    void independentObjects() {
        SemanticDna a = DnaFixtures.sample();
        SemanticDna b = new SemanticDna(
                new DnaIdentity("sfs-obj-0002-e5f6a7b8", DnaSchemaValidator.CURRENT_SCHEMA_VERSION,
                        1, "sfs-engine/0.1", DnaFixtures.T0),
                "Another summary.", List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(new StructureNode("Body", 1, 0)), EmbeddingRef.absent(),
                DnaFixtures.sample().behaviour(), SemanticDnaBuilder.SEED_RULES,
                DnaFixtures.sample().fidelity(), SecurityProfile.unrestricted());
        StoredDna storedA = repository.save(a, T0);
        StoredDna storedB = repository.save(b, T0);
        assertThat(storedB.previousSha256()).isNull();
        assertThat(storedA.canonicalSha256())
                .isNotEqualTo(storedB.canonicalSha256());
        assertThat(repository.objectCount()).isEqualTo(2);
    }
}
