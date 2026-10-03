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
@DisplayName("Milestone 11 acceptance: the reconstruction engine over HTTP")
class MilestoneElevenAcceptanceTest {

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

    private String startReconstruction(String objectId) throws Exception {
        HttpResponse<String> started = send("POST", "/api/v1/reconstructions",
                OPERATOR, "{\"objectId\":\"" + objectId + "\"}");
        assertThat(started.statusCode()).isEqualTo(201);
        Matcher matcher = JOB_ID.matcher(started.body());
        assertThat(matcher.find()).isTrue();
        return matcher.group();
    }

    private void awaitTerminal(String jobId) throws Exception {
        long deadline = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadline) {
            if (send("GET", "/api/v1/jobs/" + jobId, OPERATOR, null).body()
                    .contains("\"terminal\":true")) {
                return;
            }
            Thread.sleep(20);
        }
        throw new AssertionError("job " + jobId + " never reached a terminal state");
    }

    @Test
    @DisplayName("a single-click reconstruction produces a labeled TXT artifact")
    void singleClickProducesArtifact() throws Exception {
        String jobId = startReconstruction("sfs-obj-0001-a1b2c3d4");
        awaitTerminal(jobId);

        HttpResponse<String> artifact = send("GET",
                "/api/v1/reconstructions/" + jobId + "/artifact", OPERATOR, null);

        assertThat(artifact.statusCode()).isEqualTo(200);
        assertThat(artifact.body())
                .contains("NOT THE ORIGINAL FILE")
                .contains("estimated")
                .contains("sfs-obj-0001-a1b2c3d4")
                .contains("sfs-rules/0.2")
                .contains("sfs-reconstruction/deterministic-baseline/0.1");
        assertThat(artifact.headers().firstValue("Content-Type").orElse(""))
                .contains("text/plain");
    }

    @Test
    @DisplayName("job status stays observable with recorded provenance and findings")
    void jobStatusObservableWithProvenance() throws Exception {
        String jobId = startReconstruction("sfs-obj-0001-a1b2c3d4");
        awaitTerminal(jobId);

        HttpResponse<String> job = send("GET", "/api/v1/jobs/" + jobId, OPERATOR, null);

        assertThat(job.statusCode()).isEqualTo(200);
        assertThat(job.body())
                .contains("\"status\":\"COMPLETED\"")
                .contains("\"terminal\":true")
                .contains("sfs-dna/0.2")
                .contains("sfs-rules/0.2")
                .contains("deterministic-baseline")
                .contains("Plan rules");
    }

    @Test
    @DisplayName("fixed versioned inputs reproduce identical artifacts")
    void reproducibleWithinTolerance() throws Exception {
        String first = startReconstruction("sfs-obj-0001-a1b2c3d4");
        String second = startReconstruction("sfs-obj-0001-a1b2c3d4");
        awaitTerminal(second);

        HttpResponse<String> firstArtifact = send("GET",
                "/api/v1/reconstructions/" + first + "/artifact", OPERATOR, null);
        HttpResponse<String> secondArtifact = send("GET",
                "/api/v1/reconstructions/" + second + "/artifact", OPERATOR, null);

        assertThat(firstArtifact.statusCode()).isEqualTo(200);
        assertThat(secondArtifact.statusCode()).isEqualTo(200);
        assertThat(firstArtifact.body()).isEqualTo(secondArtifact.body());
    }

    @Test
    @DisplayName("no unauthorized secret appears and protected objects are refused")
    void protectedObjectRefused() throws Exception {
        HttpResponse<String> started = send("POST", "/api/v1/reconstructions",
                OPERATOR, "{\"objectId\":\"sfs-obj-0004-b3c4d5e6\"}");

        assertThat(started.statusCode()).isEqualTo(422);
        assertThat(started.body())
                .contains("\"status\":\"REJECTED\"")
                .contains("Protected values");
        assertThat(started.body()).doesNotContain("hunter2");

        HttpResponse<String> artifact = send("GET",
                "/api/v1/reconstructions/job-0001/artifact", OPERATOR, null);
        assertThat(artifact.body()).doesNotContain("hunter2");
    }

    @Test
    @DisplayName("an unknown object produces an explicit terminal failed job")
    void unknownObjectFailsExplicitly() throws Exception {
        HttpResponse<String> started = send("POST", "/api/v1/reconstructions",
                OPERATOR, "{\"objectId\":\"sfs-obj-9999-ffffffff\"}");

        assertThat(started.statusCode()).isEqualTo(201);
        assertThat(started.body())
                .contains("\"status\":\"FAILED\"")
                .contains("No object exists");
    }

    @Test
    @DisplayName("a memorized object reconstructs without its original bytes")
    void memorizedObjectReconstructs() throws Exception {
        String jobId = startReconstruction("sfs-obj-0002-e5f6a7b8");
        awaitTerminal(jobId);

        HttpResponse<String> job = send("GET", "/api/v1/jobs/" + jobId, OPERATOR, null);

        assertThat(job.body()).contains("\"status\":\"COMPLETED\"");
        assertThat(send("GET", "/api/v1/reconstructions/" + jobId + "/artifact",
                OPERATOR, null).statusCode()).isEqualTo(200);
    }

    @Test
    @DisplayName("the version endpoint reports the reconstruction engine as real")
    void versionReportsEngine() throws Exception {
        HttpResponse<String> version = send("GET", "/api/v1/version", null, null);

        assertThat(version.statusCode()).isEqualTo(200);
        assertThat(version.body())
                .contains("\"milestone\":\"M12")
                .contains("sfs-reconstruction-engine/0.1")
                .contains("sfs-reconstruction/deterministic-baseline/0.1")
                .contains("reconstruction-engine")
                .contains("engineJobsTotal")
                .contains("\"evaluator\":\"sfs-evaluation/0.1\"");
    }

    @Test
    @DisplayName("the single-click UI flow serves the engine artifact")
    void uiSingleClickFlow() throws Exception {
        HttpResponse<String> posted = send("POST", "/reconstruction/sfs-obj-0001-a1b2c3d4",
                null, null);
        assertThat(posted.statusCode()).isEqualTo(200);
        Matcher matcher = JOB_ID.matcher(posted.body());
        assertThat(matcher.find()).isTrue();
        String jobId = matcher.group();

        long deadline = System.currentTimeMillis() + 10_000;
        String jobPage = posted.body();
        while (System.currentTimeMillis() < deadline) {
            jobPage = send("GET", "/reconstruction/job/" + jobId, null, null).body();
            if (jobPage.contains("Completed") || jobPage.contains("Rejected")
                    || jobPage.contains("Failed")) {
                break;
            }
            Thread.sleep(20);
        }
        assertThat(jobPage).contains("Completed");

        HttpResponse<String> download = CLIENT.send(HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port
                        + "/reconstruction/job/" + jobId + "/artifact"))
                .GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(download.statusCode()).isEqualTo(200);
        assertThat(download.body()).contains("NOT THE ORIGINAL FILE");
    }
}
