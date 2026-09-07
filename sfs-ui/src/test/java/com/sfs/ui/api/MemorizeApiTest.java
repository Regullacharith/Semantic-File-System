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
@DisplayName("Memorize over HTTP")
class MemorizeApiTest {

    private static final Pattern OBJECT_ID = Pattern.compile("sfs-obj-[0-9]{4}-[a-z0-9]+");

    private static final String AUTH_HEADER = "X-SFS-Credential";
    private static final String OPERATOR = "operator";
    private static final String READER = "reader";

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

    private String importAndAnalyze() throws Exception {
        String importBody = "{\"fileName\":\"memorize-me.txt\",\"content\":\"Memorization "
                + "integration test content for the lifecycle manager.\","
                + "\"contentType\":\"text/plain\"}";
        HttpResponse<String> imported = send("POST", "/api/v1/files", OPERATOR, importBody);
        assertThat(imported.statusCode()).isEqualTo(201);

        Matcher matcher = OBJECT_ID.matcher(imported.body());
        assertThat(matcher.find()).isTrue();
        String objectId = matcher.group();

        HttpResponse<String> analyzed = send("POST", "/api/v1/files/" + objectId + "/analyze",
                OPERATOR, null);
        assertThat(analyzed.statusCode()).isEqualTo(200);
        return objectId;
    }

    @Nested
    @DisplayName("security")
    class Security {

        @Test
        @DisplayName("memorization without a credential is unauthenticated")
        void unauthenticatedMemorizationRejected() throws Exception {
            String objectId = importAndAnalyze();

            HttpResponse<String> response =
                    send("POST", "/api/v1/files/" + objectId + "/memorize", null, null);

            assertThat(response.statusCode()).isEqualTo(401);
            assertThat(response.body()).contains("AUTHENTICATION_REQUIRED");
        }

        @Test
        @DisplayName("memorization without the capability is not permitted")
        void unauthorizedMemorizationRejected() throws Exception {
            String objectId = importAndAnalyze();

            HttpResponse<String> response =
                    send("POST", "/api/v1/files/" + objectId + "/memorize", READER, null);

            assertThat(response.statusCode()).isEqualTo(403);
            assertThat(response.body()).contains("NOT_PERMITTED");
        }
    }

    @Nested
    @DisplayName("lifecycle")
    class Lifecycle {

        @Test
        @DisplayName("an analyzed object can be memorized by an authorized principal")
        void memorizeSucceeds() throws Exception {
            String objectId = importAndAnalyze();

            HttpResponse<String> response =
                    send("POST", "/api/v1/files/" + objectId + "/memorize", OPERATOR, null);

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).contains("\"successful\":true");

            HttpResponse<String> file =
                    send("GET", "/api/v1/files/" + objectId, OPERATOR, null);
            assertThat(file.body()).contains("MEMORY_COMMITTED");
        }

        @Test
        @DisplayName("repeated memorization is refused as a lifecycle conflict")
        void repeatedMemorizationRefused() throws Exception {
            String objectId = importAndAnalyze();
            send("POST", "/api/v1/files/" + objectId + "/memorize", OPERATOR, null);

            HttpResponse<String> response =
                    send("POST", "/api/v1/files/" + objectId + "/memorize", OPERATOR, null);

            assertThat(response.statusCode()).isEqualTo(409);
            assertThat(response.body()).contains("INVALID_STATE_TRANSITION");
        }

        @Test
        @DisplayName("a registered object cannot be memorized before analysis")
        void registeredObjectCannotMemorize() throws Exception {
            String importBody = "{\"fileName\":\"not-analyzed.txt\",\"content\":\"Registered "
                    + "only.\",\"contentType\":\"text/plain\"}";
            HttpResponse<String> imported = send("POST", "/api/v1/files", OPERATOR, importBody);
            Matcher matcher = OBJECT_ID.matcher(imported.body());
            assertThat(matcher.find()).isTrue();
            String objectId = matcher.group();

            HttpResponse<String> response =
                    send("POST", "/api/v1/files/" + objectId + "/memorize", OPERATOR, null);

            assertThat(response.statusCode()).isEqualTo(409);
            assertThat(response.body()).contains("INVALID_STATE_TRANSITION");
        }

        @Test
        @DisplayName("memorizing an unknown object is not found")
        void unknownObjectNotFound() throws Exception {
            HttpResponse<String> response = send("POST",
                    "/api/v1/files/sfs-obj-9999-ffffffff/memorize", OPERATOR, null);

            assertThat(response.statusCode()).isEqualTo(404);
            assertThat(response.body()).contains("FILE_NOT_FOUND");
        }
    }
}
