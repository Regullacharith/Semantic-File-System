package com.sfs.core.dna;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Canonical DNA serialization")
class DnaCanonicalTest {

    @Nested
    @DisplayName("lossless round trip")
    class RoundTrip {

        @Test
        @DisplayName("serialize then deserialize yields an equal DNA")
        void lossless() {
            SemanticDna dna = DnaFixtures.sample();
            assertThat(DnaCanonical.deserialize(DnaCanonical.serialize(dna))).isEqualTo(dna);
        }

        @Test
        @DisplayName("adversarial strings survive the round trip unchanged")
        void adversarialStrings() {
            SemanticDna dna = DnaFixtures.builder()
                    .summary(DnaFixtures.escapeProbe())
                    .facts(List.of(new Fact(DnaFixtures.escapeProbe(), false, 0.5)))
                    .build();
            assertThat(DnaCanonical.deserialize(DnaCanonical.serialize(dna))).isEqualTo(dna);
        }

        @Test
        @DisplayName("protected references and profiles survive the round trip")
        void profilesSurvive() {
            SemanticDna dna = DnaFixtures.builder()
                    .protectedReferences(List.of(new ProtectedReference(
                            "sfs-ref-abc123", "PASSWORD", "credential assignment",
                            "line 8")))
                    .build();
            SemanticDna restored = DnaCanonical.deserialize(DnaCanonical.serialize(dna));
            assertThat(restored).isEqualTo(dna);
            assertThat(restored.security().protectedReferences().getFirst().sensitiveType())
                    .isEqualTo("PASSWORD");
        }
    }

    private static SemanticDna withVersion(SemanticDna dna, int version) {
        return new SemanticDna(
                new DnaIdentity(dna.objectId(), dna.schemaVersion(), version,
                        dna.identity().engineVersion(), dna.identity().generatedAt()),
                dna.summary(), dna.concepts(), dna.topics(), dna.entities(),
                dna.facts(), dna.relationships(), dna.structure(), dna.embedding(),
                dna.behaviour(), dna.reconstructionRules(), dna.fidelity(),
                dna.security());
    }

    @Nested
    @DisplayName("canonical form")
    class CanonicalForm {

        @Test
        @DisplayName("serialization is deterministic and free of insignificant whitespace")
        void deterministic() {
            String first = DnaCanonical.serialize(DnaFixtures.sample());
            String second = DnaCanonical.serialize(DnaFixtures.sample());
            assertThat(first).isEqualTo(second);
            assertThat(first).doesNotContain(", ").doesNotContain(": ");
            assertThat(first).startsWith("{\"schemaVersion\"");
        }

        @Test
        @DisplayName("different versions of the same content are detectable")
        void versionChangesDetectable() {
            SemanticDna v1 = DnaFixtures.sample();
            SemanticDna v2 = withVersion(DnaFixtures.sample(), 2);
            String hash1 = DnaCanonical.integrityHash(v1);
            String hash2 = DnaCanonical.integrityHash(v2);
            assertThat(hash1).isNotEqualTo(hash2);
            assertThat(DnaCanonical.serialize(v1)).contains("\"dnaVersion\":1");
            assertThat(DnaCanonical.serialize(v2)).contains("\"dnaVersion\":2");
        }

        @Test
        @DisplayName("content changes change the integrity hash")
        void contentChangesDetectable() {
            String hashA = DnaCanonical.integrityHash(DnaFixtures.sample());
            String hashB = DnaCanonical.integrityHash(
                    DnaFixtures.builder().summary("Different summary entirely.").build());
            assertThat(hashA).isNotEqualTo(hashB);
        }

        @Test
        @DisplayName("parsing a future schema version is refused with guidance")
        void futureVersionRefused() {
            String future = DnaCanonical.serialize(DnaFixtures.sample())
                    .replace("\"sfs-dna/0.2\"", "\"sfs-dna/9.9\"");
            assertThatThrownBy(() -> DnaCanonical.deserialize(future))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("migrator");
        }
    }
}
