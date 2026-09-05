package com.sfs.lifecycle.state;

import java.util.Set;

public enum FileState {

    REGISTERED,
    ANALYZING,
    ANALYZED,
    MEMORIZABLE,
    MEMORY_COMMITTED,
    SOFT_DELETED,
    MEMORIZED,
    FAILED;

    public static final Set<FileState> TERMINAL_STATES = Set.of(MEMORIZED);

    public boolean isTerminal() {
        return TERMINAL_STATES.contains(this);
    }

    public boolean isLive() {
        return this == REGISTERED || this == ANALYZING || this == ANALYZED
                || this == MEMORIZABLE || this == MEMORY_COMMITTED;
    }

    public boolean hasCommittedMemory() {
        return this == MEMORY_COMMITTED || this == SOFT_DELETED || this == MEMORIZED;
    }

    public boolean rawDataPresent() {
        return this != MEMORIZED;
    }
}
