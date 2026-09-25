package com.sfs.adapters.spi;

import java.util.Arrays;
import java.util.Objects;

public record AdapterRequest(
        String objectId,
        String fileName,
        String contentType,
        byte[] content) {

    public AdapterRequest {
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

    public String extension() {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1).toLowerCase(java.util.Locale.ROOT);
    }

    public byte[] content() {
        return Arrays.copyOf(content, content.length);
    }
}
