package com.sfs.app.service;

import com.sfs.app.api.error.ApiErrorCode;

import java.util.Objects;

public class ApplicationException extends RuntimeException {

    private final ApiErrorCode errorCode;

    public ApplicationException(ApiErrorCode errorCode, String message) {
        super(Objects.requireNonNull(message, "message must not be null"));
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
    }

    public ApiErrorCode errorCode() {
        return errorCode;
    }

    public static ApplicationException fileNotFound(String objectId) {
        return new ApplicationException(ApiErrorCode.FILE_NOT_FOUND,
                "No file exists with that Object ID.");
    }

    public static ApplicationException jobNotFound() {
        return new ApplicationException(ApiErrorCode.JOB_NOT_FOUND,
                "No reconstruction job exists with that job ID.");
    }

    public static ApplicationException evaluationNotFound() {
        return new ApplicationException(ApiErrorCode.EVALUATION_NOT_FOUND,
                "No evaluation exists for that reconstruction job.");
    }

    public static ApplicationException artifactNotAvailable() {
        return new ApplicationException(ApiErrorCode.ARTIFACT_NOT_AVAILABLE,
                "That job produced no artifact. Only a completed reconstruction has one.");
    }

    public static ApplicationException invalidState(String message) {
        return new ApplicationException(ApiErrorCode.INVALID_STATE_TRANSITION, message);
    }

    public static ApplicationException validationFailed(String message) {
        return new ApplicationException(ApiErrorCode.VALIDATION_FAILED, message);
    }

    public static ApplicationException authenticationRequired() {
        return new ApplicationException(ApiErrorCode.AUTHENTICATION_REQUIRED,
                "This operation requires an authenticated caller.");
    }

    public static ApplicationException notPermitted(String capability) {
        return new ApplicationException(ApiErrorCode.NOT_PERMITTED,
                "The authenticated caller does not hold the '" + capability
                        + "' capability required by this operation.");
    }

    public static ApplicationException confirmationRequired(String operation) {
        return new ApplicationException(ApiErrorCode.CONFIRMATION_REQUIRED,
                "This operation is destructive and requires explicit confirmation. "
                        + "Supply the Object ID you intend to " + operation
                        + " in the confirmation field.");
    }

    public static ApplicationException confirmationMismatch() {
        return new ApplicationException(ApiErrorCode.CONFIRMATION_MISMATCH,
                "The confirmation does not name the object being modified. "
                        + "No change has been made.");
    }
}
