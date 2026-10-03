package com.sfs.evaluation;

import com.sfs.contracts.evaluation.FidelityDimension;
import com.sfs.contracts.evaluation.FidelityReportView;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record FidelityReport(
        String jobId,
        String objectId,
        SemanticScore semantic,
        StructuralScore structural,
        FactualScore factual,
        EntityScore entity,
        RelationshipScore relationship,
        CompletenessScore completeness,
        List<ErrorCategory> errors,
        List<CalibrationRecord> calibration,
        double overallFidelity,
        long originalBytes,
        long artifactBytes,
        long dnaBytes,
        String evaluatorVersion,
        Instant evaluatedAt) {

    public FidelityReport {
        Objects.requireNonNull(jobId, "jobId must not be null");
        Objects.requireNonNull(objectId, "objectId must not be null");
        Objects.requireNonNull(semantic, "semantic must not be null");
        Objects.requireNonNull(structural, "structural must not be null");
        Objects.requireNonNull(factual, "factual must not be null");
        Objects.requireNonNull(entity, "entity must not be null");
        Objects.requireNonNull(relationship, "relationship must not be null");
        Objects.requireNonNull(completeness, "completeness must not be null");
        errors = errors == null ? List.of() : List.copyOf(errors);
        calibration = calibration == null ? List.of() : List.copyOf(calibration);
        Objects.requireNonNull(evaluatorVersion, "evaluatorVersion must not be null");
        Objects.requireNonNull(evaluatedAt, "evaluatedAt must not be null");
        if (overallFidelity < 0.0 || overallFidelity > 1.0) {
            throw new IllegalArgumentException(
                    "overallFidelity must be between 0.0 and 1.0");
        }
        if (originalBytes < 0 || artifactBytes <= 0 || dnaBytes <= 0) {
            throw new IllegalArgumentException("byte counts must be positive");
        }
    }

    public Map<FidelityDimension, Double> dimensionScores() {
        Map<FidelityDimension, Double> scores = new EnumMap<>(FidelityDimension.class);
        scores.put(FidelityDimension.SEMANTIC, semantic.toDimensionScore());
        scores.put(FidelityDimension.STRUCTURAL, structural.toDimensionScore());
        scores.put(FidelityDimension.FACTUAL, factual.toDimensionScore());
        scores.put(FidelityDimension.ENTITY, entity.toDimensionScore());
        scores.put(FidelityDimension.RELATIONSHIP, relationship.toDimensionScore());
        scores.put(FidelityDimension.COMPLETENESS, completeness.toDimensionScore());
        return Map.copyOf(scores);
    }

    public FidelityReportView toView(List<FidelityReportView.EvaluationFinding> findings) {
        return new FidelityReportView(jobId, objectId, dimensionScores(),
                factual.critical().rate(), factual.critical().total(),
                factual.critical().preserved(), findings, originalBytes,
                artifactBytes, evaluatorVersion, evaluatedAt);
    }

    public double knowledgePreservationDensity() {
        return (double) dnaBytes / artifactBytes;
    }
}
