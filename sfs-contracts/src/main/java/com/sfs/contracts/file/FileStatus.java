package com.sfs.contracts.file;

public enum FileStatus {

    REGISTERED("Registered", "Known to the system; semantic analysis has not run yet.", false),

    ANALYZING("Analyzing", "Semantic analysis is in progress.", false),

    ANALYZED("Analyzed", "Semantic DNA has been produced and validated.", false),

   
    SOFT_DELETED("Deleted (recoverable)",
            "Withdrawn from normal use. Raw bytes are retained and the object can be restored.",
            false),

    
    MEMORIZED("Memorized", "Raw bytes removed; the Semantic Record survives and is searchable.", true),

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

    public boolean allowsSoftDeletion() {
        return this == ANALYZED;
    }

    public boolean allowsUndoDelete() {
        return this == SOFT_DELETED;
    }

    public boolean allowsPurge() {
        return this == SOFT_DELETED;
    }

    public boolean allowsAnalysis() {
        return this == REGISTERED || this == FAILED;
    }
}
