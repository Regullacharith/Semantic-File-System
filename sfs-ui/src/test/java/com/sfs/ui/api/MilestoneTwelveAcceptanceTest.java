package com.sfs.ui.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Milestone 12 acceptance: measured fidelity over HTTP")
class MilestoneTwelveAcceptanceTest {

    private static final Pattern JOB_ID = Pattern.compile("job-[0-9]+");

    private static final String OPERATOR = "operator";

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @LocalServerPort
    private int port;

    private HttpResponse<String> send(String method, String path, String credential,
                                      String body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json");
        if (credential != null) {
            builder.header("X-SFS-Credential", credential);
        }
        HttpRequest.BodyPublisher publisher = body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body);
        return CLIENT.send(builder.method(method, publisher).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private String completedReconstruction(String objectId) throws Exception {
        HttpResponse<String> started = send("POST", "/api/v1/reconstructions",
                OPERATOR, "{\"objectId\":\"" + objectId + "\"}");
        assertThat(started.statusCode()).isEqualTo(201);
        Matcher matcher = JOB_ID.matcher(started.body());
        assertThat(matcher.find()).isTrue();
        String jobId = matcher.group();
        long deadline = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadline) {
            if (send("GET", "/api/v1/jobs/" + jobId, OPERATOR, null).body()
                    .contains("\"terminal\":true")) {
                return jobId;
            }
            Thread.sleep(20);
        }
        throw new AssertionError("job never reached a terminal state");
    }

    @Test
    @DisplayName("every reconstruction receives all required metrics")
    void allMetricsPresent() throws Exception {
        String jobId = completedReconstruction("sfs-obj-0001-a1b2c3d4");

        HttpResponse<String> evaluation = send("GET",
                "/api/v1/evaluations/" + jobId, OPERATOR, null);

        assertThat(evaluation.statusCode()).isEqualTo(200);
        String body = evaluation.body();
        assertThat(body)
                .contains("\"availability\":\"AVAILABLE\"")
                .contains("SEMANTIC")
                .contains("STRUCTURAL")
                .contains("FACTUAL")
                .contains("ENTITY")
                .contains("RELATIONSHIP")
                .contains("COMPLETENESS")
                .contains("\"evaluator\":\"sfs-evaluation/0.1\"")
                .contains("criticalFact")
                .contains("originalBytes")
                .contains("semanticMemoryBytes");
    }

    @Test
    @DisplayName("critical fact preservation is reported explicitly")
    void criticalFactsExplicit() throws Exception {
        String jobId = completedReconstruction("sfs-obj-0001-a1b2c3d4");

        String body = send("GET", "/api/v1/evaluations/" + jobId, OPERATOR, null)
                .body();

        assertThat(body).contains("\"criticalFactsTotal\":")
                .contains("\"criticalFactsPreserved\":");
        assertThat(body).contains("critical fact(s) preserved.");
    }

    @Test
    @DisplayName("results are reproducible for fixed versioned inputs")
    void reproducible() throws Exception {
        String first = completedReconstruction("sfs-obj-0001-a1b2c3d4");
        String second = completedReconstruction("sfs-obj-0001-a1b2c3d4");

        String firstBody = send("GET", "/api/v1/evaluations/" + first, OPERATOR,
                null).body();
        String secondBody = send("GET", "/api/v1/evaluations/" + second, OPERATOR,
                null).body();

        assertThat(extract(firstBody, "SEMANTIC"))
                .isEqualTo(extract(secondBody, "SEMANTIC"));
        assertThat(extract(firstBody, "FACTUAL"))
                .isEqualTo(extract(secondBody, "FACTUAL"));
        assertThat(extractNumber(firstBody, "criticalFactsPreserved"))
                .isEqualTo(extractNumber(secondBody, "criticalFactsPreserved"));
    }

    @Test
    @DisplayName("a deleted original is unmeasurable and never estimated")
    void memorizedOriginalUnmeasurable() throws Exception {
        String jobId = completedReconstruction("sfs-obj-0002-e5f6a7b8");

        HttpResponse<String> evaluation = send("GET",
                "/api/v1/evaluations/" + jobId, OPERATOR, null);

        assertThat(evaluation.statusCode()).isEqualTo(200);
        assertThat(evaluation.body())
                .contains("\"availability\":\"ORIGINAL_UNAVAILABLE\"")
                .contains("no score is estimated");
        assertThat(evaluation.body()).doesNotContain("\"score\":");
    }

    @Test
    @DisplayName("the evaluation page shows measured numbers, not constants")
    void uiShowsMeasuredNumbers() throws Exception {
        String jobId = completedReconstruction("sfs-obj-0001-a1b2c3d4");

        HttpResponse<String> page = CLIENT.send(HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/evaluation/"
                        + jobId))
                .GET().build(), HttpResponse.BodyHandlers.ofString());

        assertThat(page.statusCode()).isEqualTo(200);
        assertThat(page.body())
                .contains("Fidelity")
                .contains("measured, not asserted")
                .contains("sfs-evaluation/0.1");
        assertThat(page.body()).doesNotContain("mock evaluator");
    }

    @Test
    @DisplayName("the evaluation list covers every job")
    void listCoversEveryJob() throws Exception {
        completedReconstruction("sfs-obj-0001-a1b2c3d4");

        HttpResponse<String> list = send("GET", "/api/v1/evaluations", OPERATOR,
                null);

        assertThat(list.statusCode()).isEqualTo(200);
        assertThat(list.body()).contains("AVAILABLE");
        assertThat(list.body()).contains("job-");
    }

    @Test
    @DisplayName("the version endpoint reports the evaluation system as real")
    void versionReportsEvaluation() throws Exception {
        HttpResponse<String> version = send("GET", "/api/v1/version", null, null);

        assertThat(version.statusCode()).isEqualTo(200);
        assertThat(version.body())
                .contains("\"milestone\":\"M13")
                .contains("sfs-evaluation/0.1")
                .contains("evaluation-fidelity")
                .contains("criticalFactChecks")
                .contains("calibration");
    }

    private String extract(String body, String key) {
        String quote = String.valueOf((char) 34);
        String marker = quote + key + quote + "," + quote + "label" + quote
                + ":" + quote;
        int at = body.indexOf(marker);
        assertThat(at).isGreaterThanOrEqualTo(0);
        int scoreAt = body.indexOf(quote + "score" + quote + ":", at);
        assertThat(scoreAt).isGreaterThanOrEqualTo(0);
        int valueStart = scoreAt + 8;
        return numericValue(body, valueStart);
    }

    private String extractNumber(String body, String key) {
        String quote = String.valueOf((char) 34);
        int at = body.indexOf(quote + key + quote + ":");
        assertThat(at).isGreaterThanOrEqualTo(0);
        return numericValue(body, at + key.length() + 3);
    }

    private String numericValue(String body, int valueStart) {
        int valueEnd = valueStart;
        while (valueEnd < body.length()
                && (Character.isDigit(body.charAt(valueEnd))
                        || body.charAt(valueEnd) == '.')) {
            valueEnd++;
        }
        return body.substring(valueStart, valueEnd);
    }
}
