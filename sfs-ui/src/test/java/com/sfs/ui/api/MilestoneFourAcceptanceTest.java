package com.sfs.ui.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.env.Environment;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Milestone 04 acceptance: semantic engine over HTTP")
class MilestoneFourAcceptanceTest {

    private static final Pattern OBJECT_ID = Pattern.compile("sfs-obj-[0-9]{4}-[a-z0-9]+");
    private static final Pattern JOB_ID = Pattern.compile("job-[0-9]{4}");

    private static final String AUTH_HEADER = "X-SFS-Credential";
    private static final String OPERATOR = "operator";
    private static final String READER = "reader";

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final String BENCHMARK = """
            # Summary

            The quarterly review covers the database platform for Q3 2026.
            Query latency decreased by 40 percent after indexing changes.

            # Measurements

            PostgreSQL hosts the production workload for the analytics platform.

            # Recommendations

            PostgreSQL provides the primary storage for the platform.
            """;

    @LocalServerPort
    private int port;

    @Autowired
    private Environment environment;

    private HttpResponse<String> send(String method, String path, String credential, String body)
            throws Exception {

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json");

        if (credential != null) {
            builder.header(AUTH_HEADER, credential);
        }

        HttpRequest.BodyPublisher publisher = body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body);

        return CLIENT.send(builder.method(method, publisher).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private String importObject(String fileName, String content) throws Exception {
        HttpResponse<String> imported = send("POST", "/api/v1/files", OPERATOR,
                "{\"fileName\":\"" + fileName + "\",\"content\":"
                        + quotedJson(content) + ",\"contentType\":\"text/plain\"}");
        assertThat(imported.statusCode()).isEqualTo(201);
        Matcher matcher = OBJECT_ID.matcher(imported.body());
        assertThat(matcher.find()).isTrue();
        return matcher.group();
    }

    private static String quotedJson(String text) {
        return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n") + "\"";
    }

    private String statusOf(String objectId) throws Exception {
        return send("GET", "/api/v1/files/" + objectId, READER, null).body();
    }

    private void awaitStatus(String objectId, String expectedStatus) throws Exception {
        for (int i = 0; i < 300; i++) {
            if (statusOf(objectId).contains("\"status\":\"" + expectedStatus + "\"")) {
                return;
            }
            Thread.sleep(10);
        }
        throw new AssertionError("object " + objectId + " never reached " + expectedStatus);
    }

    @Test
    @DisplayName("analysis completes independently from the import call")
    void analysisIsAsynchronous() throws Exception {
        String objectId = importObject("async.txt", BENCHMARK);

        HttpResponse<String> requested =
                send("POST", "/api/v1/files/" + objectId + "/analyze", OPERATOR, null);
        assertThat(requested.statusCode()).isEqualTo(200);

        awaitStatus(objectId, "ANALYZED");

        HttpResponse<String> events =
                send("GET", "/api/v1/files/" + objectId + "/events", READER, null);
        assertThat(events.body())
                .contains("ANALYSIS_STARTED")
                .contains("ANALYSIS_SUCCEEDED");
    }

    @Test
    @DisplayName("the analysis job is observable through the unified jobs API")
    void analysisJobIsObservable() throws Exception {
        String objectId = importObject("jobbed.txt", BENCHMARK);
        send("POST", "/api/v1/files/" + objectId + "/analyze", OPERATOR, null);

        String events = send("GET", "/api/v1/files/" + objectId + "/events", READER, null).body();
        Matcher matcher = JOB_ID.matcher(events);
        assertThat(matcher.find()).isTrue();
        String jobId = matcher.group();

        String job = null;
        for (int i = 0; i < 300; i++) {
            HttpResponse<String> response = send("GET", "/api/v1/jobs/" + jobId, READER, null);
            assertThat(response.statusCode()).isEqualTo(200);
            job = response.body();
            if (job.contains("\"status\":\"COMPLETED\"")) {
                break;
            }
            Thread.sleep(10);
        }
        assertThat(job).isNotNull();
        assertThat(job)
                .contains("\"sourceName\":\"analysis\"")
                .contains("\"status\":\"COMPLETED\"")
                .contains(objectId);
    }

    @Test
    @DisplayName("a benchmark document produces complete DNA")
    void benchmarkProducesCompleteDna() throws Exception {
        String objectId = importObject("benchmark.txt", BENCHMARK);
        send("POST", "/api/v1/files/" + objectId + "/analyze", OPERATOR, null);
        awaitStatus(objectId, "ANALYZED");

        HttpResponse<String> dna =
                send("GET", "/api/v1/objects/" + objectId + "/dna", READER, null);
        assertThat(dna.statusCode()).isEqualTo(200);
        String body = dna.body();
        assertThat(body)
                .contains("\"present\":true")
                .contains("\"schemaVersion\":\"sfs-dna/0.1\"")
                .contains("\"embeddingDimensions\":64")
                .contains("\"summary\":\"The quarterly review covers")
                .contains("Q3 2026");
        assertThat(body).contains("\"entities\":[{");
        assertThat(body).contains("\"facts\":[{");
        assertThat(body).contains("\"structure\":[{");
        assertThat(body).contains("\"concepts\":[\"");
        assertThat(body).doesNotContain("\"protectedReferences\":[{");
    }

    @Test
    @DisplayName("analysis performance measurements are captured in the audit trail")
    void analysisDurationIsMeasured() throws Exception {
        String objectId = importObject("measured.txt", BENCHMARK);
        send("POST", "/api/v1/files/" + objectId + "/analyze", OPERATOR, null);
        awaitStatus(objectId, "ANALYZED");

        String events = send("GET", "/api/v1/files/" + objectId + "/events", READER, null).body();
        assertThat(events).contains("analysis completed in");
        assertThat(events).contains("durationMs");
    }
}
