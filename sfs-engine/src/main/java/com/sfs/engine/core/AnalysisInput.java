package com.sfs.engine.core;

import java.util.Arrays;
import java.util.Objects;

public record AnalysisInput(
        String objectId,
        String fileName,
        String contentType,
        byte[] content) {

    public AnalysisInput {
        Objects.requireNonNull(objectId, "objectId must not be null");
        if (objectId.isBlank()) {
            throw new IllegalArgumentException("objectId must not be blank");
        }
        Objects.requireNonNull(fileName, "fileName must not be null");
        if (fileName.isBlank()) {
            throw new IllegalArgumentException("fileName must not be blank");
        }
        Objects.requireNonNull(content, "content must not be null");
        content = Arrays.copyOf(content, content.length);
    }

    public byte[] content() {
        return Arrays.copyOf(content, content.length);
    }
}
