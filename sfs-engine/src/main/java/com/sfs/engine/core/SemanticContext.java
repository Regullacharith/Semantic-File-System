package com.sfs.engine.core;

import com.sfs.engine.level.AnalysisLevel;

import java.time.Instant;
import java.util.Objects;

public record SemanticContext(
        String objectId,
        String content,
        String contentSha256,
        AnalysisLevel level,
        String engineVersion,
        int dnaVersion,
        Instant submittedAt) {

    public SemanticContext {
        Objects.requireNonNull(objectId, "objectId must not be null");
        Objects.requireNonNull(content, "content must not be null");
        Objects.requireNonNull(contentSha256, "contentSha256 must not be null");
        Objects.requireNonNull(level, "level must not be null");
        Objects.requireNonNull(engineVersion, "engineVersion must not be null");
        if (dnaVersion < 1) {
            throw new IllegalArgumentException("dnaVersion must be at least 1");
        }
        Objects.requireNonNull(submittedAt, "submittedAt must not be null");
    }
}
