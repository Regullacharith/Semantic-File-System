package com.sfs.core.dna;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SemanticDnaBuilder")
class SemanticDnaBuilderTest {

    @Test
    @DisplayName("derives a structured behaviour profile from a multi-heading outline")
    void structuredBehaviour() {
        SemanticDna dna = DnaFixtures.builder()
                .structure(List.of(
                        new StructureNode("Summary", 1, 0),
                        new StructureNode("Measurements", 1, 1)))
                .build();

        assertThat(dna.behaviour().documentType()).isEqualTo("structured-document");
        assertThat(dna.behaviour().structured()).isTrue();
        assertThat(dna.behaviour().guidance()).isNotEmpty();
    }

    @Test
    @DisplayName("a body-only document is a narrative")
    void narrativeBehaviour() {
        SemanticDna dna = DnaFixtures.builder()
                .structure(List.of(new StructureNode("Body", 1, 0)))
                .build();
        assertThat(dna.behaviour().documentType()).isEqualTo("narrative");
    }

    @Test
    @DisplayName("protected references switch the security profile and keep no values")
    void securityProfileSwitch() {
        SemanticDna plain = DnaFixtures.builder().build();
        assertThat(plain.security().handlingPolicy()).isEqualTo("unrestricted-v1");
        assertThat(plain.security().containsProtectedReferences()).isFalse();

        SemanticDna guarded = DnaFixtures.builder()
                .protectedReferences(List.of(new ProtectedReference(
                        "sfs-ref-abc123", "PASSWORD", "credential assignment", "line 8")))
                .build();
        assertThat(guarded.security().handlingPolicy()).isEqualTo("protected-refs-v1");
        assertThat(guarded.security().protectedReferences()).hasSize(1);
        assertThat(guarded.security().protectedReferences().getFirst().referenceId())
                .isEqualTo("sfs-ref-abc123");
    }

    @Test
    @DisplayName("every built DNA carries the deterministic seed rules")
    void seedRulesPresent() {
        assertThat(DnaFixtures.sample().reconstructionRules())
                .isEqualTo(SemanticDnaBuilder.SEED_RULES);
        assertThat(SemanticDnaBuilder.SEED_RULES).hasSize(4);
    }
}
