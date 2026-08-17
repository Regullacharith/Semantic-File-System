package com.sfs.contracts.search;

import com.sfs.contracts.file.FileStatus;

import java.util.List;
import java.util.Objects;

/**
 * One matching semantic record.
 */
public record SearchResult(
        String objectId,
        String displayName,
        FileStatus status,
        double relevance,
        String summary,
        List<SearchEvidence> evidence) {

    /**
     * Canonical constructor.
     */
    public SearchResult {
        Objects.requireNonNull(objectId, "objectId must not be null");
        Objects.requireNonNull(displayName, "displayName must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(summary, "summary must not be null");
        Objects.requireNonNull(evidence, "evidence must not be null");

        if (objectId.isBlank()) {
            throw new IllegalArgumentException("objectId must not be blank");
        }
        if (displayName.isBlank()) {
            throw new IllegalArgumentException("displayName must not be blank");
        }
        if (summary.isBlank()) {
            throw new IllegalArgumentException("summary must not be blank");
        }
        if (relevance < 0.0 || relevance > 1.0) {
            throw new IllegalArgumentException("relevance must be between 0.0 and 1.0");
        }

        // A result  evidence 
        if (evidence.isEmpty()) {
            throw new IllegalArgumentException("a result must carry at least one piece of evidence");
        }

        evidence = List.copyOf(evidence);
    }

    public boolean isMemorized() {
        return status.isRawDataRemoved();
    }
    
    /**
     * Relevance expressed as a whole percentage, for display.
     */
    public int relevancePercent() {
        return (int) Math.round(relevance * 100);
    }
}
