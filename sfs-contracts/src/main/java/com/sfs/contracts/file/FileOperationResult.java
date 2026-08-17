package com.sfs.contracts.file;

import java.util.Objects;

/**
 * Outcome of a file lifecycle operation.
 * @param successful whether the operation was accepted
 * @param objectId   the affected Object ID, or null when import validation failed before one was assigned
 * @param message    human-readable outcome, safe to display, never blank
 */
public record FileOperationResult(
        boolean successful,
        String objectId,
        String message) {

    /**
     * Canonical constructor.
     */
    public FileOperationResult {
        Objects.requireNonNull(message, "message must not be null");

        if (message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
    }

    public static FileOperationResult success(String objectId, String message) {
        Objects.requireNonNull(objectId, "objectId must not be null for a successful result");
        return new FileOperationResult(true, objectId, message);
    }

    /**
     * Creates a failed outcome.
     */
    public static FileOperationResult failure(String message) {
        return new FileOperationResult(false, null, message);
    }
}
