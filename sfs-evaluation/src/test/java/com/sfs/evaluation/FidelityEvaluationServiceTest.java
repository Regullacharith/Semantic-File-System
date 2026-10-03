package com.sfs.evaluation;

import com.sfs.contracts.evaluation.EvaluationAvailability;
import com.sfs.contracts.evaluation.FidelityDimension;
import com.sfs.contracts.evaluation.FidelityReportView;
import com.sfs.contracts.reconstruction.ReconstructionJobView;
import com.sfs.core.dna.InMemoryDnaRepository;
import com.sfs.lifecycle.store.InMemoryRawContentStore;
import com.sfs.lifecycle.store.RawContentStore;
import com.sfs.reconstruction.engine.ReconstructionEngine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Fidelity evaluation service: the frozen EvaluationService, real")
class FidelityEvaluationServiceTest {

    private EngineHarness harness;
    private ReconstructionEngine reconstructionEngine;
    private InMemoryRawContentStore rawContentStore;
    private FidelityEvaluationService service;
    private String liveJobId;
    private String memorizedObjectId;

    @BeforeEach
    void setUp() {
        harness = EngineHarness.harness();
        reconstructionEngine = harness.reconstructionEngine();
        rawContentStore = new InMemoryRawContentStore();
        for (GoldDoc doc : GoldDoc.GOLD) {
            harness.analyze(doc);
            rawContentStore.store(doc.objectId(), doc.originalBytes());
        }
        memorizedObjectId = GoldDoc.GOLD.getFirst().objectId();
        service = new FidelityEvaluationService(reconstructionEngine,
                rawContentStore, harness.dnaRepository());

        liveJobId = reconstruct(GoldDoc.GOLD.get(1).objectId());
        reconstruct(memorizedObjectId);
        rawContentStore.release(memorizedObjectId);
    }

    @AfterEach
    void tearDown() {
        reconstructionEngine.shutdown();
    }

    private String reconstruct(String objectId) {
        String jobId = reconstructionEngine.requestReconstruction(objectId).jobId();
        EngineHarness.awaitTerminal(reconstructionEngine, jobId);
        return jobId;
    }

    @Test
    @DisplayName("a completed reconstruction with its original is measured")
    void measuresCompletedReconstruction() {
        EvaluationAvailability availability = service.findEvaluation(liveJobId);

        assertThat(availability.isAvailable()).isTrue();
        FidelityReportView report = availability.reportIfAvailable().orElseThrow();
        assertThat(report.dimensionScores()).containsKeys(FidelityDimension.values());
        assertThat(report.evaluatorVersion()).isEqualTo("sfs-evaluation/0.1");
        assertThat(report.originalBytes()).isPositive();
        assertThat(report.semanticMemoryBytes()).isPositive();
        assertThat(report.findings()).isNotEmpty();
        assertThat(report.originalBytes())
                .isEqualTo(GoldDoc.GOLD.get(1).originalBytes().length);
    }

    @Test
    @DisplayName("a deleted original is reported as unmeasurable, never estimated")
    void memorizedOriginalUnmeasurable() {
        String jobId = reconstruct(memorizedObjectId);

        EvaluationAvailability availability = service.findEvaluation(jobId);

        assertThat(availability.isAvailable()).isFalse();
        assertThat(availability.status())
                .isEqualTo(EvaluationAvailability.Status.ORIGINAL_UNAVAILABLE);
        assertThat(availability.reason()).contains("no score is estimated");
    }

    @Test
    @DisplayName("a job without an artifact reports no evaluation")
    void rejectedJobHasNoArtifact() {
        String jobId = reconstruct("sfs-obj-9999-ffffffff");

        EvaluationAvailability availability = service.findEvaluation(jobId);

        assertThat(availability.isAvailable()).isFalse();
        assertThat(availability.status())
                .isEqualTo(EvaluationAvailability.Status.NO_ARTIFACT);
    }

    @Test
    @DisplayName("an unknown job is reported as not evaluated")
    void unknownJobNotEvaluated() {
        assertThat(service.findEvaluation("job-9999").status())
                .isEqualTo(EvaluationAvailability.Status.NOT_EVALUATED);
    }

    @Test
    @DisplayName("every reconstruction appears in the evaluation list")
    void listsAllEvaluations() {
        List<EvaluationAvailability> evaluations = service.listEvaluations();

        assertThat(evaluations).hasSize(2);
        assertThat(evaluations).anySatisfy(availability ->
                assertThat(availability.isAvailable()).isTrue());
        assertThat(evaluations).anySatisfy(availability ->
                assertThat(availability.status())
                        .isEqualTo(EvaluationAvailability.Status.ORIGINAL_UNAVAILABLE));
    }

    @Test
    @DisplayName("evaluations are reproducible and the served counter advances")
    void reproducibleWithCounter() {
        EvaluationAvailability first = service.findEvaluation(liveJobId);
        EvaluationAvailability second = service.findEvaluation(liveJobId);

        FidelityReportView firstReport = first.reportIfAvailable().orElseThrow();
        FidelityReportView secondReport = second.reportIfAvailable().orElseThrow();
        assertThat(firstReport.dimensionScores())
                .isEqualTo(secondReport.dimensionScores());
        assertThat(firstReport.criticalFactScore())
                .isEqualTo(secondReport.criticalFactScore());
        assertThat(service.evaluationsServed()).isGreaterThanOrEqualTo(2);
    }
}
