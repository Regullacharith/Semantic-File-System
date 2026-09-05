package com.sfs.ui.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
@DisplayName("Lifecycle audit over HTTP")
class LifecycleAuditApiTest {

    private static final Pattern OBJECT_ID = Pattern.compile("sfs-obj-[0-9]{4}-[a-z0-9]+");

    private static final String AUTH_HEADER = "X-SFS-Credential";
    private static final String OPERATOR = "operator";

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @LocalServerPort
    private int port;

    @Autowired
    private org.springframework.core.env.Environment environment;

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

    private String importedObject() throws Exception {
        String importBody = "{\"fileName\":\"audited.txt\",\"content\":\"Audit trail "
                + "integration test.\",\"contentType\":\"text/plain\"}";
        HttpResponse<String> imported = send("POST", "/api/v1/files", OPERATOR, importBody);
        Matcher matcher = OBJECT_ID.matcher(imported.body());
        assertThat(matcher.find()).isTrue();
        return matcher.group();
    }

    @Nested
    @DisplayName("event audit")
    class EventAudit {

        @Test
        @DisplayName("unauthenticated audit access is rejected")
        void unauthenticatedRejected() throws Exception {
            HttpResponse<String> response =
                    send("GET", "/api/v1/files/sfs-obj-0001-a1b2c3d4/events", null, null);

            assertThat(response.statusCode()).isEqualTo(401);
            assertThat(response.body()).contains("AUTHENTICATION_REQUIRED");
        }

        @Test
        @DisplayName("a registered reader may read the audit trail")
        void readerMayRead() throws Exception {
            HttpResponse<String> response =
                    send("GET", "/api/v1/files/sfs-obj-0001-a1b2c3d4/events", "reader", null);

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).contains("REGISTRATION_RECORDED");
        }

        @Test
        @DisplayName("the audit trail of an imported object records registration and analysis")
        void importedObjectTrail() throws Exception {
            String objectId = importedObject();
            send("POST", "/api/v1/files/" + objectId + "/analyze", OPERATOR, null);
            send("POST", "/api/v1/files/" + objectId + "/memorize", OPERATOR, null);

            HttpResponse<String> response =
                    send("GET", "/api/v1/files/" + objectId + "/events", OPERATOR, null);

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body())
                    .contains("REGISTRATION_RECORDED")
                    .contains("ANALYSIS_STARTED")
                    .contains("ANALYSIS_SUCCEEDED")
                    .contains("MEMORY_COMMITTED");
        }

        @Test
        @DisplayName("audit of an unknown object is not found")
        void unknownObjectNotFound() throws Exception {
            HttpResponse<String> response =
                    send("GET", "/api/v1/files/sfs-obj-9999-ffffffff/events", OPERATOR, null);

            assertThat(response.statusCode()).isEqualTo(404);
            assertThat(response.body()).contains("FILE_NOT_FOUND");
        }
    }

    @Nested
    @DisplayName("statistics")
    class Statistics {

        @Test
        @DisplayName("unauthenticated statistics access is rejected")
        void unauthenticatedRejected() throws Exception {
            HttpResponse<String> response =
                    send("GET", "/api/v1/meta/lifecycle", null, null);

            assertThat(response.statusCode()).isEqualTo(401);
            assertThat(response.body()).contains("AUTHENTICATION_REQUIRED");
        }

        @Test
        @DisplayName("statistics are available to an authenticated reader")
        void statisticsAvailable() throws Exception {
            HttpResponse<String> response =
                    send("GET", "/api/v1/meta/lifecycle", "reader", null);

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body())
                    .contains("totalEvents")
                    .contains("eventCounts")
                    .contains("memoryCommitCount");
        }
    }
}
