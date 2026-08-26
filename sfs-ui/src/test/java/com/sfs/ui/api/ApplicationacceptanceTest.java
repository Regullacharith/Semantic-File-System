package com.sfs.ui.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("application acceptance")
class Applicationacceptance {

    private static final Pattern OBJECT_ID = Pattern.compile("sfs-obj-[0-9]{4}-[a-z0-9]+");
    private static final Pattern JOB_ID = Pattern.compile("job-[0-9]{4}");
    private static final String BOUNDARY = "sfsAcceptanceBoundary";

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @LocalServerPort
    private int port;

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(20));
    }

    private HttpResponse<String> get(String path) throws Exception {
        return CLIENT.send(request(path).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> postJson(String path, String body) throws Exception {
        return CLIENT.send(
                request(path)
                        .header("Content-Type", "application/json")
                        .POST(body == null
                                ? HttpRequest.BodyPublishers.noBody()
                                : HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private String lastMatch(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        String found = null;
        while (matcher.find()) {
            found = matcher.group();
        }
        assertThat(found).as("expected %s in response", pattern.pattern()).isNotNull();
        return found;
    }

    @Nested
    @DisplayName("operation is callable without the UI")
    class CallableWithoutUi {

        @Test
        @DisplayName("serves every read operation as JSON")
        void servesEveryReadOperation() throws Exception {
            for (String path : new String[]{
                    "/api/v1/health",
                    "/api/v1/version",
                    "/api/v1/files",
                    "/api/v1/reconstructions",
                    "/api/v1/evaluations",
                    "/api/v1/security/settings"}) {

                HttpResponse<String> response = get(path);

                assertThat(response.statusCode()).as("status for %s", path).isEqualTo(200);
                assertThat(response.body()).as("body for %s", path).isNotBlank();
                assertThat(response.body())
                        .as("%s must return JSON, not HTML", path)
                        .doesNotContain("<!DOCTYPE html>");
            }
        }

        @Test
        @DisplayName("completes the whole lifecycle over HTTP with no HTML involved")
        void completesWholeLifecycleOverHttp() throws Exception {
            HttpResponse<String> imported = postJson("/api/v1/files",
                    "{\"fileName\":\"acceptance.txt\",\"content\":\"Budget approved 14 March 2026.\"}");

            assertThat(imported.statusCode()).isEqualTo(201);
            String objectId = lastMatch(OBJECT_ID, imported.body());

            assertThat(get("/api/v1/files/" + objectId).statusCode()).isEqualTo(200);

            assertThat(postJson("/api/v1/files/" + objectId + "/analyze", null).statusCode())
                    .isEqualTo(200);

            assertThat(get("/api/v1/objects/" + objectId + "/dna").statusCode()).isEqualTo(200);

            HttpResponse<String> search = postJson("/api/v1/search", "{\"text\":\"research\"}");
            assertThat(search.statusCode()).isEqualTo(200);

            HttpResponse<String> job = postJson("/api/v1/reconstructions",
                    "{\"objectId\":\"sfs-obj-0001-a1b2c3d4\"}");
            assertThat(job.statusCode()).isEqualTo(201);

            String jobId = lastMatch(JOB_ID, job.body());
            assertThat(get("/api/v1/jobs/" + jobId).statusCode()).isEqualTo(200);
            assertThat(get("/api/v1/reconstructions/" + jobId + "/artifact").statusCode())
                    .isEqualTo(200);
            assertThat(get("/api/v1/evaluations/" + jobId).statusCode()).isEqualTo(200);
        }

        @Test
        @DisplayName("imports a file through multipart as well as JSON")
        void importsViaMultipart() throws Exception {
            ByteArrayOutputStream body = new ByteArrayOutputStream();
            body.write(("--" + BOUNDARY + "\r\n").getBytes(StandardCharsets.UTF_8));
            body.write(("Content-Disposition: form-data; name=\"file\"; filename=\"multipart.txt\"\r\n")
                    .getBytes(StandardCharsets.UTF_8));
            body.write("Content-Type: text/plain\r\n\r\n".getBytes(StandardCharsets.UTF_8));
            body.write("Multipart content.".getBytes(StandardCharsets.UTF_8));
            body.write(("\r\n--" + BOUNDARY + "--\r\n").getBytes(StandardCharsets.UTF_8));

            HttpResponse<String> response = CLIENT.send(
                    request("/api/v1/files")
                            .header("Content-Type", "multipart/form-data; boundary=" + BOUNDARY)
                            .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());

            assertThat(response.statusCode()).isEqualTo(201);
        }

        @Test
        @DisplayName("supports both the POST and GET forms of search")
        void supportsBothSearchForms() throws Exception {
            HttpResponse<String> post = postJson("/api/v1/search", "{\"text\":\"research\"}");
            HttpResponse<String> getForm = get("/api/v1/search?q=research");

            assertThat(post.statusCode()).isEqualTo(200);
            assertThat(getForm.statusCode()).isEqualTo(200);
            assertThat(getForm.body()).contains("\"totalResults\"");
        }
    }

    @Nested
    @DisplayName("invalid inputs return deterministic errors")
    class DeterministicErrors {

        @Test
        @DisplayName("returns an identical error for identical bad input")
        void identicalBadInputGivesIdenticalError() throws Exception {
            String first = get("/api/v1/files/bad-id").body().replaceAll("\"timestamp\":\"[^\"]+\"", "");
            String second = get("/api/v1/files/bad-id").body().replaceAll("\"timestamp\":\"[^\"]+\"", "");

            assertThat(first).isEqualTo(second);
        }

        @Test
        @DisplayName("uses the frozen error codes")
        void usesFrozenErrorCodes() throws Exception {
            assertThat(get("/api/v1/files/bad-id").body()).contains("OBJECT_ID_INVALID");
            assertThat(get("/api/v1/files/sfs-obj-9999-nothere").body()).contains("FILE_NOT_FOUND");
            assertThat(get("/api/v1/jobs/bad-job").body()).contains("JOB_ID_INVALID");
            assertThat(get("/api/v1/jobs/job-9999").body()).contains("JOB_NOT_FOUND");
        }
    }

    @Nested
    @DisplayName("long-running jobs can be queried")
    class JobQuerying {

        @Test
        @DisplayName("returns a job that can then be polled by id")
        void jobCanBePolled() throws Exception {
            String jobId = lastMatch(JOB_ID, postJson("/api/v1/reconstructions",
                    "{\"objectId\":\"sfs-obj-0001-a1b2c3d4\"}").body());

            HttpResponse<String> polled = get("/api/v1/jobs/" + jobId);

            assertThat(polled.statusCode()).isEqualTo(200);
            assertThat(polled.body()).contains("\"status\"").contains("\"provenance\"");
        }

        @Test
        @DisplayName("reports a refusal as refused rather than failed")
        void reportsRefusalDistinctly() throws Exception {
            HttpResponse<String> response = postJson("/api/v1/reconstructions",
                    "{\"objectId\":\"sfs-obj-0004-b3c4d5e6\"}");

            assertThat(response.statusCode()).isEqualTo(422);
            assertThat(response.body()).contains("\"refused\":true");
            assertThat(response.body()).contains("\"hasArtifact\":false");
        }
    }

    @Nested
    @DisplayName("protected values never appear in API output")
    class NoSecretsInOutput {

        @Test
        @DisplayName("exposes no plaintext secret in any response")
        void noPlaintextSecretAnywhere() throws Exception {
            for (String path : new String[]{
                    "/api/v1/files",
                    "/api/v1/security/settings",
                    "/api/v1/objects/sfs-obj-0004-b3c4d5e6/dna"}) {

                assertThat(get(path).body())
                        .as("secret material in %s", path)
                        .doesNotContain("sk-")
                        .doesNotContain("AKIA")
                        .doesNotContain("password=")
                        .doesNotContain("Bearer ");
            }
        }

        @Test
        @DisplayName("reports protected references without their values")
        void protectedReferencesCarryNoValue() throws Exception {
            String body = get("/api/v1/objects/sfs-obj-0004-b3c4d5e6/dna").body();

            assertThat(body).contains("protectedReferences");
            assertThat(body).doesNotContain("\"value\"");
            assertThat(body).doesNotContain("\"plaintext\"");
        }

        @Test
        @DisplayName("states that no security control is enforced")
        void statesSecurityIsNotEnforced() throws Exception {
            String body = get("/api/v1/security/settings").body();

            assertThat(body).contains("\"enforced\":false");
            assertThat(body).contains("Milestone 13");
        }
    }

    @Nested
    @DisplayName("honesty of the API surface")
    class Honesty {

        @Test
        @DisplayName("reports no aggregate fidelity score")
        void reportsNoAggregateScore() throws Exception {
            String jobId = lastMatch(JOB_ID, postJson("/api/v1/reconstructions",
                    "{\"objectId\":\"sfs-obj-0001-a1b2c3d4\"}").body());

            String body = get("/api/v1/evaluations/" + jobId).body();

            assertThat(body)
                    .doesNotContain("\"overallScore\"")
                    .doesNotContain("\"aggregateScore\"")
                    .doesNotContain("\"overallFidelity\"");
        }

        @Test
        @DisplayName("declares the mocked state of every subsystem")
        void declaresMockedState() throws Exception {
            String body = get("/api/v1/version").body();

            assertThat(body).contains("mocked");
        }

        @Test
        @DisplayName("keeps the UI working alongside the API")
        void uiStillWorks() throws Exception {
            for (String path : new String[]{"/", "/files", "/search", "/settings"}) {
                assertThat(get(path).statusCode()).as("UI %s", path).isEqualTo(200);
            }
        }
    }
}

