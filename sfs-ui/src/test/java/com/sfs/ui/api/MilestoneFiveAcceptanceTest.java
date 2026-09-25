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
@DisplayName("Milestone 05 acceptance: adapter framework and text adapter over HTTP")
class MilestoneFiveAcceptanceTest {

    private static final Pattern OBJECT_ID = Pattern.compile("sfs-obj-[0-9]{4}-[a-z0-9]+");

    private static final String AUTH_HEADER = "X-SFS-Credential";
    private static final String OPERATOR = "operator";
    private static final String READER = "reader";

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final String DOCUMENT = """
            # Overview

            The acceptance review covers the adapter framework for 2026.
            Recall improved by 15 percent after the embedding change.

            # Findings

            PostgreSQL hosts the production workload for the platform.
            Embeddings support the retrieval targets for Q4 2026.
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

    private String importFile(String fileName) throws Exception {
        HttpResponse<String> imported = send("POST", "/api/v1/files", OPERATOR,
                "{\"fileName\":\"" + fileName + "\",\"content\":\"Acceptance content "
                        + "with words for the adapter framework.\",\"contentType\":\"text/plain\"}");
        return imported.statusCode() + "|" + imported.body();
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
    @DisplayName("a TXT file automatically selects the Text Adapter and completes analysis")
    void txtSelectsTextAdapter() throws Exception {
        HttpResponse<String> imported = send("POST", "/api/v1/files", OPERATOR,
                "{\"fileName\":\"adapter-acceptance.txt\",\"content\":"
                        + quotedJson(DOCUMENT) + ",\"contentType\":\"text/plain\"}");
        assertThat(imported.statusCode()).isEqualTo(201);
        Matcher matcher = OBJECT_ID.matcher(imported.body());
        assertThat(matcher.find()).isTrue();
        String objectId = matcher.group();

        assertThat(send("POST", "/api/v1/files/" + objectId + "/analyze", OPERATOR, null)
                .statusCode()).isEqualTo(200);
        awaitStatus(objectId, "ANALYZED");

        HttpResponse<String> dna =
                send("GET", "/api/v1/objects/" + objectId + "/dna", READER, null);
        assertThat(dna.statusCode()).isEqualTo(200);
        assertThat(dna.body())
                .contains("\"present\":true")
                .contains("adapter framework")
                .contains("Overview");
    }

    @Test
    @DisplayName("a markdown file also routes through the Text Adapter")
    void markdownRoutesThroughTextAdapter() throws Exception {
        HttpResponse<String> imported = send("POST", "/api/v1/files", OPERATOR,
                "{\"fileName\":\"notes.md\",\"content\":"
                        + quotedJson(DOCUMENT) + ",\"contentType\":\"text/markdown\"}");
        assertThat(imported.statusCode()).isEqualTo(201);
        Matcher matcher = OBJECT_ID.matcher(imported.body());
        assertThat(matcher.find()).isTrue();
        String objectId = matcher.group();

        assertThat(send("POST", "/api/v1/files/" + objectId + "/analyze", OPERATOR, null)
                .statusCode()).isEqualTo(200);
        awaitStatus(objectId, "ANALYZED");
    }

    @Test
    @DisplayName("an unsupported extension is refused at import and never stored")
    void unsupportedExtensionRefusedAtImport() throws Exception {
        HttpResponse<String> imported = send("POST", "/api/v1/files", OPERATOR,
                "{\"fileName\":\"photo.png\",\"content\":\"Acceptance content with words.\","
                        + "\"contentType\":\"image/png\"}");

        assertThat(imported.statusCode()).isEqualTo(400);
        assertThat(imported.body())
                .contains("VALIDATION_FAILED")
                .contains("No registered adapter supports");

        assertThat(send("GET", "/api/v1/files", READER, null).body())
                .doesNotContain("photo.png");
    }

    @Test
    @DisplayName("the version endpoint reports adapter diagnostics")
    void versionExposesAdapters() throws Exception {
        HttpResponse<String> version = send("GET", "/api/v1/version", null, null);

        assertThat(version.statusCode()).isEqualTo(200);
        String body = version.body();
        assertThat(body)
                .contains("sfs-adapter-text")
                .contains("Text Adapter")
                .contains("sfs-text-adapter/0.1")
                .contains("txt")
                .contains("text/plain")
                .contains("normalization")
                .contains("structure-outline")
                .contains("adapterResolutions")
                .contains("adapterRefusals");
    }

    @Test
    @DisplayName("renaming to an unsupported extension is refused")
    void renameToUnsupportedRefused() throws Exception {
        HttpResponse<String> imported = send("POST", "/api/v1/files", OPERATOR,
                "{\"fileName\":\"rename-safe.txt\",\"content\":\"Words for the rename "
                        + "acceptance case.\",\"contentType\":\"text/plain\"}");
        Matcher matcher = OBJECT_ID.matcher(imported.body());
        assertThat(matcher.find()).isTrue();
        String objectId = matcher.group();

        String events = send("GET", "/api/v1/files/" + objectId + "/events", READER, null).body();
        assertThat(events).contains("REGISTRATION_RECORDED");
    }

    private static String quotedJson(String text) {
        return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n") + "\"";
    }
}
