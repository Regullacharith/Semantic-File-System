package com.sfs.reconstruction.bench;

public record BenchmarkRun(
        String modelId,
        String objectId,
        boolean satisfied,
        int violationCount,
        int warningCount,
        double criticalCoverage,
        double entityCoverage,
        double relationshipCoverage,
        boolean summaryVerbatim,
        long reconstructNanos,
        long verifyNanos,
        int dnaBytes,
        int draftBytes) {

    public BenchmarkRun {
        if (modelId == null || modelId.isBlank()) {
            throw new IllegalArgumentException("modelId must not be blank");
        }
        if (violationCount < 0 || warningCount < 0) {
            throw new IllegalArgumentException("counts must not be negative");
        }
        if (criticalCoverage < 0.0 || criticalCoverage > 1.0
                || entityCoverage < 0.0 || entityCoverage > 1.0
                || relationshipCoverage < 0.0 || relationshipCoverage > 1.0) {
            throw new IllegalArgumentException("coverage values must be within [0, 1]");
        }
        if (reconstructNanos < 0 || verifyNanos < 0) {
            throw new IllegalArgumentException("nanos must not be negative");
        }
        if (dnaBytes <= 0) {
            throw new IllegalArgumentException("dnaBytes must be positive");
        }
        if (draftBytes < 0) {
            throw new IllegalArgumentException("draftBytes must not be negative");
        }
    }

    public double storageRatio() {
        return (double) draftBytes / dnaBytes;
    }

    public double knowledgePreservationDensity() {
        return (double) dnaBytes / draftBytes;
    }
}
