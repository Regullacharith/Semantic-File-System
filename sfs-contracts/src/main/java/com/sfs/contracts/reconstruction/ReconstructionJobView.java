package com.sfs.contracts.reconstruction;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * A reconstruction job and its provenance.
 */
public record ReconstructionJobView(
        String jobId,
        String objectId,
        String sourceName,
        ReconstructionStatus status,
        String dnaVersion,
        String rulesVersion,
        String modelVersion,
        Instant requestedAt,
        Instant completedAt,
        String artifactName,
        long artifactBytes,
        List<ConstraintFinding> constraintFindings,
        String failureReason) {

    /**
     * Canonical constructor.
     */
    public ReconstructionJobView {
        Objects.requireNonNull(jobId, "jobId must not be null");
        Objects.requireNonNull(objectId, "objectId must not be null");
        Objects.requireNonNull(sourceName, "sourceName must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(dnaVersion, "dnaVersion must not be null");
        Objects.requireNonNull(rulesVersion, "rulesVersion must not be null");
        Objects.requireNonNull(modelVersion, "modelVersion must not be null");
        Objects.requireNonNull(requestedAt, "requestedAt must not be null");

        if (jobId.isBlank()) {
            throw new IllegalArgumentException("jobId must not be blank");
        }
        if (objectId.isBlank()) {
            throw new IllegalArgumentException("objectId must not be blank");
        }
        if (sourceName.isBlank()) {
            throw new IllegalArgumentException("sourceName must not be blank");
        }
        if (dnaVersion.isBlank() || rulesVersion.isBlank() || modelVersion.isBlank()) {
            throw new IllegalArgumentException(
                    "dnaVersion, rulesVersion and modelVersion must all be recorded");
        }
        if (artifactBytes < 0) {
            throw new IllegalArgumentException("artifactBytes must not be negative");
        }

        if (status == ReconstructionStatus.COMPLETED && (artifactName == null || artifactName.isBlank())) {
            throw new IllegalArgumentException("a completed job must name its artifact");
        }
        if (!status.isArtifactAvailable() && artifactName != null) {
            throw new IllegalArgumentException(
                    "an artifact may only be named when the job completed successfully");
        }

        constraintFindings = List.copyOf(Objects.requireNonNull(
                constraintFindings, "constraintFindings must not be null"));
    }

    public boolean isTerminal() {
        return status.isTerminal();
    }

    public boolean hasArtifact() {
        return status.isArtifactAvailable() && artifactName != null;
    }

    public boolean hasFindings() {
        return !constraintFindings.isEmpty();
    }

    public record ConstraintFinding(Severity severity, String constraint, String detail) {

        public ConstraintFinding {
            Objects.requireNonNull(severity, "severity must not be null");
            Objects.requireNonNull(constraint, "constraint must not be null");
            Objects.requireNonNull(detail, "detail must not be null");

            if (constraint.isBlank()) {
                throw new IllegalArgumentException("constraint must not be blank");
            }
            if (detail.isBlank()) {
                throw new IllegalArgumentException("detail must not be blank");
            }
        }

        public enum Severity {

            SATISFIED("Satisfied"),

            WARNING("Warning"),

            VIOLATION("Violation");

            private final String label;

            Severity(String label) {
                this.label = label;
            }

            public String getLabel() {
                return label;
            }
        }
    }
}
