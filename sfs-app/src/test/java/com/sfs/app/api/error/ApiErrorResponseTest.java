package com.sfs.app.api.error;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("API error response")
class ApiErrorResponseTest {

    @ParameterizedTest
    @EnumSource(ApiErrorCode.class)
    @DisplayName("every error code maps to a valid HTTP error status")
    void everyCodeMapsToErrorStatus(ApiErrorCode code) {
        assertThat(code.status()).isBetween(400, 599);
    }

    @ParameterizedTest
    @EnumSource(ApiErrorCode.class)
    @DisplayName("every error code produces a complete response")
    void everyCodeProducesCompleteResponse(ApiErrorCode code) {
        ApiErrorResponse response = ApiErrorResponse.of(code, "Something went wrong.", "/api/v1/x");

        assertThat(response.code()).isEqualTo(code.name());
        assertThat(response.status()).isEqualTo(code.status());
        assertThat(response.message()).isNotBlank();
        assertThat(response.timestamp()).isNotNull();
        assertThat(response.details()).isNotNull();
    }

    @Test
    @DisplayName("produces the same code and status for the same failure")
    void isDeterministic() {
        ApiErrorResponse first =
                ApiErrorResponse.of(ApiErrorCode.OBJECT_ID_INVALID, "Bad id.", "/api/v1/files/x");
        ApiErrorResponse second =
                ApiErrorResponse.of(ApiErrorCode.OBJECT_ID_INVALID, "Bad id.", "/api/v1/files/x");

        assertThat(first.code()).isEqualTo(second.code());
        assertThat(first.status()).isEqualTo(second.status());
        assertThat(first.message()).isEqualTo(second.message());
    }

    @Test
    @DisplayName("never returns a null details list")
    void detailsAreNeverNull() {
        ApiErrorResponse response = new ApiErrorResponse(
                "X", "message", 400, java.time.Instant.now(), "/x", null);

        assertThat(response.details()).isEmpty();
    }

    @Test
    @DisplayName("strips control characters from the echoed path")
    void stripsControlCharactersFromPath() {
        ApiErrorResponse response = ApiErrorResponse.of(
                ApiErrorCode.FILE_NOT_FOUND, "Not found.", "/api/v1/files/x\r\nX-Injected: yes");

        assertThat(response.path()).doesNotContain("\r").doesNotContain("\n");
    }

    @Test
    @DisplayName("caps the echoed path length")
    void capsPathLength() {
        ApiErrorResponse response = ApiErrorResponse.of(
                ApiErrorCode.FILE_NOT_FOUND, "Not found.", "/api/v1/files/" + "a".repeat(5_000));

        assertThat(response.path()).hasSizeLessThanOrEqualTo(200);
    }

    @Test
    @DisplayName("rejects a non-error status")
    void rejectsNonErrorStatus() {
        assertThatThrownBy(() -> new ApiErrorResponse(
                "OK", "fine", 200, java.time.Instant.now(), "/x", List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("status");
    }

    @Test
    @DisplayName("rejects a blank code")
    void rejectsBlankCode() {
        assertThatThrownBy(() -> new ApiErrorResponse(
                "  ", "message", 400, java.time.Instant.now(), "/x", List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("code");
    }

    @Test
    @DisplayName("carries field-level detail when validation fails")
    void carriesFieldDetail() {
        ApiErrorResponse response = ApiErrorResponse.of(
                ApiErrorCode.VALIDATION_FAILED,
                "Request contained invalid fields.",
                "/api/v1/files",
                List.of(new ApiErrorResponse.FieldIssue("fileName", "must not be blank")));

        assertThat(response.details()).hasSize(1);
        assertThat(response.details().get(0).field()).isEqualTo("fileName");
    }

    @Test
    @DisplayName("separates a policy refusal from a lifecycle conflict")
    void separatesRefusalFromConflict() {
        assertThat(ApiErrorCode.RECONSTRUCTION_REFUSED.status()).isEqualTo(422);
        assertThat(ApiErrorCode.INVALID_STATE_TRANSITION.status()).isEqualTo(409);
    }

    @Test
    @DisplayName("classifies server errors separately from client errors")
    void classifiesServerErrors() {
        assertThat(ApiErrorCode.INTERNAL_ERROR.isClientError()).isFalse();
        assertThat(ApiErrorCode.FILE_NOT_FOUND.isClientError()).isTrue();
    }
}
