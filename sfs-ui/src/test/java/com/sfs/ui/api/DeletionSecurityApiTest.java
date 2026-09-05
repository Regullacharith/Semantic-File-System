package com.sfs.ui.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
@DisplayName("Deletion security over HTTP")
class DeletionSecurityApiTest {

    private static final Pattern OBJECT_ID = Pattern.compile("sfs-obj-[0-9]{4}-[a-z0-9]+");

    private static final String AUTH_HEADER = "X-SFS-Credential";
    private static final String OPERATOR = "operator";
    private static final String CUSTODIAN = "custodian";
    private static final String READER = "reader";

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @LocalServerPort
    private int port;

    @Autowired
    private Environment environment;

    private HttpRequest.Builder request(String path, String credential) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json");

        if (credential != null) {
            builder.header(AUTH_HEADER, credential);
        }
        return builder;
    }

    private HttpResponse<String> send(String method, String path, String credential, String body)
            throws Exception {

        HttpRequest.BodyPublisher publisher = body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body);

        return CLIENT.send(request(path, credential).method(method, publisher).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private String confirmBody(String objectId) {
        return "{\"confirmObjectId\":\"" + objectId + "\"}";
    }

    private String analyzedObject() throws Exception {
        HttpResponse<String> imported = send("POST", "/api/v1/files", null,
                "{\"fileName\":\"security-test.txt\",\"content\":\"Content under test.\"}");

        Matcher matcher = OBJECT_ID.matcher(imported.body());
        assertThat(matcher.find()).as("import should return an Object ID").isTrue();
        String objectId = matcher.group();

        send("POST", "/api/v1/files/" + objectId + "/analyze", null, null);
        return objectId;
    }

    private String memorizedObject() throws Exception {
        String objectId = analyzedObject();
        HttpResponse<String> memorized =
                send("POST", "/api/v1/files/" + objectId + "/memorize", CUSTODIAN, null);
        assertThat(memorized.statusCode()).isEqualTo(200);
        return objectId;
    }

    private String statusOf(String objectId) throws Exception {
        return send("GET", "/api/v1/files/" + objectId, null, null).body();
    }

    @Nested
    @DisplayName("authentication")
    class Authentication {

        @Test
        @DisplayName("rejects an unauthenticated delete with 401")
        void rejectsUnauthenticatedDelete() throws Exception {
            String objectId = analyzedObject();

            HttpResponse<String> response =
                    send("DELETE", "/api/v1/files/" + objectId, null, confirmBody(objectId));

            assertThat(response.statusCode()).isEqualTo(401);
            assertThat(response.body()).contains("AUTHENTICATION_REQUIRED");
        }

        @Test
        @DisplayName("rejects an unauthenticated purge with 401")
        void rejectsUnauthenticatedPurge() throws Exception {
            String objectId = analyzedObject();

            HttpResponse<String> response = send(
                    "POST", "/api/v1/files/" + objectId + "/purge", null, confirmBody(objectId));

            assertThat(response.statusCode()).isEqualTo(401);
        }

        @Test
        @DisplayName("leaves the object untouched when authentication fails")
        void unauthenticatedDeleteChangesNothing() throws Exception {
            String objectId = analyzedObject();

            send("DELETE", "/api/v1/files/" + objectId, null, confirmBody(objectId));

            assertThat(statusOf(objectId)).contains("\"status\":\"ANALYZED\"");
        }

        @Test
        @DisplayName("allows an authenticated and authorized delete")
        void allowsAuthenticatedDelete() throws Exception {
            String objectId = analyzedObject();

            HttpResponse<String> response =
                    send("DELETE", "/api/v1/files/" + objectId, OPERATOR, confirmBody(objectId));

            assertThat(response.statusCode()).isEqualTo(200);
        }
    }

    @Nested
    @DisplayName("authorization")
    class Authorization {

        @Test
        @DisplayName("rejects a delete by an authenticated caller lacking the capability")
        void rejectsUnauthorizedDelete() throws Exception {
            String objectId = analyzedObject();

            HttpResponse<String> response =
                    send("DELETE", "/api/v1/files/" + objectId, READER, confirmBody(objectId));

            assertThat(response.statusCode()).isEqualTo(403);
            assertThat(response.body()).contains("NOT_PERMITTED");
        }

        @Test
        @DisplayName("refuses a purge to a caller who may delete but not purge")
        void deleteCapabilityDoesNotImplyPurge() throws Exception {
            String objectId = analyzedObject();
            send("DELETE", "/api/v1/files/" + objectId, OPERATOR, confirmBody(objectId));

            HttpResponse<String> response = send(
                    "POST", "/api/v1/files/" + objectId + "/purge", OPERATOR, confirmBody(objectId));

            assertThat(response.statusCode()).isEqualTo(403);
        }

        @Test
        @DisplayName("permits a purge to a caller holding the purge capability")
        void permitsAuthorizedPurge() throws Exception {
            String objectId = memorizedObject();
            send("DELETE", "/api/v1/files/" + objectId, CUSTODIAN, confirmBody(objectId));

            HttpResponse<String> response = send(
                    "POST", "/api/v1/files/" + objectId + "/purge", CUSTODIAN, confirmBody(objectId));

            assertThat(response.statusCode()).isEqualTo(200);
        }
    }

    @Nested
    @DisplayName("confirmation")
    class Confirmation {

        @Test
        @DisplayName("rejects a delete with no confirmation body")
        void rejectsMissingConfirmation() throws Exception {
            String objectId = analyzedObject();

            HttpResponse<String> response =
                    send("DELETE", "/api/v1/files/" + objectId, OPERATOR, null);

            assertThat(response.statusCode()).isEqualTo(400);
            assertThat(response.body()).contains("CONFIRMATION_REQUIRED");
        }

        @Test
        @DisplayName("rejects a confirmation naming a different object")
        void rejectsMismatchedConfirmation() throws Exception {
            String objectId = analyzedObject();

            HttpResponse<String> response = send("DELETE", "/api/v1/files/" + objectId,
                    OPERATOR, confirmBody("sfs-obj-0001-a1b2c3d4"));

            assertThat(response.statusCode()).isEqualTo(400);
            assertThat(response.body()).contains("CONFIRMATION_MISMATCH");
        }

        @Test
        @DisplayName("leaves the object untouched when confirmation is missing")
        void missingConfirmationChangesNothing() throws Exception {
            String objectId = analyzedObject();

            send("DELETE", "/api/v1/files/" + objectId, OPERATOR, null);

            assertThat(statusOf(objectId)).contains("\"status\":\"ANALYZED\"");
        }

        @Test
        @DisplayName("accepts a confirmation naming the correct object")
        void acceptsMatchingConfirmation() throws Exception {
            String objectId = analyzedObject();

            assertThat(send("DELETE", "/api/v1/files/" + objectId, OPERATOR, confirmBody(objectId))
                    .statusCode()).isEqualTo(200);
        }
    }

    @Nested
    @DisplayName("reversible deletion and purge")
    class ReversibleDeletion {

        @Test
        @DisplayName("delete leaves the object recoverable with raw bytes retained")
        void deleteIsReversible() throws Exception {
            String objectId = analyzedObject();
            send("DELETE", "/api/v1/files/" + objectId, OPERATOR, confirmBody(objectId));

            String body = statusOf(objectId);

            assertThat(body).contains("\"status\":\"SOFT_DELETED\"");
            assertThat(body).contains("\"rawDataRemoved\":false");
        }

        @Test
        @DisplayName("restores a deleted object")
        void restoresDeletedObject() throws Exception {
            String objectId = analyzedObject();
            send("DELETE", "/api/v1/files/" + objectId, OPERATOR, confirmBody(objectId));

            HttpResponse<String> undo = send(
                    "POST", "/api/v1/files/" + objectId + "/undo-delete", OPERATOR, null);

            assertThat(undo.statusCode()).isEqualTo(200);
            assertThat(statusOf(objectId)).contains("\"status\":\"ANALYZED\"");
        }

        @Test
        @DisplayName("purge releases raw bytes and keeps the semantic record")
        void purgeReleasesRawBytes() throws Exception {
            String objectId = memorizedObject();
            send("DELETE", "/api/v1/files/" + objectId, CUSTODIAN, confirmBody(objectId));
            send("POST", "/api/v1/files/" + objectId + "/purge", CUSTODIAN, confirmBody(objectId));

            String body = statusOf(objectId);

            assertThat(body).contains("\"status\":\"MEMORIZED\"");
            assertThat(body).contains("\"rawDataRemoved\":true");
        }

        @Test
        @DisplayName("refuses to purge a live object in a single step")
        void refusesSingleStepPurge() throws Exception {
            String objectId = analyzedObject();

            HttpResponse<String> response = send(
                    "POST", "/api/v1/files/" + objectId + "/purge", CUSTODIAN, confirmBody(objectId));

            assertThat(response.statusCode()).isEqualTo(409);
            assertThat(statusOf(objectId)).contains("\"status\":\"ANALYZED\"");
        }

        @Test
        @DisplayName("refuses to restore an object after purge")
        void refusesUndoAfterPurge() throws Exception {
            String objectId = memorizedObject();
            send("DELETE", "/api/v1/files/" + objectId, CUSTODIAN, confirmBody(objectId));
            send("POST", "/api/v1/files/" + objectId + "/purge", CUSTODIAN, confirmBody(objectId));

            HttpResponse<String> undo = send(
                    "POST", "/api/v1/files/" + objectId + "/undo-delete", CUSTODIAN, null);

            assertThat(undo.statusCode()).isEqualTo(409);
        }
    }

    @Nested
    @DisplayName("no unguarded destructive path")
    class NoUnguardedPath {

        @Test
        @DisplayName("offers no single-call endpoint that releases raw bytes")
        void noSingleCallDestructiveEndpoint() throws Exception {
            String objectId = analyzedObject();

            HttpResponse<String> response = send(
                    "POST", "/api/v1/files/" + objectId + "/delete-raw", CUSTODIAN, null);

            assertThat(response.statusCode())
                    .as("a single-call destructive endpoint must not be routable")
                    .isIn(404, 405);
            assertThat(statusOf(objectId)).contains("\"status\":\"ANALYZED\"");
        }
    }

    @Nested
    @DisplayName("deployment restriction")
    class DeploymentRestriction {

        @Test
        @DisplayName("the application is configured to bind to loopback")
        void configuredForLoopback() {
            assertThat(environment.getProperty("server.address")).isEqualTo("127.0.0.1");
        }
    }
}
