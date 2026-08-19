package com.sfs.contracts.reconstruction;

import java.util.List;
import java.util.Optional;

/**
 * Application-facing contract for semantic reconstruction.
*/
public interface ReconstructionService {

    ReconstructionJobView requestReconstruction(String objectId);

    Optional<ReconstructionJobView> findJob(String jobId);

    List<ReconstructionJobView> listJobs();

    Optional<ReconstructionArtifact> findArtifact(String jobId);
}
