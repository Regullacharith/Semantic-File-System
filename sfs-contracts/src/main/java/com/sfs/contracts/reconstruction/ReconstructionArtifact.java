package com.sfs.contracts.reconstruction;

import java.util.Objects;

/**
 * A generated reconstruction artifact for download.
 */
public record ReconstructionArtifact(
        String jobId,
        String fileName,
        String content,
        String contentType) {

    public static final String TEXT_PLAIN = "text/plain; charset=UTF-8";

    public ReconstructionArtifact {
        Objects.requireNonNull(jobId, "jobId must not be null");
        Objects.requireNonNull(fileName, "fileName must not be null");
        Objects.requireNonNull(content, "content must not be null");
        Objects.requireNonNull(contentType, "contentType must not be null");

        if (jobId.isBlank()) {
            throw new IllegalArgumentException("jobId must not be blank");
        }
        if (fileName.isBlank()) {
            throw new IllegalArgumentException("fileName must not be blank");
        }
        if (contentType.isBlank()) {
            throw new IllegalArgumentException("contentType must not be blank");
        }
    }

    public static String provenanceHeader(String objectId,
                                          String dnaVersion,
                                          String rulesVersion,
                                          String modelVersion) {
        return """
                =============================================================
                SFS SEMANTIC RECONSTRUCTION - NOT THE ORIGINAL FILE
                =============================================================
                This document was regenerated from semantic memory. It is a
                new, semantically equivalent artifact. It is NOT a byte-for-byte
                recovery of the original file, and exact wording will differ.

                Object ID      : %s
                Semantic DNA   : %s
                Reconstruction : %s
                Model          : %s
                =============================================================

                """.formatted(objectId, dnaVersion, rulesVersion, modelVersion);
    }
}
