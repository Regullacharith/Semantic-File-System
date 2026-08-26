package com.sfs.app.api.error;

public enum ApiErrorCode {

    VALIDATION_FAILED(400),
    CONFIRMATION_REQUIRED(400),
    CONFIRMATION_MISMATCH(400),
    AUTHENTICATION_REQUIRED(401),
    NOT_PERMITTED(403),
    OBJECT_ID_INVALID(400),
    JOB_ID_INVALID(400),
    REQUEST_MALFORMED(400),
    FILE_NOT_FOUND(404),
    JOB_NOT_FOUND(404),
    EVALUATION_NOT_FOUND(404),
    ARTIFACT_NOT_AVAILABLE(404),
    METHOD_NOT_ALLOWED(405),
    INVALID_STATE_TRANSITION(409),
    PAYLOAD_TOO_LARGE(413),
    UNSUPPORTED_MEDIA_TYPE(415),
    RECONSTRUCTION_REFUSED(422),
    INTERNAL_ERROR(500);

    private final int status;

    ApiErrorCode(int status) {
        this.status = status;
    }

    public int status() {
        return status;
    }

    public boolean isClientError() {
        return status >= 400 && status < 500;
    }
}
