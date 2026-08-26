package com.sfs.app.api.response;

import com.sfs.contracts.file.FileSummary;

import java.time.Instant;
import java.util.Objects;

public record FileResponse(
        String objectId,
        String fileName,
        String status,
        String statusLabel,
        long sizeBytes,
        Instant registeredAt,
        Instant analyzedAt,
        boolean rawDataRemoved,
        boolean allowsAnalysis,
        boolean allowsSoftDeletion,
        boolean allowsUndoDelete,
        boolean allowsPurge) {

    public FileResponse {
        Objects.requireNonNull(objectId, "objectId must not be null");
        Objects.requireNonNull(fileName, "fileName must not be null");
        Objects.requireNonNull(status, "status must not be null");
    }

    public static FileResponse from(FileSummary summary) {
        Objects.requireNonNull(summary, "summary must not be null");

        return new FileResponse(
                summary.objectId(),
                summary.displayName(),
                summary.status().name(),
                summary.status().getLabel(),
                summary.sizeBytes(),
                summary.registeredAt(),
                summary.analyzedAt(),
                summary.status().isRawDataRemoved(),
                summary.status().allowsAnalysis(),
                summary.status().allowsSoftDeletion(),
                summary.status().allowsUndoDelete(),
                summary.status().allowsPurge());
    }
}
