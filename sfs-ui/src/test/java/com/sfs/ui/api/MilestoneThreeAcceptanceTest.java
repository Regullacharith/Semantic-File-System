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
@DisplayName("Milestone 03 acceptance: file lifecycle manager over HTTP")
class MilestoneThreeAcceptanceTest {

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

    private String confirmBody(String objectId) {
        return "{\"confirmObjectId\":\"" + objectId + "\"}";
    }

    private String importObject(String fileName) throws Exception {
        HttpResponse<String> imported = send("POST", "/api/v1/files", OPERATOR,
                "{\"fileName\":\"" + fileName + "\",\"content\":\"Milestone three "
                        + "acceptance content for " + fileName + ".\",\"contentType\":\"text/plain\"}");
        assertThat(imported.statusCode()).isEqualTo(201);
        Matcher matcher = OBJECT_ID.matcher(imported.body());
        assertThat(matcher.find()).isTrue();
        return matcher.group();
    }

    @Test
    @DisplayName("a file receives a stable Object ID that never depends on its name")
    void stableObjectId() throws Exception {
        String objectId = importObject("stable-id.txt");

        assertThat(objectId).matches("sfs-obj-[0-9]{4}-[a-z0-9]+");
        assertThat(send("GET", "/api/v1/files/" + objectId, READER, null).body())
                .contains(objectId)
                .contains("stable-id.txt");
    }

    @Test
    @DisplayName("only valid lifecycle transitions occur")
    void onlyValidTransitions() throws Exception {
        String objectId = importObject("transitions.txt");

        assertThat(send("POST", "/api/v1/files/" + objectId + "/memorize", OPERATOR, null)
                .statusCode()).isEqualTo(409);
        assertThat(send("DELETE", "/api/v1/files/" + objectId, OPERATOR, confirmBody(objectId))
                .statusCode()).isEqualTo(409);

        assertThat(send("POST", "/api/v1/files/" + objectId + "/analyze", OPERATOR, null)
                .statusCode()).isEqualTo(200);
        assertThat(send("POST", "/api/v1/files/" + objectId + "/analyze", OPERATOR, null)
                .statusCode()).isEqualTo(409);
        assertThat(send("POST", "/api/v1/files/" + objectId + "/undo-delete", OPERATOR, null)
                .statusCode()).isEqualTo(409);
    }

    @Test
    @DisplayName("raw deletion is impossible before the memory commit, and the gated path succeeds")
    void rawDeletionGate() throws Exception {
        String objectId = importObject("gated.txt");
        send("POST", "/api/v1/files/" + objectId + "/analyze", OPERATOR, null);

        assertThat(send("POST", "/api/v1/files/" + objectId + "/purge", CUSTODIAN,
                confirmBody(objectId)).statusCode()).isEqualTo(409);

        assertThat(send("DELETE", "/api/v1/files/" + objectId, OPERATOR, confirmBody(objectId))
                .statusCode()).isEqualTo(200);
        HttpResponse<String> gatedPurge = send("POST", "/api/v1/files/" + objectId + "/purge",
                CUSTODIAN, confirmBody(objectId));
        assertThat(gatedPurge.statusCode()).isEqualTo(409);
        assertThat(gatedPurge.body()).contains("INVALID_STATE_TRANSITION");
        assertThat(send("GET", "/api/v1/files/" + objectId, READER, null).body())
                .contains("\"rawDataRemoved\":false");

        assertThat(send("POST", "/api/v1/files/" + objectId + "/undo-delete", OPERATOR, null)
                .statusCode()).isEqualTo(200);
        assertThat(send("POST", "/api/v1/files/" + objectId + "/memorize", OPERATOR, null)
                .statusCode()).isEqualTo(200);
        assertThat(send("DELETE", "/api/v1/files/" + objectId, OPERATOR, confirmBody(objectId))
                .statusCode()).isEqualTo(200);
        assertThat(send("POST", "/api/v1/files/" + objectId + "/purge", CUSTODIAN,
                confirmBody(objectId)).statusCode()).isEqualTo(200);
        assertThat(send("GET", "/api/v1/files/" + objectId, READER, null).body())
                .contains("\"status\":\"MEMORIZED\"")
                .contains("\"rawDataRemoved\":true");
    }

    @Test
    @DisplayName("a deleted record remains address-independent and auditable")
    void deletedRecordRemainsAddressable() throws Exception {
        String objectId = importObject("purged.txt");
        send("POST", "/api/v1/files/" + objectId + "/analyze", OPERATOR, null);
        send("POST", "/api/v1/files/" + objectId + "/memorize", OPERATOR, null);
        send("DELETE", "/api/v1/files/" + objectId, OPERATOR, confirmBody(objectId));
        send("POST", "/api/v1/files/" + objectId + "/purge", CUSTODIAN, confirmBody(objectId));

        HttpResponse<String> record = send("GET", "/api/v1/files/" + objectId, READER, null);
        assertThat(record.statusCode()).isEqualTo(200);
        assertThat(record.body())
                .contains(objectId)
                .contains("purged.txt")
                .contains("\"status\":\"MEMORIZED\"");

        HttpResponse<String> events =
                send("GET", "/api/v1/files/" + objectId + "/events", READER, null);
        assertThat(events.statusCode()).isEqualTo(200);
        assertThat(events.body())
                .contains("REGISTRATION_RECORDED")
                .contains("MEMORY_COMMITTED")
                .contains("PURGE_REQUESTED")
                .contains("RAW_RELEASED");
    }

    @Test
    @DisplayName("lifecycle statistics expose event counts and captured durations")
    void observabilityCapturesMeasurements() throws Exception {
        String objectId = importObject("observed.txt");
        send("POST", "/api/v1/files/" + objectId + "/analyze", OPERATOR, null);
        send("POST", "/api/v1/files/" + objectId + "/memorize", OPERATOR, null);

        HttpResponse<String> stats = send("GET", "/api/v1/meta/lifecycle", READER, null);
        assertThat(stats.statusCode()).isEqualTo(200);
        assertThat(stats.body())
                .contains("totalEvents")
                .contains("memoryCommitCount")
                .contains("averageMemoryCommitMillis")
                .contains("averagePurgeReleaseMillis");
    }

    @Test
    @DisplayName("destructive operations remain authenticated, authorized and confirmed")
    void securityBoundariesHold() throws Exception {
        String objectId = importObject("guarded.txt");

        assertThat(send("DELETE", "/api/v1/files/" + objectId, null, confirmBody(objectId))
                .statusCode()).isEqualTo(401);
        assertThat(send("POST", "/api/v1/files/" + objectId + "/memorize", READER, null)
                .statusCode()).isEqualTo(403);
        assertThat(send("DELETE", "/api/v1/files/" + objectId, OPERATOR, null)
                .statusCode()).isEqualTo(400);
        assertThat(send("DELETE", "/api/v1/files/" + objectId, OPERATOR,
                confirmBody("sfs-obj-9999-ffffffff")).statusCode()).isEqualTo(400);
        assertThat(send("GET", "/api/v1/files/" + objectId, READER, null).body())
                .contains("\"status\":\"REGISTERED\"");
    }
}
