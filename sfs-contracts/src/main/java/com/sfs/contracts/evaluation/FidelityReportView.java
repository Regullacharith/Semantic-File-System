package com.sfs.contracts.evaluation;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Measured fidelity of one reconstruction, compared against its original.
 */
public record FidelityReportView(
        String jobId,
        String objectId,
        Map<FidelityDimension, Double> dimensionScores,
        double criticalFactScore,
        int criticalFactsTotal,
        int criticalFactsPreserved,
        List<EvaluationFinding> findings,
        long originalBytes,
        long semanticMemoryBytes,
        String evaluatorVersion,
        Instant evaluatedAt) {

    /**
     * Canonical constructor.
     */
    public FidelityReportView {
        Objects.requireNonNull(jobId, "jobId must not be null");
        Objects.requireNonNull(objectId, "objectId must not be null");
        Objects.requireNonNull(evaluatorVersion, "evaluatorVersion must not be null");
        Objects.requireNonNull(evaluatedAt, "evaluatedAt must not be null");
        Objects.requireNonNull(dimensionScores, "dimensionScores must not be null");

        if (jobId.isBlank()) {
            throw new IllegalArgumentException("jobId must not be blank");
        }
        if (objectId.isBlank()) {
            throw new IllegalArgumentException("objectId must not be blank");
        }
        if (evaluatorVersion.isBlank()) {
            throw new IllegalArgumentException("evaluatorVersion must not be blank");
        }

        for (FidelityDimension dimension : FidelityDimension.values()) {
            Double score = dimensionScores.get(dimension);
            if (score == null) {
                throw new IllegalArgumentException(
                        "every fidelity dimension must be scored; missing: " + dimension);
            }
            if (score < 0.0 || score > 1.0) {
                throw new IllegalArgumentException(
                        "score for " + dimension + " must be between 0.0 and 1.0");
            }
        }

        if (criticalFactScore < 0.0 || criticalFactScore > 1.0) {
            throw new IllegalArgumentException("criticalFactScore must be between 0.0 and 1.0");
        }
        if (criticalFactsTotal < 0 || criticalFactsPreserved < 0) {
            throw new IllegalArgumentException("critical fact counts must not be negative");
        }
        if (criticalFactsPreserved > criticalFactsTotal) {
            throw new IllegalArgumentException(
                    "preserved critical facts cannot exceed the total required");
        }
        if (originalBytes < 0 || semanticMemoryBytes < 0) {
            throw new IllegalArgumentException("byte counts must not be negative");
        }

        dimensionScores = Map.copyOf(dimensionScores);
        findings = List.copyOf(Objects.requireNonNull(findings, "findings must not be null"));
    }

    public double scoreFor(FidelityDimension dimension) {
        return dimensionScores.get(dimension);
    }

    public int percentFor(FidelityDimension dimension) {
        return (int) Math.round(scoreFor(dimension) * 100);
    }

    /** @return critical fact score as a whole percentage */
    public int criticalFactPercent() {
        return (int) Math.round(criticalFactScore * 100);
    }

    public boolean hasCriticalFactLoss() {
        return criticalFactsPreserved < criticalFactsTotal;
    }

    public boolean hasCorrectnessFailure(double threshold) {
        return dimensionScores.entrySet().stream()
                .filter(entry -> entry.getKey().isCorrectnessCritical())
                .anyMatch(entry -> entry.getValue() < threshold);
    }

    public Optional<Double> storageRatio() {
        if (originalBytes <= 0) {
            return Optional.empty();
        }
        return Optional.of((double) semanticMemoryBytes / originalBytes);
    }

    public record EvaluationFinding(FidelityDimension dimension, boolean preserved, String detail) {

        public EvaluationFinding {
            Objects.requireNonNull(dimension, "dimension must not be null");
            Objects.requireNonNull(detail, "detail must not be null");

            if (detail.isBlank()) {
                throw new IllegalArgumentException("detail must not be blank");
            }
        }
    }
}
