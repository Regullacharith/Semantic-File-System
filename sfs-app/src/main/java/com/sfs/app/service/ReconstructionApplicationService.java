package com.sfs.app.service;

import com.sfs.app.api.request.ReconstructionApiRequest;
import com.sfs.app.api.response.JobStatusResponse;
import com.sfs.contracts.reconstruction.ReconstructionArtifact;
import com.sfs.contracts.reconstruction.ReconstructionJobView;
import com.sfs.contracts.reconstruction.ReconstructionService;
import com.sfs.core.identity.ObjectId;

import java.util.List;
import java.util.Objects;

public class ReconstructionApplicationService {

    private final ReconstructionService reconstructionService;
    private final JobRegistry jobRegistry;

    public ReconstructionApplicationService(ReconstructionService reconstructionService) {
        this(reconstructionService, new JobRegistry());
    }

    public ReconstructionApplicationService(ReconstructionService reconstructionService,
                                            JobRegistry jobRegistry) {
        this.reconstructionService =
                Objects.requireNonNull(reconstructionService, "reconstructionService must not be null");
        this.jobRegistry = Objects.requireNonNull(jobRegistry, "jobRegistry must not be null");
    }

    public JobRegistry jobRegistry() {
        return jobRegistry;
    }

    public JobStatusResponse startReconstruction(ReconstructionApiRequest request) {
        Objects.requireNonNull(request, "request must not be null");

        String objectId = ObjectId.of(request.objectId()).value();

        ReconstructionJobView job;
        try {
            job = reconstructionService.requestReconstruction(objectId);
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw ApplicationException.invalidState(e.getMessage());
        }

        jobRegistry.record(job);

        return JobStatusResponse.from(job);
    }

    public List<JobStatusResponse> listJobs() {
        return reconstructionService.listJobs().stream().map(JobStatusResponse::from).toList();
    }

    public JobStatusResponse getJob(String jobId) {
        return JobStatusResponse.from(requireJob(jobId));
    }

    public ReconstructionArtifact getArtifact(String jobId) {
        ReconstructionJobView job = requireJob(jobId);

        return reconstructionService.findArtifact(job.jobId())
                .orElseThrow(ApplicationException::artifactNotAvailable);
    }

    private ReconstructionJobView requireJob(String jobId) {
        String validated = JobId.validate(jobId);

        return reconstructionService.findJob(validated)
                .orElseThrow(ApplicationException::jobNotFound);
    }
}
