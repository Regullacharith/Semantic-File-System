package com.sfs.contracts.file;

import java.util.Objects;

/**
 * Request to import a text file into the Semantic File System.
 * @param fileName    original file name, never blank
 * @param content     decoded text content, never null but may be empty
 * @param contentType declared content type, or null if unknown
 */
public record FileImportRequest(
        String fileName,
        String content,
        String contentType) {

    /** Maximum accepted file name length, guarding against pathological input. */
    public static final int MAX_FILE_NAME_LENGTH = 255;

    /**
     * Canonical constructor.
     */
    public FileImportRequest {
        Objects.requireNonNull(fileName, "fileName must not be null");
        Objects.requireNonNull(content, "content must not be null");

        if (fileName.isBlank()) {
            throw new IllegalArgumentException("fileName must not be blank");
        }
        if (fileName.length() > MAX_FILE_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "fileName must not exceed " + MAX_FILE_NAME_LENGTH + " characters");
        }

        // Reject path separators and parent-directory references. A file name is a name,
        // not a path; accepting one would invite path traversal once a real storage
        if (fileName.contains("/") || fileName.contains("\\") || fileName.contains("..")) {
            throw new IllegalArgumentException(
                    "fileName must not contain a path separator or parent reference");
        }
    }

    /**
     * Size of the supplied content in characters.
     */
    public int contentLength() {
        return content.length();
    }
}
