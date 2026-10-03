package com.sfs.ui.api;

import com.sfs.core.dna.DnaCanonical;
import com.sfs.engine.record.InMemorySemanticRecordStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Milestone 14 benchmark: stage-separated performance and storage accounting")
class StagePerformanceBenchmarkTest {

    private static final Pattern OBJECT_ID = Pattern.compile("sfs-obj-[0-9a-f-]+");
    private static final Pattern JOB_ID = Pattern.compile("job-[0-9]+");

    private static final String[] FIXTURES = {
            "quarterly-report.txt", "team-meeting-minutes.txt", "research-notes.txt"};

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @LocalServerPort
    private int port;

    @Autowired
    private InMemorySemanticRecordStore recordStore;

    private final Map<String, Long> importNanos = new LinkedHashMap<>();
    private final Map<String, Long> analyzeNanos = new LinkedHashMap<>();
    private final Map<String, Long> searchNanos = new LinkedHashMap<>();
    private final Map<String, Long> reconstructNanos = new LinkedHashMap<>();
    private final Map<String, Long> evaluateNanos = new LinkedHashMap<>();
    private final Map<String, byte[]> originals = new LinkedHashMap<>();
    private final Map<String, String> objectIds = new LinkedHashMap<>();

    private HttpResponse<String> send(String method, String path, String credential,
                                      String body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json");
        if (credential != null) {
            builder.header("X-SFS-Credential", credential);
        }
        return CLIENT.send(builder.method(method,
                body == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private void awaitStatus(String objectId, String status) throws Exception {
        long deadline = System.currentTimeMillis() + 30_000;
        while (System.currentTimeMillis() < deadline) {
            if (send("GET", "/api/v1/files/" + objectId, "reader", null).body()
                    .contains("\"" + status + "\"")) {
                return;
            }
            Thread.sleep(30);
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
        throw new AssertionError("job never reached a terminal state");
    }

    private void runPipeline() throws Exception {
        for (String fixture : FIXTURES) {
            byte[] bytes = Files.readAllBytes(Path.of("..", "sfs-engine", "src",
                    "test", "resources", "benchmarks", fixture));
            originals.put(fixture, bytes);

            long importStart = System.nanoTime();
            String boundary = "sfs-perf-boundary";
            java.io.ByteArrayOutputStream multipart = new java.io.ByteArrayOutputStream();
            multipart.write(("--" + boundary + "\r\nContent-Disposition: form-data; "
                    + "name=\"file\"; filename=\"" + fixture + "\"\r\n"
                    + "Content-Type: text/plain\r\n\r\n")
                    .getBytes(StandardCharsets.UTF_8));
            multipart.write(bytes);
            multipart.write(("\r\n--" + boundary + "--\r\n")
                    .getBytes(StandardCharsets.UTF_8));
            HttpResponse<String> imported = CLIENT.send(HttpRequest.newBuilder()
                    .uri(URI.create("http://127.0.0.1:" + port + "/api/v1/files"))
                    .header("Content-Type", "multipart/form-data; boundary="
                            + boundary)
                    .header("X-SFS-Credential", "operator")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(
                            multipart.toByteArray()))
                    .build(), HttpResponse.BodyHandlers.ofString());
            assertThat(imported.statusCode()).isEqualTo(201);
            Matcher idMatcher = OBJECT_ID.matcher(imported.body());
            assertThat(idMatcher.find()).isTrue();
            String objectId = idMatcher.group();
            objectIds.put(fixture, objectId);
            importNanos.put(fixture, System.nanoTime() - importStart);

            long analyzeStart = System.nanoTime();
            send("POST", "/api/v1/files/" + objectId + "/analyze", "operator", null);
            awaitStatus(objectId, "ANALYZED");
            analyzeNanos.put(fixture, System.nanoTime() - analyzeStart);
        }

        long searchStart = System.nanoTime();
        HttpResponse<String> search = send("POST", "/api/v1/search", "operator",
                "{\"text\":\"database latency indexing embeddings retrieval "
                        + "milestone delivery\",\"maxResults\":10}");
        searchNanos.put("corpus", System.nanoTime() - searchStart);
        assertThat(search.statusCode()).isEqualTo(200);

        for (String fixture : FIXTURES) {
            String objectId = objectIds.get(fixture);
            long reconstructStart = System.nanoTime();
            HttpResponse<String> started = send("POST", "/api/v1/reconstructions",
                    "operator", "{\"objectId\":\"" + objectId + "\"}");
            assertThat(started.statusCode()).isEqualTo(201);
            Matcher jobMatcher = JOB_ID.matcher(started.body());
            assertThat(jobMatcher.find()).isTrue();
            String jobId = jobMatcher.group();
            awaitJobTerminal(jobId);
            reconstructNanos.put(fixture, System.nanoTime() - reconstructStart);

            long evaluateStart = System.nanoTime();
            HttpResponse<String> evaluation = send("GET", "/api/v1/evaluations/"
                    + jobId, "operator", null);
            evaluateNanos.put(fixture, System.nanoTime() - evaluateStart);
            assertThat(evaluation.body()).contains("\"availability\":\"AVAILABLE\"");
        }
    }

    @Test
    @DisplayName("every stage is measured separately across the corpus")
    void stagesSeparated() throws Exception {
        runPipeline();

        for (String fixture : FIXTURES) {
            assertThat(importNanos.get(fixture)).isPositive();
            assertThat(analyzeNanos.get(fixture)).isPositive();
            assertThat(reconstructNanos.get(fixture)).isPositive();
            assertThat(evaluateNanos.get(fixture)).isPositive();
            assertThat(importNanos.get(fixture)).isLessThan(30_000_000_000L);
            assertThat(analyzeNanos.get(fixture)).isLessThan(30_000_000_000L);
            assertThat(reconstructNanos.get(fixture)).isLessThan(30_000_000_000L);
            assertThat(evaluateNanos.get(fixture)).isLessThan(30_000_000_000L);
        }
        assertThat(searchNanos.get("corpus")).isPositive();
        assertThat(searchNanos.get("corpus")).isLessThan(30_000_000_000L);
    }

    @Test
    @DisplayName("storage accounting includes original, artifact, DNA, vector, database "
            + "and security overhead")
    void storageAccounting() throws Exception {
        runPipeline();

        long originalBytes = 0;
        long artifactBytes = 0;
        long dnaBytes = 0;
        long vectorBytes = 0;
        for (String fixture : FIXTURES) {
            String objectId = objectIds.get(fixture);
            originalBytes += originals.get(fixture).length;
            var stored = recordStore.findStored(objectId).orElseThrow();
            dnaBytes += DnaCanonical.serialize(stored.dna())
                    .getBytes(StandardCharsets.UTF_8).length;
            vectorBytes += stored.dna().embedding().dimensions() * 8L;
            HttpResponse<String> evaluation = send("GET", "/api/v1/evaluations",
                    "operator", null);
            assertThat(evaluation.statusCode()).isEqualTo(200);
        }

        assertThat(originalBytes).isPositive();
        assertThat(dnaBytes).isPositive();
        assertThat(vectorBytes).isEqualTo(64L * 8 * FIXTURES.length);

        Path dbFile = Path.of("data", "sfs-memory.mv.db");
        long dbBytes = Files.exists(dbFile) ? Files.size(dbFile) : 0L;
        assertThat(dbBytes).isPositive();

        Path secureFile = Path.of("data", "sfs-secure", "secrets.properties");
        Path keyFile = Path.of("data", "sfs-keys", "master.key");
        long secureBytes = Files.exists(secureFile) ? Files.size(secureFile) : 0L;
        long keyBytes = Files.exists(keyFile) ? Files.size(keyFile) : 0L;
        assertThat(keyBytes).isGreaterThanOrEqualTo(32L);

        long memoryOnly = dnaBytes + vectorBytes;
        long fullAccounting = dbBytes + secureBytes + keyBytes;
        assertThat(fullAccounting).isPositive();
        assertThat(memoryOnly).isLessThan(originalBytes * 10);
    }

    @Test
    @DisplayName("fixed versioned inputs reproduce the same artifacts")
    void reproducible() throws Exception {
        runPipeline();
        String objectId = objectIds.get(FIXTURES[0]);

        HttpResponse<String> first = send("POST", "/api/v1/reconstructions",
                "operator", "{\"objectId\":\"" + objectId + "\"}");
        Matcher firstMatcher = JOB_ID.matcher(first.body());
        assertThat(firstMatcher.find()).isTrue();
        awaitJobTerminal(firstMatcher.group());
        String firstArtifact = send("GET", "/api/v1/reconstructions/"
                + firstMatcher.group() + "/artifact", "operator", null).body();

        HttpResponse<String> second = send("POST", "/api/v1/reconstructions",
                "operator", "{\"objectId\":\"" + objectId + "\"}");
        Matcher secondMatcher = JOB_ID.matcher(second.body());
        assertThat(secondMatcher.find()).isTrue();
        awaitJobTerminal(secondMatcher.group());
        String secondArtifact = send("GET", "/api/v1/reconstructions/"
                + secondMatcher.group() + "/artifact", "operator", null).body();

        assertThat(firstArtifact).isEqualTo(secondArtifact);
    }
}
