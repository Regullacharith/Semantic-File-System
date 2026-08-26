package com.sfs.app.service;

import com.sfs.contracts.reconstruction.ReconstructionJobView;
import com.sfs.contracts.reconstruction.ReconstructionStatus;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class JobRegistry {

    public record JobRecord(
            String jobId,
            String objectId,
            String jobType,
            String status,
            Instant submittedAt,
            Instant completedAt,
            String terminationReason) {

        public JobRecord {
            Objects.requireNonNull(jobId, "jobId must not be null");
            Objects.requireNonNull(status, "status must not be null");
            Objects.requireNonNull(submittedAt, "submittedAt must not be null");
        }

        public boolean isTerminal() {
            return "COMPLETED".equals(status)
                    || "REJECTED".equals(status)
                    || "FAILED".equals(status);
        }
    }

    public static final String TYPE_RECONSTRUCTION = "RECONSTRUCTION";

    private static final String ORPHANED_REASON =
            "The job did not reach a terminal state before the application stopped. "
                    + "Job state is held in memory in V1 and does not survive a restart; "
                    + "durable job storage arrives with Milestone 08.";

    private final ConcurrentMap<String, JobRecord> jobs = new ConcurrentHashMap<>();

    public JobRecord record(ReconstructionJobView job) {
        Objects.requireNonNull(job, "job must not be null");

        JobRecord record = new JobRecord(
                job.jobId(),
                job.objectId(),
                TYPE_RECONSTRUCTION,
                job.status().name(),
                job.requestedAt(),
                job.completedAt(),
                job.status() == ReconstructionStatus.REJECTED ? job.failureReason() : null);

        jobs.put(job.jobId(), record);

        return record;
    }

    public Optional<JobRecord> find(String jobId) {
        return Optional.ofNullable(jobs.get(jobId));
    }

    public List<JobRecord> list() {
        return List.copyOf(jobs.values());
    }

    public int failOrphanedJobs() {
        int failed = 0;

        for (JobRecord record : List.copyOf(jobs.values())) {
            if (!record.isTerminal()) {
                jobs.put(record.jobId(), new JobRecord(
                        record.jobId(),
                        record.objectId(),
                        record.jobType(),
                        "FAILED",
                        record.submittedAt(),
                        Instant.now(),
                        ORPHANED_REASON));
                failed++;
            }
        }

        return failed;
    }

    public void clear() {
        jobs.clear();
    }
}
