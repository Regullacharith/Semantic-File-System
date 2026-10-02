package com.sfs.reconstruction.engine;

import com.sfs.contracts.reconstruction.ReconstructionArtifact;

import java.util.Objects;

public final class ReconstructionArtifactFactory {

    public ReconstructionArtifact create(String jobId, String sourceName,
                                         String labeledContent) {
        Objects.requireNonNull(jobId, "jobId must not be null");
        Objects.requireNonNull(sourceName, "sourceName must not be null");
        Objects.requireNonNull(labeledContent, "labeledContent must not be null");
        if (labeledContent.isBlank()) {
            throw new IllegalArgumentException("labeledContent must not be blank");
        }
        return new ReconstructionArtifact(jobId, artifactName(sourceName, jobId),
                labeledContent, ReconstructionArtifact.TEXT_PLAIN);
    }

    public static String artifactName(String sourceName, String jobId) {
        String base = sourceName.endsWith(".txt")
                ? sourceName.substring(0, sourceName.length() - 4)
                : sourceName;
        return base + ".reconstructed." + jobId + ".txt";
    }
}
