package com.sfs.app.api.response;

import com.sfs.contracts.file.FileOperationResult;

import java.util.Objects;

public record OperationResponse(boolean successful, String objectId, String message) {

    public OperationResponse {
        Objects.requireNonNull(message, "message must not be null");
    }

    public static OperationResponse from(FileOperationResult result) {
        Objects.requireNonNull(result, "result must not be null");

        return new OperationResponse(result.successful(), result.objectId(), result.message());
    }
}
