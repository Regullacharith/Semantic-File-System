package com.sfs.contracts.file;

/**
 * Lifecycle status of a file as presented to the user interface.
 */
public enum FileStatus {

    /** Registered and known to the system, but not yet analyzed. */
    REGISTERED("Registered", "Known to the system; semantic analysis has not run yet.", false),

    /** Semantic analysis is currently running. */
    ANALYZING("Analyzing", "Semantic analysis is in progress.", false),

    /** Analyzed, with a validated Semantic DNA, raw bytes still present. */
    ANALYZED("Analyzed", "Semantic DNA has been produced and validated.", false),

    /**
     * Semantic Record durably committed and raw bytes removed.
     */
    MEMORIZED("Memorized", "Raw bytes removed; the Semantic Record survives and is searchable.", true),

    /** Analysis failed; the file has no usable Semantic DNA. */
    FAILED("Failed", "Semantic analysis failed; no usable Semantic DNA exists.", false);

    private final String label;
    private final String description;
    private final boolean rawDataRemoved;

    FileStatus(String label, String description, boolean rawDataRemoved) {
        this.label = label;
        this.description = description;
        this.rawDataRemoved = rawDataRemoved;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }


    public boolean isRawDataRemoved() {
        return rawDataRemoved;
    }

    public boolean allowsSemanticDeletion() {
        return this == ANALYZED;
    }

    public boolean allowsAnalysis() {
        return this == REGISTERED || this == FAILED;
    }
}
