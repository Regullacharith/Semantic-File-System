package com.sfs.evaluation;

import com.sfs.core.dna.DnaIdentity;
import com.sfs.core.dna.Entity;
import com.sfs.core.dna.Fact;
import com.sfs.core.dna.FidelityProfile;
import com.sfs.core.dna.ProtectedReference;
import com.sfs.core.dna.Relationship;
import com.sfs.core.dna.SemanticDna;
import com.sfs.core.dna.SemanticDnaBuilder;
import com.sfs.core.dna.StructureNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("The six evaluators (12.1-12.6)")
class EvaluatorsTest {

    private static final String ORIGINAL = """
            # Overview

            The platform migration moves the core database in the second quarter.
            Rollback takes at most two hours.

            # Timeline

            The migration window opens on April 7.
            """;

    private static final String PERFECT_ARTIFACT = """
            The platform migration moves the core database in the second quarter.
            # Overview
            # Timeline
            [critical] The migration window opens on April 7.
            Rollback takes at most two hours.
            Core Database (system)
            Core Database migrates-to New Infrastructure.
            platform migration, capacity planning
            """;

    private static SemanticDna dna() {
        return SemanticDnaBuilder.forIdentity(new DnaIdentity(
                        "sfs-obj-6001-aaaaaaaa", "sfs-dna/0.2", 1,
                        "sfs-engine/0.1", Instant.parse("2026-06-01T08:00:00Z")))
                .summary("The platform migration moves the core database in the "
                        + "second quarter.")
                .concepts(List.of("platform migration", "capacity planning"))
                .entities(List.of(new Entity("Core Database", "system", 2)))
                .facts(List.of(
                        new Fact("The migration window opens on April 7.", true, 0.95),
                        new Fact("Rollback takes at most two hours.", false, 0.8)))
                .relationships(List.of(new Relationship(
                        "Core Database", "migrates-to", "New Infrastructure")))
                .structure(List.of(
                        new StructureNode("Overview", 1, 0),
                        new StructureNode("Timeline", 1, 1)))
                .fidelity(new FidelityProfile(0.9, 0.9, "sfs-engine/0.1"))
                .build();
    }

    private EvaluationInput input(String artifact) {
        return EvaluationInput.of("sfs-obj-6001-aaaaaaaa", ORIGINAL, artifact, dna());
    }

    @Test
    @DisplayName("a fully preserving artifact scores one on every non-semantic dimension")
    void perfectArtifact() {
        EvaluationInput input = input(PERFECT_ARTIFACT);

        assertThat(new StructuralEvaluator().evaluate(input)).satisfies(structural -> {
            assertThat(structural.headingsTotal()).isEqualTo(2);
            assertThat(structural.headingsPreserved()).isEqualTo(2);
            assertThat(structural.orderPreserved()).isTrue();
            assertThat(structural.summaryVerbatim()).isTrue();
            assertThat(structural.toDimensionScore()).isEqualTo(1.0);
        });
        FactualScore factual = new FactualEvaluator().evaluate(input);
        assertThat(factual.toDimensionScore()).isEqualTo(1.0);
        assertThat(factual.critical().total()).isEqualTo(1);
        assertThat(factual.critical().preserved()).isEqualTo(1);
        assertThat(factual.missingStatements()).isEmpty();
        assertThat(new EntityEvaluator().evaluate(input).toDimensionScore())
                .isEqualTo(1.0);
        RelationshipScore relationship = new RelationshipEvaluator().evaluate(input);
        assertThat(relationship.toDimensionScore()).isEqualTo(1.0);
        assertThat(relationship.withDirection()).isEqualTo(1);
        assertThat(new CompletenessEvaluator().evaluate(input).toDimensionScore())
                .isEqualTo(1.0);
    }

    @Test
    @DisplayName("semantic overlap measures the original against the artifact")
    void semanticOverlap() {
        SemanticScore score = new SemanticEvaluator().evaluate(input(PERFECT_ARTIFACT));

        assertThat(score.toDimensionScore()).isBetween(0.0, 1.0);
        assertThat(score.referenceTokens()).isPositive();
        assertThat(score.matchedTokens()).isPositive();
        assertThat(score.matchedTokens()).isLessThanOrEqualTo(score.referenceTokens());
    }

    @Test
    @DisplayName("losing material is visible in every affected dimension")
    void degradedArtifact() {
        String degraded = """
                Rollback takes at most two hours.
                # Timeline
                # Overview
                platform migration, capacity planning
                """;
        EvaluationInput input = input(degraded);

        FactualScore factual = new FactualEvaluator().evaluate(input);
        assertThat(factual.factsPreserved()).isEqualTo(1);
        assertThat(factual.missingStatements()).containsExactly(
                "The migration window opens on April 7.");
        assertThat(factual.critical().preserved()).isZero();
        assertThat(factual.critical().missing()).containsExactly(
                "The migration window opens on April 7.");

        StructuralScore structural = new StructuralEvaluator().evaluate(input);
        assertThat(structural.headingsPreserved()).isEqualTo(2);
        assertThat(structural.orderPreserved()).isFalse();

        assertThat(new EntityEvaluator().evaluate(input).missing())
                .containsExactly("Core Database");
        assertThat(new RelationshipEvaluator().evaluate(input).missing())
                .containsExactly("Core Database migrates-to New Infrastructure");
        assertThat(new CompletenessEvaluator().evaluate(input).unitsPresent())
                .isLessThan(new CompletenessEvaluator().evaluate(input).unitsTotal());
    }

    @Test
    @DisplayName("a lost summary is an explicit structural failure")
    void lostSummary() {
        String withoutSummary = """
                # Overview
                # Timeline
                [critical] The migration window opens on April 7.
                Rollback takes at most two hours.
                Core Database (system)
                Core Database migrates-to New Infrastructure.
                """;
        StructuralScore structural =
                new StructuralEvaluator().evaluate(input(withoutSummary));

        assertThat(structural.summaryVerbatim()).isFalse();
        assertThat(structural.toDimensionScore()).isLessThan(1.0);
    }

    @Test
    @DisplayName("entity substitution is detectable as a missing entity")
    void entitySubstitution() {
        String substituted = PERFECT_ARTIFACT.replace("Core Database (system)",
                "Database Core (system)");
        EntityScore entity = new EntityEvaluator().evaluate(input(substituted));

        assertThat(entity.preserved()).isEqualTo(1);
        assertThat(entity.total()).isEqualTo(1);
    }

    @Test
    @DisplayName("evaluation without an original falls back to DNA material as reference")
    void noOriginalUsesDnaReference() {
        EvaluationInput withoutOriginal = EvaluationInput.of("sfs-obj-6001-aaaaaaaa",
                null, PERFECT_ARTIFACT, dna());

        SemanticScore score = new SemanticEvaluator().evaluate(withoutOriginal);

        assertThat(score.referenceTokens()).isPositive();
        assertThat(score.toDimensionScore()).isBetween(0.0, 1.0);
    }

    @Test
    @DisplayName("protected DNA is flagged so withheld values stay visible")
    void protectedFlagged() {
        SemanticDna guarded = SemanticDnaBuilder.forIdentity(new DnaIdentity(
                        "sfs-obj-6002-bbbbbbbb", "sfs-dna/0.2", 1,
                        "sfs-engine/0.1", Instant.parse("2026-06-01T08:00:00Z")))
                .summary("The deployment credentials list the database password.")
                .entities(List.of(new Entity("Production Platform", "system", 1)))
                .facts(List.of(new Fact(
                        "The production database requires a password.", true, 0.9)))
                .protectedReferences(List.of(new ProtectedReference("ref-1",
                        "credential", "database password", "Credentials")))
                .fidelity(new FidelityProfile(0.9, 0.9, "sfs-engine/0.1"))
                .build();
        String artifact = "The deployment credentials list the database password.\n"
                + "The production database requires a password.\n"
                + "Production Platform (system)\n";
        EvaluationInput input = EvaluationInput.of("sfs-obj-6002-bbbbbbbb",
                null, artifact, guarded);

        FidelityReport report = new FidelityEvaluator().evaluate(
                "job-9001", input);

        assertThat(report.errors()).contains(ErrorCategory.PROTECTED_VALUE_WITHHELD);
        assertThat(report.knowledgePreservationDensity()).isPositive();
    }
}
