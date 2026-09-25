package com.sfs.ui.mock;

import com.sfs.contracts.evaluation.EvaluationAvailability;
import com.sfs.contracts.evaluation.FidelityDimension;
import com.sfs.contracts.evaluation.FidelityReportView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the mock evaluator's honesty rules.
 *
 * <p>Wired to the real reconstruction, file and semantic mocks so the services are exercised
 * together as the running application uses them.
 */
@DisplayName("Mock evaluation service")
class MockEvaluationServiceTest {

    /** Memorized: raw bytes deleted, so no comparison is possible. */
    private static final String MEMORIZED_OBJECT = "sfs-obj-0002-e5f6a7b8";

    /** Analyzed with the original still present: measurable. */
    private static final String LIVE_OBJECT = "sfs-obj-0001-a1b2c3d4";

    /** Holds protected values, so reconstruction is rejected and no artifact exists. */
    private static final String REJECTED_OBJECT = "sfs-obj-0004-b3c4d5e6";

    private MockReconstructionService reconstructionService;
    private MockEvaluationService service;

    @BeforeEach
    void setUp() {
        var suite = EngineTestSupport.seeded();
        reconstructionService = new MockReconstructionService(
                suite.lifecycle(), suite.records(), suite.records(), suite.planner());
        service = new MockEvaluationService(reconstructionService, suite.lifecycle());
    }

    @Test
    @DisplayName("refuses to score a reconstruction whose original was deleted")
    void refusesToScoreWithoutAnOriginal() {
        // The case most likely to tempt a fabricated number: the reconstruction succeeded,
        // but there is no original left to measure it against.
        String jobId = reconstructionService.requestReconstruction(MEMORIZED_OBJECT).jobId();

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

        EvaluationAvailability evaluation = service.findEvaluation(jobId);

        assertThat(evaluation.isAvailable()).isTrue();
        assertThat(evaluation.reportIfAvailable()).isPresent();
    }

    @Test
    @DisplayName("reports no evaluation for a rejected reconstruction")
    void noEvaluationWithoutAnArtifact() {
        String jobId = reconstructionService.requestReconstruction(REJECTED_OBJECT).jobId();

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
        FidelityReportView report = service.findEvaluation(jobId).reportIfAvailable().orElseThrow();

        for (FidelityDimension dimension : FidelityDimension.values()) {
            assertThat(report.scoreFor(dimension)).isBetween(0.0, 1.0);
        }
    }

    @Test
    @DisplayName("shows a factual shortfall alongside a strong semantic score")
    void surfacesFactualShortfall() {
        // The fixture is deliberately shaped this way: a report where semantic quality is
        // high and factual fidelity is not is exactly what an aggregate score would hide.
        String jobId = reconstructionService.requestReconstruction(LIVE_OBJECT).jobId();
        FidelityReportView report = service.findEvaluation(jobId).reportIfAvailable().orElseThrow();

        assertThat(report.scoreFor(FidelityDimension.SEMANTIC))
                .isGreaterThan(report.scoreFor(FidelityDimension.FACTUAL));
        assertThat(report.hasCriticalFactLoss()).isTrue();
    }

    @Test
    @DisplayName("records storage cost beside fidelity")
    void recordsStorageCost() {
        String jobId = reconstructionService.requestReconstruction(LIVE_OBJECT).jobId();
        FidelityReportView report = service.findEvaluation(jobId).reportIfAvailable().orElseThrow();

        assertThat(report.originalBytes()).isPositive();
        assertThat(report.semanticMemoryBytes()).isPositive();
        assertThat(report.storageRatio()).isPresent();
    }

    @Test
    @DisplayName("lists unmeasurable outcomes rather than omitting them")
    void listsUnmeasurableOutcomes() {
        reconstructionService.requestReconstruction(MEMORIZED_OBJECT);
        reconstructionService.requestReconstruction(REJECTED_OBJECT);

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
        FidelityReportView report = service.findEvaluation(jobId).reportIfAvailable().orElseThrow();

        assertThat(report.evaluatorVersion()).isNotBlank().contains("mock");
    }
}
