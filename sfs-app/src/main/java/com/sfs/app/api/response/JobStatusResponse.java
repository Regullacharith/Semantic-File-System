package com.sfs.app.api.response;

import com.sfs.contracts.reconstruction.ReconstructionJobView;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record JobStatusResponse(
        String jobId,
        String objectId,
        String sourceName,
        String status,
        String statusLabel,
        boolean terminal,
        boolean refused,
        Instant requestedAt,
        Instant completedAt,
        Provenance provenance,
        boolean hasArtifact,
        String artifactName,
        long artifactBytes,
        List<FindingResponse> findings,
        String failureReason) {

    public JobStatusResponse {
        Objects.requireNonNull(jobId, "jobId must not be null");
        Objects.requireNonNull(status, "status must not be null");
        findings = findings == null ? List.of() : List.copyOf(findings);
    }

    public static JobStatusResponse from(ReconstructionJobView job) {
        Objects.requireNonNull(job, "job must not be null");

        boolean refused = job.status() == com.sfs.contracts.reconstruction.ReconstructionStatus.REJECTED;

        return new JobStatusResponse(
                job.jobId(),
                job.objectId(),
                job.sourceName(),
                job.status().name(),
                job.status().getLabel(),
                job.isTerminal(),
                refused,
                job.requestedAt(),
                job.completedAt(),
                new Provenance(job.dnaVersion(), job.rulesVersion(), job.modelVersion()),
                job.hasArtifact(),
                job.hasArtifact() ? job.artifactName() : null,
                job.hasArtifact() ? job.artifactBytes() : 0L,
                job.constraintFindings().stream().map(FindingResponse::from).toList(),
                job.failureReason());
    }

    public record Provenance(String dnaVersion, String rulesVersion, String modelVersion) {

        public Provenance {
            Objects.requireNonNull(dnaVersion, "dnaVersion must not be null");
            Objects.requireNonNull(rulesVersion, "rulesVersion must not be null");
            Objects.requireNonNull(modelVersion, "modelVersion must not be null");
        }
    }

    public record FindingResponse(String severity, String constraint, String detail) {

        public static FindingResponse from(ReconstructionJobView.ConstraintFinding finding) {
            return new FindingResponse(
                    finding.severity().name(), finding.constraint(), finding.detail());
        }
    }
}
