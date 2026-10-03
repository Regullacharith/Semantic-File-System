package com.sfs.evaluation;

import com.sfs.contracts.evaluation.FidelityDimension;
import com.sfs.contracts.reconstruction.ReconstructionJobView;
import com.sfs.reconstruction.engine.ReconstructionEngine;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@DisplayName("Regression benchmark: fidelity can never silently drop (12.9)")
class RegressionBenchmarkTest {

    private static final Map<String, EvaluationInput> CORPUS = new LinkedHashMap<>();
    private static final Map<String, String> ARTIFACTS = new LinkedHashMap<>();

    @BeforeAll
    static void buildCorpus() {
        EngineHarness harness = EngineHarness.harness();
        ReconstructionEngine engine = harness.reconstructionEngine();
        try {
            for (GoldDoc doc : GoldDoc.GOLD) {
                harness.analyze(doc);
                String jobId = engine.requestReconstruction(doc.objectId()).jobId();
                EngineHarness.awaitTerminal(engine, jobId);
                ReconstructionJobView job = engine.findJob(jobId).orElseThrow();
                String artifact = engine.findArtifact(jobId)
                        .orElseThrow(() -> new IllegalStateException(
                                "no artifact for " + doc.objectId()))
                        .content();
                ARTIFACTS.put(doc.objectId(), artifact);
                CORPUS.put(doc.objectId(), EvaluationInput.of(doc.objectId(),
                        doc.originalText(), artifact, harness.dnaByObject()
                                .get(doc.objectId())));
            }
        } finally {
            engine.shutdown();
        }
    }

    @Test
    @DisplayName("gold annotations: every critical fact and heading survives in the artifact")
    void goldExpectationsHold() {
        for (GoldDoc doc : GoldDoc.GOLD) {
            String artifact = ARTIFACTS.get(doc.objectId());
            for (String critical : doc.criticalFacts()) {
                if (!TextMatching.containsNormalized(artifact, critical)) {
                    fail("gold critical fact no longer survives for "
                            + doc.objectId() + ": " + critical);
                }
            }
            for (String heading : doc.headings()) {
                if (!TextMatching.containsNormalized(artifact, heading)) {
                    fail("gold heading no longer survives for "
                            + doc.objectId() + ": " + heading);
                }
            }
            for (String entity : doc.entities()) {
                if (!TextMatching.containsNormalized(artifact, entity)) {
                    fail("gold entity no longer survives for "
                            + doc.objectId() + ": " + entity);
                }
            }
        }
    }

    @Test
    @DisplayName("benchmark results are reproducible run over run")
    void reproducible() {
        List<RegressionBenchmark.Result> first =
                new RegressionBenchmark().run(CORPUS);
        List<RegressionBenchmark.Result> second =
                new RegressionBenchmark().run(CORPUS);

        for (int i = 0; i < first.size(); i++) {
            assertThat(first.get(i).dimensionScores())
                    .isEqualTo(second.get(i).dimensionScores());
            assertThat(first.get(i).overallFidelity())
                    .isEqualTo(second.get(i).overallFidelity());
            assertThat(first.get(i).artifactBytes())
                    .isEqualTo(second.get(i).artifactBytes());
            assertThat(first.get(i).knowledgePreservationDensity())
                    .isEqualTo(second.get(i).knowledgePreservationDensity());
        }
    }

    @Test
    @DisplayName("no dimension may silently fall below the committed baseline")
    void regressionBaselineHolds() {
        Properties baseline = loadBaseline();
        List<RegressionBenchmark.Result> results =
                new RegressionBenchmark().run(CORPUS);

        assertThat(results).hasSize(GoldDoc.GOLD.size());
        for (RegressionBenchmark.Result result : results) {
            for (FidelityDimension dimension : FidelityDimension.values()) {
                String key = result.objectId() + "." + dimension.name();
                String recorded = baseline.getProperty(key);
                if (recorded == null) {
                    fail("baseline is missing " + key + "; commit the measured "
                            + "value so regressions become visible");
                }
                double floor = Double.parseDouble(recorded);
                double actual = result.dimensionScores().get(dimension);
                if (actual < floor) {
                    fail(key + " dropped from " + floor + " to " + actual
                            + "; fidelity regressed and must not ship silently");
                }
            }
            String overallKey = result.objectId() + ".OVERALL";
            String recordedOverall = baseline.getProperty(overallKey);
            if (recordedOverall == null) {
                fail("baseline is missing " + overallKey);
            }
            if (result.overallFidelity() < Double.parseDouble(recordedOverall)) {
                fail(overallKey + " dropped from " + recordedOverall + " to "
                        + result.overallFidelity());
            }
        }
    }

    @Test
    @DisplayName("storage cost is reported beside fidelity for every document")
    void storageReportedBesideFidelity() {
        List<RegressionBenchmark.Result> results =
                new RegressionBenchmark().run(CORPUS);

        assertThat(results).allSatisfy(result -> {
            assertThat(result.artifactBytes()).isPositive();
            assertThat(result.dnaBytes()).isPositive();
            assertThat(result.knowledgePreservationDensity()).isPositive();
            for (GoldDoc doc : GoldDoc.GOLD) {
                if (doc.objectId().equals(result.objectId())) {
                    long originalBytes = doc.originalBytes().length;
                    assertThat(originalBytes).isPositive();
                    assertThat(result.artifactBytes())
                            .isLessThanOrEqualTo(originalBytes * 4);
                }
            }
        });
    }

    @Test
    @DisplayName("the improvement advisor turns collected errors into prioritized advice")
    void improvementLoop() {
        List<RegressionBenchmark.Result> results =
                new RegressionBenchmark().run(CORPUS);
        FidelityEvaluator evaluator = new FidelityEvaluator();
        ImprovementAdvisor advisor = new ImprovementAdvisor();
        List<FidelityReport> reports = CORPUS.entrySet().stream()
                .map(entry -> evaluator.evaluate(entry.getKey(), entry.getValue()))
                .toList();

        List<ImprovementSuggestion> suggestions = advisor.advise(reports);

        assertThat(reports).hasSize(results.size());
        for (FidelityReport report : reports) {
            assertThat(report.errors()).doesNotContain(ErrorCategory.MISSING_CRITICAL_FACT);
        }
        for (ImprovementSuggestion suggestion : suggestions) {
            assertThat(suggestion.advice()).isNotBlank();
            assertThat(suggestion.occurrences()).isPositive();
        }
        for (int i = 1; i < suggestions.size(); i++) {
            if (suggestions.get(i - 1).correctness()
                    == suggestions.get(i).correctness()) {
                assertThat(suggestions.get(i - 1).occurrences())
                        .isGreaterThanOrEqualTo(suggestions.get(i).occurrences());
            }
        }
    }

    private Properties loadBaseline() {
        Properties properties = new Properties();
        try (InputStream in = RegressionBenchmarkTest.class
                .getResourceAsStream("/regression/baseline.properties")) {
            if (in == null) {
                fail("regression/baseline.properties is missing; run the benchmark "
                        + "and commit the measured baseline");
            }
            properties.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("unreadable regression baseline", e);
        }
        return properties;
    }
}
