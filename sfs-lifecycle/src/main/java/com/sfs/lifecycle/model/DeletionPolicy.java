package com.sfs.lifecycle.model;

public record DeletionPolicy(
        boolean requireSoftDeletedState,
        boolean requireMemoryCommit) {

    public static DeletionPolicy v1() {
        return new DeletionPolicy(true, true);
    }

    public DeletionPolicy {
        if (!requireSoftDeletedState && requireMemoryCommit) {
            throw new IllegalArgumentException(
                    "memory-commit enforcement presupposes the soft-deleted state");
        }
    }
}
