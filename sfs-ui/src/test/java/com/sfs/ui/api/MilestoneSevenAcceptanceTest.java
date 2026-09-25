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
@DisplayName("Milestone 07 acceptance: reconstruction rules over HTTP")
class MilestoneSevenAcceptanceTest {

    private static final Pattern JOB_ID = Pattern.compile("job-[0-9]{4,}");

    private static final String AUTH_HEADER = "X-SFS-Credential";
    private static final String OPERATOR = "operator";

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

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

    @Test
    @DisplayName("a reconstruction is planned from real rules and proven by findings")
    void reconstructionUsesRealRules() throws Exception {
        HttpResponse<String> started = send("POST", "/api/v1/reconstructions", OPERATOR,
                "{\"objectId\":\"sfs-obj-0002-e5f6a7b8\"}");
        assertThat(started.statusCode()).isEqualTo(201);
        Matcher matcher = JOB_ID.matcher(started.body());
        assertThat(matcher.find()).isTrue();
        String jobId = matcher.group();

        HttpResponse<String> job = send("GET", "/api/v1/jobs/" + jobId, OPERATOR, null);
        assertThat(job.statusCode()).isEqualTo(200);
        String body = job.body();
        assertThat(body).contains("\"status\":\"COMPLETED\"");
        assertThat(body)
                .contains("sfs-rules/0.2")
                .contains("Plan rules")
                .contains("required fact");
    }

    @Test
    @DisplayName("a protected document is refused before any reconstruction runs")
    void protectedDocumentRejected() throws Exception {
        HttpResponse<String> started = send("POST", "/api/v1/reconstructions", OPERATOR,
                "{\"objectId\":\"sfs-obj-0004-b3c4d5e6\"}");
        assertThat(started.statusCode()).isEqualTo(422);
        assertThat(started.body())
                .contains("\"status\":\"REJECTED\"")
                .contains("Protected values");
    }

    @Test
    @DisplayName("the version endpoint reports rules diagnostics")
    void versionReportsRules() throws Exception {
        HttpResponse<String> version = send("GET", "/api/v1/version", null, null);
        assertThat(version.statusCode()).isEqualTo(200);
        assertThat(version.body())
                .contains("\"rules\"")
                .contains("sfs-rules/0.2")
                .contains("boundRuleSets")
                .contains("planDerivations")
                .contains("reconstruction-rules");
    }
}
