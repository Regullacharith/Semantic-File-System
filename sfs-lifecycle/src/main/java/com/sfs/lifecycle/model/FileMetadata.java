package com.sfs.lifecycle.model;

import java.time.Instant;
import java.util.Objects;
import java.util.regex.Pattern;

public record FileMetadata(
        String fileName,
        String contentType,
        long sizeBytes,
        String sha256,
        String storageAddress,
        Instant registeredAt,
        Instant lastModifiedAt) {

    public static final int MAX_FILE_NAME_LENGTH = 255;
    public static final int MAX_STORAGE_ADDRESS_LENGTH = 512;

    private static final Pattern SHA256_PATTERN = Pattern.compile("^[0-9a-f]{64}$");

    public FileMetadata {
        fileName = requireFileName(fileName);
        Objects.requireNonNull(contentType, "contentType must not be null");
        if (contentType.isBlank()) {
            throw new IllegalArgumentException("contentType must not be blank");
        }
        if (sizeBytes < 0) {
            throw new IllegalArgumentException("sizeBytes must not be negative");
        }
        Objects.requireNonNull(sha256, "sha256 must not be null");
        if (!SHA256_PATTERN.matcher(sha256).matches()) {
            throw new IllegalArgumentException("sha256 must be 64 lowercase hexadecimal characters");
        }
        if (storageAddress != null) {
            if (storageAddress.isBlank()) {
                throw new IllegalArgumentException("storageAddress must not be blank when present");
            }
            if (storageAddress.length() > MAX_STORAGE_ADDRESS_LENGTH) {
                throw new IllegalArgumentException(
                        "storageAddress must not exceed " + MAX_STORAGE_ADDRESS_LENGTH + " characters");
            }
        }
        Objects.requireNonNull(registeredAt, "registeredAt must not be null");
        Objects.requireNonNull(lastModifiedAt, "lastModifiedAt must not be null");
        if (lastModifiedAt.isBefore(registeredAt)) {
            throw new IllegalArgumentException("lastModifiedAt must not be before registeredAt");
        }
    }

    private static String requireFileName(String candidate) {
        Objects.requireNonNull(candidate, "fileName must not be null");
        if (candidate.isBlank()) {
            throw new IllegalArgumentException("fileName must not be blank");
        }
        if (candidate.length() > MAX_FILE_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "fileName must not exceed " + MAX_FILE_NAME_LENGTH + " characters");
        }
        if (candidate.contains("/") || candidate.contains("\\") || candidate.contains("..")) {
            throw new IllegalArgumentException(
                    "fileName must not contain a path separator or parent reference");
        }
        return candidate;
    }

    public FileMetadata withFileName(String newFileName, Instant at) {
        return new FileMetadata(requireFileName(newFileName), contentType, sizeBytes,
                sha256, storageAddress, registeredAt, at);
    }

    public FileMetadata withoutStorageAddress(Instant at) {
        return new FileMetadata(fileName, contentType, sizeBytes, sha256,
                null, registeredAt, at);
    }
}
