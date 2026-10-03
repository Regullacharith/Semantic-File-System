package com.sfs.evaluation;

import com.sfs.contracts.evaluation.FidelityDimension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class RegressionBenchmark {

    public record Result(String objectId, double overallFidelity,
                         Map<FidelityDimension, Double> dimensionScores,
                         int criticalFactsTotal, int criticalFactsPreserved,
                         long artifactBytes, long dnaBytes) {

        public Result {
            Objects.requireNonNull(objectId, "objectId must not be null");
            Objects.requireNonNull(dimensionScores, "dimensionScores must not be null");
        }

        public double knowledgePreservationDensity() {
            return (double) dnaBytes / artifactBytes;
        }
    }

    private final FidelityEvaluator evaluator = new FidelityEvaluator();

    public List<Result> run(Map<String, EvaluationInput> corpus) {
        Objects.requireNonNull(corpus, "corpus must not be null");
        List<Result> results = new ArrayList<>();
        for (Map.Entry<String, EvaluationInput> entry : corpus.entrySet()) {
            FidelityReport report = evaluator.evaluate(entry.getKey(),
                    entry.getValue());
            results.add(new Result(entry.getKey(), report.overallFidelity(),
                    report.dimensionScores(), report.factual().critical().total(),
                    report.factual().critical().preserved(), report.artifactBytes(),
                    report.dnaBytes()));
        }
        return List.copyOf(results);
    }
}
