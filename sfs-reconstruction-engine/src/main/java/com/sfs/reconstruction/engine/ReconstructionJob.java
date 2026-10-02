package com.sfs.reconstruction.engine;

import com.sfs.contracts.reconstruction.ReconstructionJobView;
import com.sfs.contracts.reconstruction.ReconstructionStatus;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ReconstructionJob(
        String jobId,
        String objectId,
        String sourceName,
        ReconstructionStatus status,
        String dnaVersion,
        String rulesVersion,
        String modelId,
        Instant requestedAt,
        Instant completedAt,
        String artifactName,
        long artifactBytes,
        List<ReconstructionJobView.ConstraintFinding> findings,
        String failureReason,
        VerificationResult verification,
        ReconstructionMetadata metadata) {

    private static final String PENDING_RULES_VERSION = "pending";

    public ReconstructionJob {
        Objects.requireNonNull(jobId, "jobId must not be null");
        Objects.requireNonNull(objectId, "objectId must not be null");
        Objects.requireNonNull(sourceName, "sourceName must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(dnaVersion, "dnaVersion must not be null");
        Objects.requireNonNull(rulesVersion, "rulesVersion must not be null");
        Objects.requireNonNull(modelId, "modelId must not be null");
        Objects.requireNonNull(requestedAt, "requestedAt must not be null");
        Objects.requireNonNull(findings, "findings must not be null");
        findings = List.copyOf(findings);
    }

    public static ReconstructionJob refused(String jobId, String objectId,
                                            String sourceName, Instant at,
                                            String reason) {
        return new ReconstructionJob(jobId, objectId,
                sourceName == null || sourceName.isBlank()
                        ? "unavailable" : sourceName,
                ReconstructionStatus.FAILED, "unavailable",
                PENDING_RULES_VERSION, "unavailable",
                at, at, null, 0, List.of(), reason, null, null);
    }

    public ReconstructionJob running(Instant at) {
        return new ReconstructionJob(jobId, objectId, sourceName,
                ReconstructionStatus.RUNNING, dnaVersion, rulesVersion, modelId,
                requestedAt, at, null, 0, findings, failureReason, null, null);
    }

    public ReconstructionJob withRules(String plannedRulesVersion) {
        return new ReconstructionJob(jobId, objectId, sourceName, status, dnaVersion,
                plannedRulesVersion, modelId, requestedAt, completedAt, artifactName,
                artifactBytes, findings, failureReason, verification, metadata);
    }

    public ReconstructionJob rejected(Instant at,
                                      List<ReconstructionJobView.ConstraintFinding> findings,
                                      VerificationResult verification) {
        return new ReconstructionJob(jobId, objectId, sourceName,
                ReconstructionStatus.REJECTED, dnaVersion, rulesVersion, modelId,
                requestedAt, at, null, 0, findings, null, verification, null);
    }

    public ReconstructionJob completed(Instant at, String artifactName,
                                       long artifactBytes,
                                       List<ReconstructionJobView.ConstraintFinding> findings,
                                       VerificationResult verification,
                                       ReconstructionMetadata metadata) {
        return new ReconstructionJob(jobId, objectId, sourceName,
                ReconstructionStatus.COMPLETED, dnaVersion, rulesVersion, modelId,
                requestedAt, at, artifactName, artifactBytes, findings, null,
                verification, metadata);
    }

    public ReconstructionJob failed(Instant at, String reason) {
        return new ReconstructionJob(jobId, objectId, sourceName,
                ReconstructionStatus.FAILED, dnaVersion, rulesVersion, modelId,
                requestedAt, at, null, 0, findings, reason, verification, null);
    }

    public boolean terminal() {
        return status.isTerminal();
    }

    public ReconstructionJobView toView() {
        return new ReconstructionJobView(jobId, objectId, sourceName, status,
                dnaVersion, rulesVersion, modelId, requestedAt, completedAt,
                artifactName, artifactBytes, findings, failureReason);
    }
}
