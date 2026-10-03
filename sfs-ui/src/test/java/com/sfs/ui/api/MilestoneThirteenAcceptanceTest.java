package com.sfs.ui.api;

import com.sfs.contracts.security.Principal;
import com.sfs.security.SecretResolutionDeniedException;
import com.sfs.security.SecretResolutionService;
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
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Milestone 13 acceptance: secrets stay separated from semantics")
class MilestoneThirteenAcceptanceTest {

    private static final Pattern OBJECT_ID = Pattern.compile("sfs-obj-[0-9a-f-]+");
    private static final Pattern REFERENCE_ID = Pattern.compile("sfs-ref-[0-9a-f]+");

    private static final String PASSWORD = "hunter2";
    private static final String API_KEY = "sk-live-9f8e7d6c5b4a3210";

    private static final String SECRET_DOCUMENT = """
            # Deployment

            The deployment notes describe the production platform for 2026.

            # Credentials

            password=hunter2
            api_key=sk-live-9f8e7d6c5b4a3210

            # Contacts

            Report issues to ops@example.com or call +1 415 555 0123.
            Account number: XJ44-90881
            """;

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @LocalServerPort
    private int port;

    @Autowired
    private SecretResolutionService secretResolutionService;

    @Autowired
    private com.sfs.contracts.security.AuthenticationService authenticationService;

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

    private String importedAnalyzedObject() throws Exception {
        String boundary = "sfs-m13-boundary";
        java.io.ByteArrayOutputStream multipart = new java.io.ByteArrayOutputStream();
        multipart.write(("--" + boundary + "\r\nContent-Disposition: form-data; "
                + "name=\"file\"; filename=\"deployment-notes.txt\"\r\n"
                + "Content-Type: text/plain\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        multipart.write(SECRET_DOCUMENT.getBytes(StandardCharsets.UTF_8));
        multipart.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

        HttpResponse<String> imported = CLIENT.send(HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/files/import"))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .header("X-SFS-Credential", "operator")
                .POST(HttpRequest.BodyPublishers.ofByteArray(multipart.toByteArray()))
                .build(), HttpResponse.BodyHandlers.ofString());
        assertThat(imported.statusCode()).isEqualTo(200);

        Matcher matcher = OBJECT_ID.matcher(imported.body());
        assertThat(matcher.find()).isTrue();
        String objectId = matcher.group();

        send("POST", "/api/v1/files/" + objectId + "/analyze", "operator", null);
        long deadline = System.currentTimeMillis() + 20_000;
        while (System.currentTimeMillis() < deadline) {
            String file = send("GET", "/api/v1/files/" + objectId, "operator",
                    null).body();
            if (file.contains("\"status\":\"ANALYZED\"")
                    || file.contains("\"status\":\"MEMORIZABLE\"")) {
                return objectId;
            }
            Thread.sleep(50);
        }
        throw new AssertionError("analysis never completed for " + objectId);
    }

    @Test
    @DisplayName("synthetic secrets are absent from ordinary DNA and search")
    void secretsAbsentFromDnaAndSearch() throws Exception {
        String objectId = importedAnalyzedObject();

        HttpResponse<String> dna = send("GET", "/api/v1/objects/" + objectId + "/dna",
                "operator", null);
        assertThat(dna.statusCode()).isEqualTo(200);
        String dnaBody = dna.body();
        assertThat(dnaBody)
                .doesNotContain(PASSWORD)
                .doesNotContain(API_KEY)
                .doesNotContain("ops@example.com")
                .doesNotContain("+1 415 555 0123")
                .doesNotContain("XJ44-90881");
        assertThat(dnaBody).contains("protectedReferences");

        HttpResponse<String> search = send("POST", "/api/v1/search", "operator",
                "{\"text\":\"deployment credentials password platform\",\"maxResults\":10}");
        assertThat(search.statusCode()).isEqualTo(200);
        assertThat(search.body()).doesNotContain(PASSWORD);
        assertThat(search.body()).doesNotContain(API_KEY);
    }

    @Test
    @DisplayName("authorized access resolves the encrypted reference; unauthorized is denied")
    void resolutionUnderAuthorization() throws Exception {
        String objectId = importedAnalyzedObject();

        String dnaBody = send("GET", "/api/v1/objects/" + objectId + "/dna",
                "operator", null).body();
        Matcher matcher = Pattern
                .compile("\"referenceId\":\"(sfs-ref-[0-9a-f]+)\",\"sensitiveType\":\"API_KEY\"")
                .matcher(dnaBody);
        assertThat(matcher.find()).isTrue();
        String referenceId = matcher.group(1);

        Principal custodian = authenticationService.authenticate("custodian")
                .orElseThrow();
        Principal operator = authenticationService.authenticate("operator")
                .orElseThrow();

        assertThat(secretResolutionService.resolve(referenceId, custodian))
                .isEqualTo(API_KEY);

        assertThatThrownBy(() -> secretResolutionService.resolve(referenceId, operator))
                .isInstanceOf(SecretResolutionDeniedException.class);
    }

    @Test
    @DisplayName("the password is never resolvable for anyone")
    void passwordNeverResolvable() {
        Principal custodian = authenticationService.authenticate("custodian")
                .orElseThrow();

        assertThatThrownBy(() -> secretResolutionService.resolve(
                "sfs-ref-doesnotexist00", custodian))
                .isInstanceOf(SecretResolutionDeniedException.class)
                .hasMessageContaining("No stored secret");
    }

    @Test
    @DisplayName("the settings page and API report real policies and audit events")
    void settingsReportRealState() throws Exception {
        HttpResponse<String> settings = send("GET", "/api/v1/security/settings",
                "reader", null);

        assertThat(settings.statusCode()).isEqualTo(200);
        String body = settings.body();
        assertThat(body)
                .contains("PASSWORD")
                .contains("REDACT")
                .contains("embeddingsExcludeSecrets\":true")
                .contains("logsExcludeSecrets\":true")
                .contains("dnaExcludeSecrets\":true")
                .contains("authorizationRequired\":true")
                .contains("master key");

        HttpResponse<String> page = CLIENT.send(HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/settings"))
                .GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(page.statusCode()).isEqualTo(200);
    }

    @Test
    @DisplayName("the version endpoint reports the security subsystem")
    void versionReportsSecurity() throws Exception {
        HttpResponse<String> version = send("GET", "/api/v1/version", null, null);

        assertThat(version.statusCode()).isEqualTo(200);
        assertThat(version.body())
                .contains("\"milestone\":\"M14")
                .contains("\"security\":{")
                .contains("sfs-security/0.1")
                .contains("security-privacy")
                .contains("passwordPolicy")
                .contains("storedSecrets");
    }
}
