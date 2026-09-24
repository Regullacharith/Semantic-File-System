package com.sfs.engine.pipeline;

import com.sfs.contracts.semantic.SemanticDnaView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record PipelineResult(
        boolean success,
        SemanticDnaView dna,
        Map<String, Long> stageDurationsMs,
        String failedStage,
        String failureReason,
        List<String> validationIssues) {

    public PipelineResult {
        Objects.requireNonNull(stageDurationsMs, "stageDurationsMs must not be null");
        stageDurationsMs = Map.copyOf(stageDurationsMs);
        validationIssues = validationIssues == null
                ? List.of()
                : List.copyOf(validationIssues);
    }

    public static PipelineResult success(SemanticDnaView dna, Map<String, Long> stageDurationsMs) {
        Objects.requireNonNull(dna, "dna must not be null");
        return new PipelineResult(true, dna, stageDurationsMs, null, null, List.of());
    }

    public static PipelineResult failure(String failedStage, String failureReason,
                                         Map<String, Long> stageDurationsMs) {
        Objects.requireNonNull(failedStage, "failedStage must not be null");
        Objects.requireNonNull(failureReason, "failureReason must not be null");
        return new PipelineResult(false, null, stageDurationsMs, failedStage, failureReason,
                new ArrayList<>());
    }

    public static PipelineResult invalid(List<String> issues, Map<String, Long> stageDurationsMs) {
        Objects.requireNonNull(issues, "issues must not be null");
        if (issues.isEmpty()) {
            throw new IllegalArgumentException("an invalid result needs at least one issue");
        }
        return new PipelineResult(false, null, stageDurationsMs, "dna-validation",
                "The produced Semantic DNA is incomplete: " + String.join("; ", issues),
                issues);
    }
}
