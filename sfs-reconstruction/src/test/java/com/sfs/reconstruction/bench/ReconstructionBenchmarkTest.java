package com.sfs.reconstruction.bench;

import com.sfs.reconstruction.ConstraintInterface;
import com.sfs.reconstruction.CollectingTrainingHook;
import com.sfs.reconstruction.Fixtures;
import com.sfs.reconstruction.ModelInput;
import com.sfs.reconstruction.ModelOutput;
import com.sfs.reconstruction.NaiveEchoModel;
import com.sfs.reconstruction.PlanConstraintInterface;
import com.sfs.reconstruction.model.DeterministicBaselineRenderer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Reconstruction benchmark: fidelity, latency and storage over the frozen corpus (10.8)")
class ReconstructionBenchmarkTest {

    private final Map<String, ModelInput> corpus = Fixtures.engineCorpus();
    private final PlanConstraintInterface constraints = new PlanConstraintInterface();
    private final ReconstructionBenchmark benchmark =
            new ReconstructionBenchmark(constraints);
    private final DeterministicBaselineRenderer baseline =
            new DeterministicBaselineRenderer();
    private final NaiveEchoModel echo = new NaiveEchoModel();

    private List<ModelInput> cleanCases() {
        return Fixtures.CORPUS_IDS.stream()
                .filter(id -> !id.endsWith("credentials"))
                .map(corpus::get)
                .toList();
    }

    @Test
    @DisplayName("the baseline satisfies every constraint on every clean corpus document")
    void baselineSatisfiesCleanCorpus() {
        List<BenchmarkRun> runs = benchmark.run(cleanCases(), baseline);

        assertThat(runs).hasSize(3);
        assertThat(runs).allSatisfy(run -> {
            assertThat(run.satisfied()).isTrue();
            assertThat(run.violationCount()).isZero();
            assertThat(run.summaryVerbatim()).isTrue();
            assertThat(run.criticalCoverage()).isEqualTo(1.0);
            assertThat(run.entityCoverage()).isEqualTo(1.0);
            assertThat(run.relationshipCoverage()).isEqualTo(1.0);
            assertThat(run.reconstructNanos()).isPositive();
            assertThat(run.verifyNanos()).isPositive();
        });
    }

    @DisplayName("the protected corpus document is refused with an explicit violation")
    void protectedDocumentRefused() {
        BenchmarkRun run = benchmark.run(
                List.of(corpus.get("sfs-obj-1004-credentials")), baseline)
                .getFirst();

        assertThat(run.satisfied()).isFalse();
        assertThat(run.criticalCoverage()).isEqualTo(1.0);
        ModelInput input = corpus.get("sfs-obj-1004-credentials");
        ConstraintInterface.Result result = constraints.judge(input,
                baseline.reconstruct(input).draftText());
        assertThat(result.violations()).anySatisfy(violation ->
                assertThat(violation).contains("protected value"));
        assertThat(baseline.reconstruct(input).draftText())
                .doesNotContain("hunter2")
                .doesNotContain("sk-live-9f8e7d6c5b4a");
    }

    @Test
    @DisplayName("the harness discriminates: the naive echo model measures strictly worse")
    void harnessDiscriminatesModels() {
        List<BenchmarkRun> baselineRuns = benchmark.run(cleanCases(), baseline);
        List<BenchmarkRun> echoRuns = benchmark.run(cleanCases(), echo);

        for (int i = 0; i < baselineRuns.size(); i++) {
            BenchmarkRun strong = baselineRuns.get(i);
            BenchmarkRun weak = echoRuns.get(i);
            assertThat(weak.satisfied()).isFalse();
            assertThat(weak.violationCount()).isGreaterThan(strong.violationCount());
            assertThat(weak.summaryVerbatim()).isFalse();
            assertThat(strong.summaryVerbatim()).isTrue();
        }
    }

    @Test
    @DisplayName("the benchmark is reproducible: identical fidelity and drafts across runs")
    void reproducible() {
        List<ModelInput> cases = cleanCases();
        List<BenchmarkRun> first = benchmark.run(cases, baseline);
        List<BenchmarkRun> second = benchmark.run(cases, baseline);

        for (int i = 0; i < first.size(); i++) {
            BenchmarkRun a = first.get(i);
            BenchmarkRun b = second.get(i);
            assertThat(a.satisfied()).isEqualTo(b.satisfied());
            assertThat(a.violationCount()).isEqualTo(b.violationCount());
            assertThat(a.warningCount()).isEqualTo(b.warningCount());
            assertThat(a.criticalCoverage()).isEqualTo(b.criticalCoverage());
            assertThat(a.entityCoverage()).isEqualTo(b.entityCoverage());
            assertThat(a.relationshipCoverage()).isEqualTo(b.relationshipCoverage());
            assertThat(a.summaryVerbatim()).isEqualTo(b.summaryVerbatim());
            assertThat(a.dnaBytes()).isEqualTo(b.dnaBytes());
            assertThat(a.draftBytes()).isEqualTo(b.draftBytes());
            assertThat(baseline.reconstruct(cases.get(i)).draftText())
                    .isEqualTo(baseline.reconstruct(cases.get(i)).draftText());
        }
    }

    @Test
    @DisplayName("storage is measured as knowledge preservation density over canonical DNA bytes")
    void storageMeasuredAsDensity() {
        List<BenchmarkRun> runs = benchmark.run(cleanCases(), baseline);

        assertThat(runs).allSatisfy(run -> {
            assertThat(run.dnaBytes()).isPositive();
            assertThat(run.draftBytes()).isPositive();
            assertThat(run.knowledgePreservationDensity()).isPositive();
            assertThat(run.storageRatio()).isPositive();
            assertThat(run.knowledgePreservationDensity())
                    .isCloseTo(1.0 / run.storageRatio(),
                            org.assertj.core.data.Offset.offset(1e-9));
        });
    }

    @Test
    @DisplayName("every reconstruction passes through the future-training hook")
    void trainingHookReceivesEveryRun() {
        CollectingTrainingHook hook = new CollectingTrainingHook();
        List<ModelInput> cases = List.copyOf(corpus.values().stream().toList());

        benchmark.run(cases, baseline, hook);

        assertThat(hook.received()).hasSize(cases.size());
        for (int i = 0; i < cases.size(); i++) {
            String expectedPrefix = cases.get(i).objectId() + ":"
                    + DeterministicBaselineRenderer.MODEL_ID + ":";
            assertThat(hook.received().get(i)).startsWith(expectedPrefix);
        }
    }
}
