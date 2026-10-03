package com.sfs.ui.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

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
@DisplayName("Milestone 14 end-to-end journey: one file, its whole life, over HTTP")
class EndToEndJourneyTest {

    private static final Pattern OBJECT_ID = Pattern.compile("sfs-obj-[0-9a-f-]+");
    private static final Pattern JOB_ID = Pattern.compile("job-[0-9]+");

    private static final String DOCUMENT = """
            # Overview

            The release candidate review covers the whole platform for 2026.

            # Checklist

            The release candidate must pass every automated test.
            The build must succeed from a clean checkout.
            """;

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
                .timeout(Duration.ofSeconds(30))
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

    private void awaitStatus(String objectId, String status) throws Exception {
        long deadline = System.currentTimeMillis() + 30_000;
        while (System.currentTimeMillis() < deadline) {
            if (send("GET", "/api/v1/files/" + objectId, "reader", null).body()
                    .contains("\"" + status + "\"")) {
                return;
            }
            Thread.sleep(50);
        }
        throw new AssertionError(objectId + " never reached " + status);
    }

    private void awaitJobTerminal(String jobId) throws Exception {
        long deadline = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadline) {
            if (send("GET", "/api/v1/jobs/" + jobId, "operator", null).body()
                    .contains("\"terminal\":true")) {
                return;
            }
            Thread.sleep(20);
        }
        throw new AssertionError("job " + jobId + " never reached a terminal state");
    }

    @Test
    @DisplayName("import, analysis, search, reconstruction, evaluation, memorization "
            + "and memory-only reconstruction all work in one journey")
    void fullJourney() throws Exception {
        String boundary = "sfs-e2e-boundary";
        java.io.ByteArrayOutputStream multipart = new java.io.ByteArrayOutputStream();
        multipart.write(("--" + boundary + "\r\nContent-Disposition: form-data; "
                + "name=\"file\"; filename=\"release-review.txt\"\r\n"
                + "Content-Type: text/plain\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        multipart.write(DOCUMENT.getBytes(StandardCharsets.UTF_8));
        multipart.write(("\r\n--" + boundary + "--\r\n")
                .getBytes(StandardCharsets.UTF_8));
        HttpResponse<String> imported = CLIENT.send(HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/api/v1/files"))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .header("X-SFS-Credential", "operator")
                .POST(HttpRequest.BodyPublishers.ofByteArray(multipart.toByteArray()))
                .build(), HttpResponse.BodyHandlers.ofString());
        assertThat(imported.statusCode()).isEqualTo(201);
        Matcher idMatcher = OBJECT_ID.matcher(imported.body());
        assertThat(idMatcher.find()).isTrue();
        String objectId = idMatcher.group();

        assertThat(send("POST", "/api/v1/files/" + objectId + "/analyze",
                "operator", null).statusCode()).isEqualTo(200);
        awaitStatus(objectId, "ANALYZED");

        HttpResponse<String> dna = send("GET", "/api/v1/objects/" + objectId
                + "/dna", "operator", null);
        assertThat(dna.statusCode()).isEqualTo(200);
        assertThat(dna.body()).contains("release candidate");

        HttpResponse<String> search = send("POST", "/api/v1/search", "operator",
                "{\"text\":\"release candidate review platform\",\"maxResults\":10}");
        assertThat(search.statusCode()).isEqualTo(200);
        assertThat(search.body()).contains(objectId);

        HttpResponse<String> started = send("POST", "/api/v1/reconstructions",
                "operator", "{\"objectId\":\"" + objectId + "\"}");
        assertThat(started.statusCode()).isEqualTo(201);
        Matcher jobMatcher = JOB_ID.matcher(started.body());
        assertThat(jobMatcher.find()).isTrue();
        String jobId = jobMatcher.group();
        awaitJobTerminal(jobId);
        assertThat(send("GET", "/api/v1/jobs/" + jobId, "operator", null).body())
                .contains("\"status\":\"COMPLETED\"");

        HttpResponse<String> artifact = send("GET", "/api/v1/reconstructions/"
                + jobId + "/artifact", "operator", null);
        assertThat(artifact.statusCode()).isEqualTo(200);
        assertThat(artifact.body()).contains("NOT THE ORIGINAL FILE");

        HttpResponse<String> evaluation = send("GET", "/api/v1/evaluations/"
                + jobId, "operator", null);
        assertThat(evaluation.statusCode()).isEqualTo(200);
        assertThat(evaluation.body())
                .contains("\"availability\":\"AVAILABLE\"")
                .contains("sfs-evaluation/0.1");

        assertThat(send("POST", "/api/v1/files/" + objectId + "/memorize",
                "operator", null).statusCode()).isEqualTo(200);
        awaitStatus(objectId, "MEMORY_COMMITTED");
        assertThat(send("DELETE", "/api/v1/files/" + objectId,
                "operator", "{\"confirmObjectId\":\"" + objectId + "\"}")
                .statusCode()).isEqualTo(200);
        assertThat(send("POST", "/api/v1/files/" + objectId + "/purge",
                "custodian", "{\"confirmObjectId\":\"" + objectId + "\"}")
                .statusCode()).isEqualTo(200);

        HttpResponse<String> memorySearch = send("POST", "/api/v1/search",
                "operator",
                "{\"text\":\"release candidate review platform\",\"maxResults\":10}");
        assertThat(memorySearch.body()).contains(objectId);

        HttpResponse<String> memoryReconstruction = send("POST",
                "/api/v1/reconstructions", "operator",
                "{\"objectId\":\"" + objectId + "\"}");
        assertThat(memoryReconstruction.statusCode()).isEqualTo(201);
        Matcher memoryJob = JOB_ID.matcher(memoryReconstruction.body());
        assertThat(memoryJob.find()).isTrue();
        awaitJobTerminal(memoryJob.group());
        assertThat(send("GET", "/api/v1/jobs/" + memoryJob.group(), "operator",
                null).body()).contains("\"status\":\"COMPLETED\"");

        HttpResponse<String> unmeasurable = send("GET", "/api/v1/evaluations/"
                + memoryJob.group(), "operator", null);
        assertThat(unmeasurable.body()).contains("\"availability\":\"ORIGINAL_UNAVAILABLE\"");
    }
}
