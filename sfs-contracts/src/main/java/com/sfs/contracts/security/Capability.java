package com.sfs.contracts.security;

public enum Capability {

    READ("Read"),

    WRITE("Write"),

    DELETE_RAW("Delete raw data (reversible)"),

    UNDO_DELETE("Undo deletion"),

    MEMORIZE("Memorize (commit semantic memory)"),

    PURGE_RAW("Purge raw data (permanent)");

    private final String label;

    Capability(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean isIrreversible() {
        return this == PURGE_RAW;
    }
}
