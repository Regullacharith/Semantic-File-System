package com.sfs.ui.view;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Error view model")
class ErrorViewModelTest {

    @Nested
    @DisplayName("construction")
    class Construction {

        @Test
        @DisplayName("rejects a status that is not an HTTP error")
        void rejectsNonErrorStatus() {
            assertThatThrownBy(() -> new ErrorViewModel(200, "Fine", "Fine", "Fine", "/"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("status");
        }

        @Test
        @DisplayName("rejects a blank title")
        void rejectsBlankTitle() {
            assertThatThrownBy(() -> new ErrorViewModel(404, "  ", "Summary", "Guidance", "/"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("title");
        }

        @Test
        @DisplayName("rejects a blank summary")
        void rejectsBlankSummary() {
            assertThatThrownBy(() -> new ErrorViewModel(404, "Title", "  ", "Guidance", "/"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("summary");
        }

        @Test
        @DisplayName("rejects a blank guidance")
        void rejectsBlankGuidance() {
            assertThatThrownBy(() -> new ErrorViewModel(404, "Title", "Summary", "  ", "/"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("guidance");
        }
    }

    @Nested
    @DisplayName("status mapping")
    class StatusMapping {

        @ParameterizedTest
        @ValueSource(ints = {400, 403, 404, 405, 409, 500, 503})
        @DisplayName("produces a complete message for every error status")
        void producesCompleteMessage(int status) {
            ErrorViewModel error = ErrorViewModel.forStatus(status, "/anything");

            assertThat(error.status()).isEqualTo(status);
            assertThat(error.title()).isNotBlank();
            assertThat(error.summary()).isNotBlank();
            assertThat(error.guidance()).isNotBlank();
        }

        @Test
        @DisplayName("falls back to a server error when the status is not an error status")
        void fallsBackToServerError() {
            assertThat(ErrorViewModel.forStatus(200, "/x").status()).isEqualTo(500);
            assertThat(ErrorViewModel.forStatus(0, "/x").status()).isEqualTo(500);
        }

        @Test
        @DisplayName("distinguishes client refusal from server failure")
        void distinguishesClientFromServer() {
            assertThat(ErrorViewModel.forStatus(404, "/x").isServerError()).isFalse();
            assertThat(ErrorViewModel.forStatus(500, "/x").isServerError()).isTrue();
        }

        @ParameterizedTest
        @ValueSource(ints = {400, 403, 404, 405})
        @DisplayName("never blames the user's data for a client error")
        void neverImpliesDataLoss(int status) {
            ErrorViewModel error = ErrorViewModel.forStatus(status, "/x");

            assertThat(error.summary() + " " + error.guidance())
                    .doesNotContainIgnoringCase("deleted")
                    .doesNotContainIgnoringCase("corrupt")
                    .doesNotContainIgnoringCase("lost");
        }
    }

    @Nested
    @DisplayName("requested path")
    class RequestedPath {

        @Test
        @DisplayName("reports when no path is available")
        void reportsMissingPath() {
            assertThat(ErrorViewModel.forStatus(404, null).hasRequestedPath()).isFalse();
            assertThat(ErrorViewModel.forStatus(404, "   ").hasRequestedPath()).isFalse();
        }

        @Test
        @DisplayName("keeps a normal path intact")
        void keepsNormalPath() {
            ErrorViewModel error = ErrorViewModel.forStatus(404, "/objects/sfs-obj-9999-zzzz");

            assertThat(error.hasRequestedPath()).isTrue();
            assertThat(error.requestedPath()).isEqualTo("/objects/sfs-obj-9999-zzzz");
        }

        @Test
        @DisplayName("truncates an absurdly long path")
        void truncatesLongPath() {
            String longPath = "/" + "a".repeat(5_000);

            String rendered = ErrorViewModel.forStatus(404, longPath).requestedPath();

            assertThat(rendered).hasSizeLessThan(250);
        }
    }

    @Nested
    @DisplayName("message safety")
    class MessageSafety {

        @ParameterizedTest
        @ValueSource(ints = {400, 403, 404, 405, 500})
        @DisplayName("exposes no framework or implementation detail")
        void exposesNoInternals(int status) {
            ErrorViewModel error = ErrorViewModel.forStatus(status, "/x");
            String text = error.title() + " " + error.summary() + " " + error.guidance();

            assertThat(text)
                    .doesNotContain("Whitelabel")
                    .doesNotContain("Spring")
                    .doesNotContain("Exception")
                    .doesNotContain("org.springframework")
                    .doesNotContain("java.lang")
                    .doesNotContainIgnoringCase("stack trace")
                    .doesNotContainIgnoringCase("mapping for");
        }
    }
}
