package com.sfs.core.rules;

import com.sfs.core.dna.DnaCanonical;
import com.sfs.core.dna.SemanticDna;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ReconstructionPlanner")
class ReconstructionPlannerTest {

    private static final Instant T0 = Instant.parse("2026-03-15T10:00:00Z");

    private final RuleRepository repository = new RuleRepository();
    private final ReconstructionPlanner planner = new ReconstructionPlanner(repository);

    @Test
    @DisplayName("a plan can be generated from DNA alone")
    void planFromDnaAlone() {
        SemanticDna dna = RuleSetsFixture.dna();
        ReconstructionPlan plan = planner.plan(dna, T0);

        assertThat(plan.objectId()).isEqualTo(dna.objectId());
        assertThat(plan.dnaVersion()).isEqualTo(1);
        assertThat(plan.dnaSha256()).isEqualTo(DnaCanonical.integrityHash(dna));
        assertThat(plan.rulesVersion()).isEqualTo("sfs-rules/0.2");
        assertThat(plan.requiredFacts()).isNotEmpty();
        assertThat(plan.requiredEntities()).isNotEmpty();
        assertThat(plan.requiredRelationships()).isNotEmpty();
        assertThat(plan.content().preserveSummaryVerbatim()).isTrue();
        assertThat(plan.content().summary()).isEqualTo(dna.summary());
        assertThat(plan.validation().forbidInventedFacts()).isTrue();
        assertThat(plan.warnings()).isEmpty();
    }

    @Test
    @DisplayName("a second plan reuses the bound rule set")
    void secondPlanReuses() {
        planner.plan(RuleSetsFixture.dna(), T0);
        planner.plan(RuleSetsFixture.dna(), T0);
        assertThat(repository.hits()).isEqualTo(1);
        assertThat(repository.derivations()).isEqualTo(1);
    }

    @Test
    @DisplayName("an error-severity conflict refuses planning explicitly")
    void errorConflictRefuses() {
        SemanticDna dna = RuleSetsFixture.dna();
        RuleSet derived = new RuleDeriver().derive(dna);
        Rule conflicting = new Rule("facts-extra", RuleType.FACT,
                RulePriority.CRITICAL, "d",
                List.of(new RequiredFactConstraint(
                        derived.rules().stream()
                                .filter(rule -> rule.type() == RuleType.FACT)
                                .findFirst()
                                .orElseThrow()
                                .constraints()
                                .stream()
                                .map(RequiredFactConstraint.class::cast)
                                .findFirst()
                                .orElseThrow()
                                .statement(),
                        false)));
        List<Rule> rules = new java.util.ArrayList<>(derived.rules());
        rules.add(conflicting);
        repository.save(new RuleSet(derived.objectId(), derived.dnaVersion(),
                derived.dnaSha256(), derived.rulesVersion(), rules), T0);

        assertThatThrownBy(() -> planner.plan(dna, T0))
                .isInstanceOf(RulePlanningException.class)
                .hasMessageContaining("contradictory missing-severity");
    }

    @Test
    @DisplayName("a version conflict becomes a plan warning, not a refusal")
    void versionConflictWarns() {
        SemanticDna dna = RuleSetsFixture.dna();
        planner.plan(dna, T0);
        SemanticDna tampered = new SemanticDna(
                dna.identity(), dna.summary() + " Tampered.", dna.concepts(),
                dna.topics(), dna.entities(), dna.facts(), dna.relationships(),
                dna.structure(), dna.embedding(), dna.behaviour(),
                dna.reconstructionRules(), dna.fidelity(), dna.security());

        ReconstructionPlan plan = planner.plan(tampered, T0);

        assertThat(plan.warnings()).anySatisfy(warning ->
                assertThat(warning).contains("different DNA content"));
    }

    @Test
    @DisplayName("a narrative document plans without section order")
    void narrativeHasNoSectionOrder() {
        SemanticDna dna = RuleSetsFixture.dna();
        SemanticDna bodyOnly = new SemanticDna(
                dna.identity(), dna.summary(), dna.concepts(), dna.topics(),
                dna.entities(), dna.facts(), dna.relationships(),
                List.of(new com.sfs.core.dna.StructureNode("Body", 1, 0)),
                dna.embedding(), dna.behaviour(), dna.reconstructionRules(),
                dna.fidelity(), dna.security());

        ReconstructionPlan plan = planner.plan(bodyOnly, T0);

        assertThat(plan.sectionOrder()).isEmpty();
        assertThat(plan.requiredFacts()).isNotEmpty();
    }
}
