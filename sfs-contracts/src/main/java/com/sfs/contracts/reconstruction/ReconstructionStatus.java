package com.sfs.contracts.reconstruction;

/**
 * Lifecycle state of a reconstruction job.
 */
public enum ReconstructionStatus {

    QUEUED("Queued", "Accepted and waiting to start.", false, false),

    RUNNING("Running", "Reconstruction is in progress.", false, false),

    COMPLETED("Completed", "A reconstructed artifact was produced.", true, true),

    REJECTED("Rejected", "Output was produced but failed constraint verification.", true, false),

    FAILED("Failed", "Reconstruction could not be completed.", true, false);

    private final String label;
    private final String description;
    private final boolean terminal;
    private final boolean artifactAvailable;

    ReconstructionStatus(String label, String description,
                         boolean terminal, boolean artifactAvailable) {
        this.label = label;
        this.description = description;
        this.terminal = terminal;
        this.artifactAvailable = artifactAvailable;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public boolean isTerminal() {
        return terminal;
    }

    public boolean isArtifactAvailable() {
        return artifactAvailable;
    }
}
