package com.sfs.ui.api;

import com.sfs.app.api.error.ApiErrorCode;
import com.sfs.app.api.error.ApiErrorResponse;
import com.sfs.app.service.ApplicationException;
import com.sfs.core.identity.ObjectId;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

@RestControllerAdvice(basePackages = "com.sfs.ui.api")
public class ApiExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ApiErrorResponse> handleApplicationException(
            ApplicationException e, HttpServletRequest request) {

        return respond(e.errorCode(), e.getMessage(), request);
    }

    @ExceptionHandler(ObjectId.InvalidObjectIdException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidObjectId(
            ObjectId.InvalidObjectIdException e, HttpServletRequest request) {

        return respond(ApiErrorCode.OBJECT_ID_INVALID, e.getMessage(), request);
    }

    @ExceptionHandler(ApiExceptions.PayloadTooLarge.class)
    public ResponseEntity<ApiErrorResponse> handlePayloadTooLarge(
            ApiExceptions.PayloadTooLarge e, HttpServletRequest request) {

        return respond(ApiErrorCode.PAYLOAD_TOO_LARGE, e.getMessage(), request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleMaxUploadSize(
            MaxUploadSizeExceededException e, HttpServletRequest request) {

        return respond(ApiErrorCode.PAYLOAD_TOO_LARGE,
                "The uploaded file exceeds the permitted size.", request);
    }

    @ExceptionHandler(ApiExceptions.UnsupportedPayload.class)
    public ResponseEntity<ApiErrorResponse> handleUnsupportedPayload(
            ApiExceptions.UnsupportedPayload e, HttpServletRequest request) {

        return respond(ApiErrorCode.UNSUPPORTED_MEDIA_TYPE, e.getMessage(), request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException e, HttpServletRequest request) {

        return respond(ApiErrorCode.UNSUPPORTED_MEDIA_TYPE,
                "The request content type is not supported by this operation.", request);
    }

    @ExceptionHandler(ApiExceptions.PayloadRejected.class)
    public ResponseEntity<ApiErrorResponse> handlePayloadRejected(
            ApiExceptions.PayloadRejected e, HttpServletRequest request) {

        return respond(ApiErrorCode.VALIDATION_FAILED, e.getMessage(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
            IllegalArgumentException e, HttpServletRequest request) {

        return respond(ApiErrorCode.VALIDATION_FAILED, safeMessage(e), request);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class})
    public ResponseEntity<ApiErrorResponse> handleMalformedRequest(
            Exception e, HttpServletRequest request) {

        return respond(ApiErrorCode.REQUEST_MALFORMED,
                "The request body or parameters could not be read.", request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException e, HttpServletRequest request) {

        return respond(ApiErrorCode.METHOD_NOT_ALLOWED,
                "That method is not allowed for this operation.", request);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoHandler(
            NoHandlerFoundException e, HttpServletRequest request) {

        return respond(ApiErrorCode.FILE_NOT_FOUND, "No such API operation.", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(
            Exception e, HttpServletRequest request) {

        LOG.error("Unhandled API failure for {} {}",
                request.getMethod(), redactedPath(request), e);

        return respond(ApiErrorCode.INTERNAL_ERROR,
                "The request failed. The failure has been logged.", request);
    }

    private ResponseEntity<ApiErrorResponse> respond(
            ApiErrorCode code, String message, HttpServletRequest request) {

        String safeMessage = (message == null || message.isBlank())
                ? "The request could not be completed."
                : message;

        if (code.isClientError()) {
            LOG.warn("API request refused: {} {} -> {}",
                    request.getMethod(), redactedPath(request), code);
        }

        return ResponseEntity
                .status(HttpStatus.valueOf(code.status()))
                .body(ApiErrorResponse.of(code, safeMessage, redactedPath(request)));
    }

    private String redactedPath(HttpServletRequest request) {
        String path = request.getRequestURI();

        if (path != null && path.endsWith("/search")) {
            return path;
        }

        return path;
    }

    private String safeMessage(IllegalArgumentException e) {
        String message = e.getMessage();

        if (message == null || message.isBlank()) {
            return "The request contained an invalid value.";
        }

        return message;
    }
}
