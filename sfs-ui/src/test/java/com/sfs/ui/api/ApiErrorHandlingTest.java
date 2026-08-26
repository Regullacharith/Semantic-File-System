package com.sfs.ui.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("API error handling")
class ApiErrorHandlingTest {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @LocalServerPort
    private int port;

    private HttpResponse<String> send(String method, String path, String body) throws Exception {
        HttpRequest.BodyPublisher publisher = body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .method(method, publisher)
                .build();

        return CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String path) throws Exception {
        return send("GET", path, null);
    }

    @Nested
    @DisplayName("deterministic error codes")
    class DeterministicCodes {

        @Test
        @DisplayName("returns the same code and status for repeated invalid input")
        void repeatedInvalidInputIsDeterministic() throws Exception {
            String first = get("/api/v1/files/not-an-id").body();
            String second = get("/api/v1/files/not-an-id").body();

            assertThat(first).contains("\"code\":\"OBJECT_ID_INVALID\"");
            assertThat(second).contains("\"code\":\"OBJECT_ID_INVALID\"");
            assertThat(first).contains("\"status\":400");
            assertThat(second).contains("\"status\":400");
        }

        @Test
        @DisplayName("distinguishes a malformed identifier from an unknown one")
        void distinguishesMalformedFromUnknown() throws Exception {
            HttpResponse<String> malformed = get("/api/v1/files/not-an-id");
            HttpResponse<String> unknown = get("/api/v1/files/sfs-obj-9999-nothere");

            assertThat(malformed.statusCode()).isEqualTo(400);
            assertThat(malformed.body()).contains("OBJECT_ID_INVALID");

            assertThat(unknown.statusCode()).isEqualTo(404);
            assertThat(unknown.body()).contains("FILE_NOT_FOUND");
        }

        @Test
        @DisplayName("rejects a malformed job identifier before lookup")
        void rejectsMalformedJobId() throws Exception {
            HttpResponse<String> response = get("/api/v1/jobs/not-a-job");

            assertThat(response.statusCode()).isEqualTo(400);
            assertThat(response.body()).contains("JOB_ID_INVALID");
        }

        @Test
        @DisplayName("reports a lifecycle conflict as 409")
        void reportsLifecycleConflict() throws Exception {
            HttpResponse<String> response =
                    send("POST", "/api/v1/files/sfs-obj-0001-a1b2c3d4/analyze", null);

            assertThat(response.statusCode()).isEqualTo(409);
            assertThat(response.body()).contains("INVALID_STATE_TRANSITION");
        }

        @Test
        @DisplayName("reports a policy refusal as 422, not as a failure")
        void reportsPolicyRefusalAs422() throws Exception {
            HttpResponse<String> response = send("POST", "/api/v1/reconstructions",
                    "{\"objectId\":\"sfs-obj-0004-b3c4d5e6\"}");

            assertThat(response.statusCode()).isEqualTo(422);
            assertThat(response.body()).contains("\"refused\":true");
        }

        @Test
        @DisplayName("reports unreadable JSON as a malformed request")
        void reportsMalformedJson() throws Exception {
            HttpResponse<String> response = send("POST", "/api/v1/search", "{not json");

            assertThat(response.statusCode()).isEqualTo(400);
            assertThat(response.body()).contains("REQUEST_MALFORMED");
        }
    }

    @Nested
    @DisplayName("error shape")
    class ErrorShape {

        @Test
        @DisplayName("always returns the frozen error fields")
        void alwaysReturnsFrozenFields() throws Exception {
            String body = get("/api/v1/files/not-an-id").body();

            assertThat(body)
                    .contains("\"code\"")
                    .contains("\"message\"")
                    .contains("\"status\"")
                    .contains("\"timestamp\"")
                    .contains("\"path\"")
                    .contains("\"details\"");
        }

        @Test
        @DisplayName("returns JSON rather than the HTML error page for an API path")
        void apiPathsReturnJson() throws Exception {
            HttpResponse<String> notFound = get("/api/v1/nonexistent");

            assertThat(notFound.statusCode()).isEqualTo(404);
            assertThat(notFound.body())
                    .startsWith("{")
                    .doesNotContain("<!DOCTYPE html>");
        }

        @Test
        @DisplayName("returns JSON for a method that is not allowed on an API path")
        void methodNotAllowedReturnsJson() throws Exception {
            HttpResponse<String> response =
                    send("POST", "/api/v1/files/sfs-obj-0001-a1b2c3d4", null);

            assertThat(response.statusCode()).isEqualTo(405);
            assertThat(response.body())
                    .contains("METHOD_NOT_ALLOWED")
                    .doesNotContain("<!DOCTYPE html>");
        }

        @Test
        @DisplayName("still returns the HTML shell for a browser path")
        void uiPathsReturnHtml() throws Exception {
            HttpResponse<String> response = get("/nope");

            assertThat(response.statusCode()).isEqualTo(404);
            assertThat(response.body()).contains("Semantic File System");
        }
    }

    @Nested
    @DisplayName("safety")
    class Safety {

        @Test
        @DisplayName("never leaks a stack trace or framework detail")
        void neverLeaksInternals() throws Exception {
            for (String path : new String[]{
                    "/api/v1/files/not-an-id",
                    "/api/v1/files/sfs-obj-9999-nothere",
                    "/api/v1/jobs/not-a-job",
                    "/api/v1/nonexistent"}) {

                assertThat(get(path).body())
                        .as("leakage on %s", path)
                        .doesNotContain("org.springframework")
                        .doesNotContain("java.lang")
                        .doesNotContain("Exception")
                        .doesNotContain("at com.sfs");
            }
        }

        @Test
        @DisplayName("does not echo control characters into the error path")
        void doesNotEchoControlCharacters() throws Exception {
            HttpResponse<String> response = get("/api/v1/files/abc%0d%0aX-Injected:%20yes");

            assertThat(response.body()).doesNotContain("X-Injected: yes\r\n");
        }
    }
}
