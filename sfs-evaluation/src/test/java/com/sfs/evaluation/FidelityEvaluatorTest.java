package com.sfs.evaluation;

import com.sfs.contracts.evaluation.FidelityDimension;
import com.sfs.contracts.evaluation.FidelityReportView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("The fidelity evaluation pipeline")
class FidelityEvaluatorTest {

    private static final String ORIGINAL = """
            The platform migration moves the core database in the second quarter.

            # Overview

            body
            """;

    private static final String ARTIFACT = """
            The platform migration moves the core database in the second quarter.
            # Overview
            [critical] The migration window opens on April 7.
            Rollback takes at most two hours.
            Core Database (system)
            Core Database migrates-to New Infrastructure.
            platform migration, capacity planning
            Overview
            Timeline
            """;

    @Test
    @DisplayName("every reconstruction receives every required metric")
    void allMetricsPresent() {
        FidelityReport report = new FidelityEvaluator().evaluate("job-7001",
                EvaluationInput.of("sfs-obj-6001-aaaaaaaa", ORIGINAL, ARTIFACT,
                        EngineSupport.dna("sfs-obj-6001-aaaaaaaa")));

        assertThat(report.dimensionScores()).containsKeys(FidelityDimension.values());
        assertThat(report.dimensionScores()).allSatisfy((dimension, score) ->
                assertThat(score).isBetween(0.0, 1.0));
        assertThat(report.overallFidelity()).isBetween(0.0, 1.0);
        assertThat(report.semantic()).isNotNull();
        assertThat(report.structural()).isNotNull();
        assertThat(report.factual()).isNotNull();
        assertThat(report.entity()).isNotNull();
        assertThat(report.relationship()).isNotNull();
        assertThat(report.completeness()).isNotNull();
        assertThat(report.calibration()).hasSize(4);
        assertThat(report.originalBytes()).isPositive();
        assertThat(report.artifactBytes()).isPositive();
        assertThat(report.dnaBytes()).isPositive();
        assertThat(report.knowledgePreservationDensity()).isPositive();
    }

    @Test
    @DisplayName("identical input produces an identical report")
    void reproducible() {
        FidelityEvaluator evaluator = new FidelityEvaluator();
        EvaluationInput input = EvaluationInput.of("sfs-obj-6001-aaaaaaaa",
                ORIGINAL, ARTIFACT, EngineSupport.dna("sfs-obj-6001-aaaaaaaa"));

        FidelityReport first = evaluator.evaluate("job-7002", input);
        FidelityReport second = evaluator.evaluate("job-7003", input);

        assertThat(first.dimensionScores()).isEqualTo(second.dimensionScores());
        assertThat(first.overallFidelity()).isEqualTo(second.overallFidelity());
        assertThat(first.errors()).isEqualTo(second.errors());
        assertThat(first.calibration()).isEqualTo(second.calibration());
        assertThat(first.artifactBytes()).isEqualTo(second.artifactBytes());
    }

    @Test
    @DisplayName("the report maps onto the frozen view with complete scores")
    void frozenViewMapping() {
        FidelityEvaluator evaluator = new FidelityEvaluator();
        FidelityReport report = evaluator.evaluate("job-7004",
                EvaluationInput.of("sfs-obj-6001-aaaaaaaa", ORIGINAL, ARTIFACT,
                        EngineSupport.dna("sfs-obj-6001-aaaaaaaa")));

        FidelityReportView view = report.toView(evaluator.findings(report));

        assertThat(view.jobId()).isEqualTo("job-7004");
        assertThat(view.objectId()).isEqualTo("sfs-obj-6001-aaaaaaaa");
        assertThat(view.evaluatorVersion()).isEqualTo("sfs-evaluation/0.1");
        assertThat(view.criticalFactsTotal())
                .isEqualTo(report.factual().critical().total());
        assertThat(view.criticalFactsPreserved())
                .isEqualTo(report.factual().critical().preserved());
        assertThat(view.semanticMemoryBytes()).isEqualTo(report.artifactBytes());
        assertThat(view.findings()).hasSize(7);
        assertThat(view.findings()).anySatisfy(finding ->
                assertThat(finding.dimension()).isEqualTo(FidelityDimension.FACTUAL));
    }

    @Test
    @DisplayName("degraded artifacts collect the right error categories")
    void errorCategoriesCollected() {
        String degraded = """
                Rollback takes at most two hours.
                # Timeline
                # Overview
                platform migration
                """;
        FidelityReport report = new FidelityEvaluator().evaluate("job-7005",
                EvaluationInput.of("sfs-obj-6001-aaaaaaaa", ORIGINAL, degraded,
                        EngineSupport.dna("sfs-obj-6001-aaaaaaaa")));

        assertThat(report.errors()).contains(ErrorCategory.MISSING_CRITICAL_FACT);
        assertThat(report.errors()).contains(ErrorCategory.MISSING_ENTITY);
        assertThat(report.errors()).contains(ErrorCategory.MISSING_RELATIONSHIP);
        assertThat(report.errors()).contains(ErrorCategory.SECTION_ORDER_BROKEN);
        assertThat(report.overallFidelity()).isLessThan(1.0);
    }
}
