package com.sfs.ui.mock;

import com.sfs.contracts.evaluation.EvaluationAvailability;
import com.sfs.contracts.evaluation.FidelityDimension;
import com.sfs.contracts.evaluation.FidelityReportView;
import com.sfs.reconstruction.PlanConstraintInterface;
import com.sfs.reconstruction.engine.ReconstructionArtifactFactory;
import com.sfs.reconstruction.engine.ReconstructionEngine;
import com.sfs.reconstruction.engine.ReconstructionJob;
import com.sfs.reconstruction.engine.ReconstructionPostProcessor;
import com.sfs.reconstruction.model.DeterministicBaselineRenderer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Mock evaluation service")
class MockEvaluationServiceTest {

    private static final String MEMORIZED_OBJECT = "sfs-obj-0002-e5f6a7b8";

    private static final String LIVE_OBJECT = "sfs-obj-0001-a1b2c3d4";

    private static final String REJECTED_OBJECT = "sfs-obj-0004-b3c4d5e6";

    private ReconstructionEngine reconstructionService;
    private MockEvaluationService service;

    @BeforeEach
    void setUp() {
        var suite = EngineTestSupport.seeded();
        reconstructionService = new ReconstructionEngine(
                new com.sfs.reconstruction.engine.DnaRuleLoader(
                        suite.dnaRepository(), suite.lifecycle()),
                suite.planner(),
                new DeterministicBaselineRenderer(),
                new PlanConstraintInterface(),
                new ReconstructionPostProcessor(),
                new ReconstructionArtifactFactory());
        service = new MockEvaluationService(reconstructionService, suite.lifecycle());
    }

    private void awaitTerminal(String jobId) {
        long deadline = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadline) {
            Optional<ReconstructionJob> job =
                    reconstructionService.findJobRecord(jobId);
            if (job.isPresent() && job.get().terminal()) {
                return;
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("await interrupted", e);
            }
        }
        throw new IllegalStateException("job never reached a terminal state");
    }

    @Test
    @DisplayName("refuses to score a reconstruction whose original was deleted")
    void refusesToScoreWithoutAnOriginal() {
        String jobId = reconstructionService.requestReconstruction(MEMORIZED_OBJECT).jobId();
        awaitTerminal(jobId);

        EvaluationAvailability evaluation = service.findEvaluation(jobId);

        assertThat(evaluation.isAvailable()).isFalse();
        assertThat(evaluation.status())
                .isEqualTo(EvaluationAvailability.Status.ORIGINAL_UNAVAILABLE);
        assertThat(evaluation.reportIfAvailable()).isEmpty();
        assertThat(evaluation.reason()).contains("no score is estimated");
    }

    @Test
    @DisplayName("produces a report when the original is still present")
    void producesReportWhenComparisonIsPossible() {
        String jobId = reconstructionService.requestReconstruction(LIVE_OBJECT).jobId();
        awaitTerminal(jobId);

        EvaluationAvailability evaluation = service.findEvaluation(jobId);

        assertThat(evaluation.isAvailable()).isTrue();
        assertThat(evaluation.reportIfAvailable()).isPresent();
    }

    @Test
    @DisplayName("reports no evaluation for a rejected reconstruction")
    void noEvaluationWithoutAnArtifact() {
        String jobId = reconstructionService.requestReconstruction(REJECTED_OBJECT).jobId();
        awaitTerminal(jobId);

        assertThat(service.findEvaluation(jobId).status())
                .isEqualTo(EvaluationAvailability.Status.NO_ARTIFACT);
    }

    @Test
    @DisplayName("reports an unknown job as not evaluated")
    void unknownJobIsNotEvaluated() {
        assertThat(service.findEvaluation("job-9999").status())
                .isEqualTo(EvaluationAvailability.Status.NOT_EVALUATED);
    }

    @Test
    @DisplayName("scores every dimension separately")
    void scoresEveryDimension() {
        String jobId = reconstructionService.requestReconstruction(LIVE_OBJECT).jobId();
        awaitTerminal(jobId);
        FidelityReportView report = service.findEvaluation(jobId).reportIfAvailable().orElseThrow();

        for (FidelityDimension dimension : FidelityDimension.values()) {
            assertThat(report.scoreFor(dimension)).isBetween(0.0, 1.0);
        }
    }

    @Test
    @DisplayName("shows a factual shortfall alongside a strong semantic score")
    void surfacesFactualShortfall() {
        String jobId = reconstructionService.requestReconstruction(LIVE_OBJECT).jobId();
        awaitTerminal(jobId);
        FidelityReportView report = service.findEvaluation(jobId).reportIfAvailable().orElseThrow();

        assertThat(report.scoreFor(FidelityDimension.SEMANTIC))
                .isGreaterThan(report.scoreFor(FidelityDimension.FACTUAL));
        assertThat(report.hasCriticalFactLoss()).isTrue();
    }

    @Test
    @DisplayName("records storage cost beside fidelity")
    void recordsStorageCost() {
        String jobId = reconstructionService.requestReconstruction(LIVE_OBJECT).jobId();
        awaitTerminal(jobId);
        FidelityReportView report = service.findEvaluation(jobId).reportIfAvailable().orElseThrow();

        assertThat(report.originalBytes()).isPositive();
        assertThat(report.semanticMemoryBytes()).isPositive();
        assertThat(report.storageRatio()).isPresent();
    }

    @Test
    @DisplayName("lists unmeasurable outcomes rather than omitting them")
    void listsUnmeasurableOutcomes() {
        String memorized = reconstructionService.requestReconstruction(MEMORIZED_OBJECT).jobId();
        String rejected = reconstructionService.requestReconstruction(REJECTED_OBJECT).jobId();
        awaitTerminal(memorized);
        awaitTerminal(rejected);

        assertThat(service.listEvaluations())
                .hasSize(2)
                .extracting(EvaluationAvailability::status)
                .containsExactlyInAnyOrder(
                        EvaluationAvailability.Status.ORIGINAL_UNAVAILABLE,
                        EvaluationAvailability.Status.NO_ARTIFACT);
    }

    @Test
    @DisplayName("names the evaluator that produced each report")
    void namesEvaluator() {
        String jobId = reconstructionService.requestReconstruction(LIVE_OBJECT).jobId();
        awaitTerminal(jobId);
        FidelityReportView report = service.findEvaluation(jobId).reportIfAvailable().orElseThrow();

        assertThat(report.evaluatorVersion()).isNotBlank().contains("mock");
    }
}
