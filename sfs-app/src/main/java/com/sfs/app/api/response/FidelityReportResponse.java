package com.sfs.app.api.response;

import com.sfs.contracts.evaluation.EvaluationAvailability;
import com.sfs.contracts.evaluation.FidelityDimension;
import com.sfs.contracts.evaluation.FidelityReportView;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record FidelityReportResponse(
        String jobId,
        String objectId,
        String availability,
        String availabilityLabel,
        String reason,
        List<DimensionScore> dimensions,
        Integer criticalFactsPreserved,
        Integer criticalFactsTotal,
        List<FindingResponse> findings,
        Long originalBytes,
        Long semanticMemoryBytes,
        String evaluator,
        Instant evaluatedAt) {

    public FidelityReportResponse {
        Objects.requireNonNull(availability, "availability must not be null");
        dimensions = dimensions == null ? List.of() : List.copyOf(dimensions);
        findings = findings == null ? List.of() : List.copyOf(findings);
    }

    public static FidelityReportResponse from(String jobId, EvaluationAvailability availability) {
        Objects.requireNonNull(availability, "availability must not be null");

        if (availability.status() != EvaluationAvailability.Status.AVAILABLE) {
            return new FidelityReportResponse(
                    jobId,
                    null,
                    availability.status().name(),
                    availability.status().getLabel(),
                    availability.reason(),
                    List.of(),
                    null,
                    null,
                    List.of(),
                    null,
                    null,
                    null,
                    null);
        }

        FidelityReportView report = availability.report();

        List<DimensionScore> dimensions = new ArrayList<>();
        for (FidelityDimension dimension : FidelityDimension.values()) {
            dimensions.add(new DimensionScore(
                    dimension.name(),
                    dimension.getLabel(),
                    report.dimensionScores().get(dimension),
                    dimension.isCorrectnessCritical()));
        }

        return new FidelityReportResponse(
                report.jobId(),
                report.objectId(),
                availability.status().name(),
                availability.status().getLabel(),
                null,
                dimensions,
                report.criticalFactsPreserved(),
                report.criticalFactsTotal(),
                report.findings().stream().map(FindingResponse::from).toList(),
                report.originalBytes(),
                report.semanticMemoryBytes(),
                report.evaluatorVersion(),
                report.evaluatedAt());
    }

    public record DimensionScore(
            String dimension,
            String label,
            Double score,
            boolean correctnessCritical) {

        public DimensionScore {
            Objects.requireNonNull(dimension, "dimension must not be null");

            if (score != null && (score < 0.0 || score > 1.0)) {
                throw new IllegalArgumentException("score must be between 0.0 and 1.0");
            }
        }
    }

    public record FindingResponse(String dimension, boolean preserved, String detail) {

        public static FindingResponse from(FidelityReportView.EvaluationFinding finding) {
            return new FindingResponse(
                    finding.dimension().name(), finding.preserved(), finding.detail());
        }
    }
}
