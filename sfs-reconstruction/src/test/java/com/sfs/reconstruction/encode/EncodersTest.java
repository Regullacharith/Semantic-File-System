package com.sfs.reconstruction.encode;

import com.sfs.core.dna.SemanticDna;
import com.sfs.core.rules.ReconstructionPlan;
import com.sfs.reconstruction.Fixtures;
import com.sfs.reconstruction.ModelInput;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Encoders: DNA, structure, facts/entities and relationships (10.3-10.6)")
class EncodersTest {

    @Test
    @DisplayName("the structure encoder emits sections in recorded order regardless of list order")
    void structureSortedByOrder() {
        SemanticDna dna = Fixtures.shuffledDna();
        ReconstructionPlan plan = new ReconstructionPlan(
                dna.objectId(), dna.dnaVersion(), "0f9a2c4b", "sfs-rules/test",
                List.of("Overview", "Timeline", "Risks"),
                List.of(), List.of(), List.of(),
                new ReconstructionPlan.ContentContract(true, dna.summary(), 0, 0),
                new ReconstructionPlan.ValidationContract(true, true, 0.7),
                List.of());

        List<EncodedSection> sections =
                new StructureEncoder().encode(ModelInput.of(dna, plan));

        assertThat(sections).extracting(EncodedSection::heading)
                .containsExactly("Overview", "Timeline", "Risks");
        assertThat(sections).allSatisfy(section ->
                assertThat(section.requiredByRules()).isTrue());
        assertThat(sections).extracting(EncodedSection::level)
                .containsExactly(1, 1, 2);
    }

    @Test
    @DisplayName("derived structure rules mark every recorded section as required")
    void derivedStructureRules() {
        SemanticDna dna = Fixtures.handDna();

        List<EncodedSection> sections = new StructureEncoder()
                .encode(ModelInput.of(dna, Fixtures.derivedPlan(dna)));

        assertThat(sections).extracting(EncodedSection::heading)
                .containsExactly("Overview", "Timeline", "Risks");
        assertThat(sections).allSatisfy(section ->
                assertThat(section.requiredByRules()).isTrue());
    }

    @Test
    @DisplayName("the fact/entity encoder carries criticality and plan requirements")
    void factsAndEntitiesCarryPlanRequirements() {
        SemanticDna dna = Fixtures.handDna();

        EncodedContent content = new FactEntityEncoder()
                .encode(ModelInput.of(dna, Fixtures.explicitPlan(dna)));

        assertThat(content.facts()).hasSize(2);
        assertThat(content.facts().getFirst().critical()).isTrue();
        assertThat(content.facts().getFirst().requiredByRules()).isTrue();
        assertThat(content.facts().get(1).critical()).isFalse();
        assertThat(content.facts().get(1).requiredByRules()).isFalse();
        assertThat(content.entities()).hasSize(2);
        assertThat(content.entities().getFirst().minMentions()).isEqualTo(2);
        assertThat(content.entities().getFirst().requiredEmissions()).isEqualTo(2);
        assertThat(content.entities().get(1).minMentions()).isZero();
        assertThat(content.entities().get(1).requiredEmissions()).isEqualTo(1);
    }

    @Test
    @DisplayName("derived rules require critical facts verbatim and recurring entities at their recorded mentions")
    void derivedRulesRequireCriticalFactsAndRecurringEntities() {
        SemanticDna dna = Fixtures.handDna();

        EncodedContent content = new FactEntityEncoder()
                .encode(ModelInput.of(dna, Fixtures.derivedPlan(dna)));

        assertThat(content.facts().getFirst().critical()).isTrue();
        assertThat(content.facts().getFirst().requiredByRules()).isTrue();
        assertThat(content.facts().get(1).critical()).isFalse();
        assertThat(content.facts().get(1).requiredByRules()).isFalse();
        assertThat(content.entities().getFirst().name()).isEqualTo("Core Database");
        assertThat(content.entities().getFirst().minMentions()).isEqualTo(3);
        assertThat(content.entities().getFirst().requiredEmissions()).isEqualTo(3);
        assertThat(content.entities().get(1).minMentions()).isZero();
        assertThat(content.entities().get(1).requiredEmissions()).isEqualTo(1);
    }

    @Test
    @DisplayName("the relationship encoder flags required relationships")
    void relationshipsFlaggedFromPlan() {
        SemanticDna dna = Fixtures.handDna();

        List<EncodedRelationship> explicit = new RelationshipEncoder()
                .encode(ModelInput.of(dna, Fixtures.explicitPlan(dna)));
        List<EncodedRelationship> derived = new RelationshipEncoder()
                .encode(ModelInput.of(dna, Fixtures.derivedPlan(dna)));

        assertThat(explicit).hasSize(1);
        assertThat(explicit.getFirst().requiredByRules()).isTrue();
        assertThat(derived).hasSize(1);
        assertThat(derived.getFirst().requiredByRules()).isTrue();
    }

    @Test
    @DisplayName("the DNA encoder assembles one unified representation with protection counts")
    void dnaEncoderAssembles() {
        SemanticDna dna = Fixtures.handDna();
        SemanticDna guarded = Fixtures.protectedDna();

        UnifiedRepresentation plain = new SemanticDnaEncoder()
                .encode(ModelInput.of(dna, Fixtures.derivedPlan(dna)));
        UnifiedRepresentation guardedRepresentation = new SemanticDnaEncoder()
                .encode(ModelInput.of(guarded, Fixtures.derivedPlan(guarded)));

        assertThat(plain.summary()).startsWith("The platform migration plan");
        assertThat(plain.sections()).hasSize(3);
        assertThat(plain.content().facts()).hasSize(2);
        assertThat(plain.relationships()).hasSize(1);
        assertThat(plain.protectedReferenceCount()).isZero();
        assertThat(guardedRepresentation.protectedReferenceCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("the vocabulary covers encoded material plus the critical marker only")
    void vocabularyCoversExactlyTheMaterial() {
        SemanticDna dna = Fixtures.handDna();
        UnifiedRepresentation representation = new SemanticDnaEncoder()
                .encode(ModelInput.of(dna, Fixtures.derivedPlan(dna)));

        var vocabulary = representation.vocabulary();

        assertThat(vocabulary).contains("migration", "database", "rollback",
                "overview", "timeline", "risks", "migrates",
                EncodedFact.CRITICAL_MARKER);
        assertThat(vocabulary).doesNotContain("budget", "fabricated", "unknown");
    }

    @Test
    @DisplayName("the unified representation is immutable")
    void representationImmutable() {
        SemanticDna dna = Fixtures.handDna();
        UnifiedRepresentation representation = new SemanticDnaEncoder()
                .encode(ModelInput.of(dna, Fixtures.derivedPlan(dna)));

        assertThatThrownBy(() -> representation.sections().add(null))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
