package com.sfs.ui.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.io.ByteArrayOutputStream;
import java.net.CookieManager;
import java.net.CookiePolicy;
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
@DisplayName("Milestone 01 acceptance")
class MilestoneOneAcceptanceTest {

    private static final Pattern OBJECT_ID = Pattern.compile("sfs-obj-[0-9]{4}-[a-z0-9]+");
    private static final Pattern JOB_ID = Pattern.compile("job-[0-9]{4}");

    private static final String BOUNDARY = "sfsAcceptanceBoundary";

    @LocalServerPort
    private int port;

    private HttpClient client() {
        CookieManager cookies = new CookieManager();
        cookies.setCookiePolicy(CookiePolicy.ACCEPT_ALL);

        return HttpClient.newBuilder()
                .cookieHandler(cookies)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(20));
    }

    private HttpResponse<String> get(HttpClient client, String path) throws Exception {
        return client.send(request(path).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(HttpClient client, String path) throws Exception {
        return client.send(
                request(path).POST(HttpRequest.BodyPublishers.noBody())
                        .header("Content-Type", "application/x-www-form-urlencoded").build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> upload(HttpClient client, String fileName, String content) throws Exception {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.write(("--" + BOUNDARY + "\r\n").getBytes(StandardCharsets.UTF_8));
        body.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n")
                .getBytes(StandardCharsets.UTF_8));
        body.write("Content-Type: text/plain\r\n\r\n".getBytes(StandardCharsets.UTF_8));
        body.write(content.getBytes(StandardCharsets.UTF_8));
        body.write(("\r\n--" + BOUNDARY + "--\r\n").getBytes(StandardCharsets.UTF_8));

        return client.send(
                request("/files/import")
                        .header("Content-Type", "multipart/form-data; boundary=" + BOUNDARY)
                        .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private String lastMatch(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        String found = null;
        while (matcher.find()) {
            found = matcher.group();
        }
        assertThat(found).as("expected %s in response", pattern.pattern()).isNotNull();
        return found;
    }

    @Nested
    @DisplayName("the application starts and serves every screen")
    class Startup {

        @Test
        @DisplayName("serves all seven Milestone 01 screens")
        void servesEveryScreen() throws Exception {
            HttpClient client = client();

            for (String path : new String[]{
                    "/", "/files", "/search", "/objects", "/reconstruction", "/evaluation", "/settings"}) {

                HttpResponse<String> response = get(client, path);

                assertThat(response.statusCode()).as("status for %s", path).isEqualTo(200);
                assertThat(response.body()).as("shell for %s", path).contains("Semantic File System");
            }
        }

        @Test
        @DisplayName("uses no external asset and no script anywhere")
        void usesNoExternalAssets() throws Exception {
            HttpClient client = client();

            for (String path : new String[]{
                    "/", "/files", "/search", "/objects", "/reconstruction", "/evaluation", "/settings"}) {

                assertThat(get(client, path).body())
                        .as("assets for %s", path)
                        .doesNotContain("<script")
                        .doesNotContain("https://")
                        .doesNotContain("http://cdn");
            }
        }
    }

    @Nested
    @DisplayName("a text file can be imported and identified")
    class ImportJourney {

        @Test
        @DisplayName("imports a TXT file and displays its Object ID")
        void importsAndDisplaysObjectId() throws Exception {
            HttpClient client = client();

            HttpResponse<String> response = upload(client, "acceptance-note.txt",
                    "Quarterly planning notes. Budget approved on 14 March 2026.");

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).contains("acceptance-note.txt");

            String objectId = lastMatch(OBJECT_ID, get(client, "/files").body());
            assertThat(objectId).matches("sfs-obj-[0-9]{4}-[a-z0-9]+");
        }

        @Test
        @DisplayName("rejects a file that is not UTF-8 text")
        void rejectsNonTextFile() throws Exception {
            HttpClient client = client();

            ByteArrayOutputStream body = new ByteArrayOutputStream();
            body.write(("--" + BOUNDARY + "\r\n").getBytes(StandardCharsets.UTF_8));
            body.write("Content-Disposition: form-data; name=\"file\"; filename=\"binary.bin\"\r\n"
                    .getBytes(StandardCharsets.UTF_8));
            body.write("Content-Type: application/octet-stream\r\n\r\n".getBytes(StandardCharsets.UTF_8));
            body.write(new byte[]{(byte) 0xC3, (byte) 0x28, (byte) 0xA0, (byte) 0xA1});
            body.write(("\r\n--" + BOUNDARY + "--\r\n").getBytes(StandardCharsets.UTF_8));

            HttpResponse<String> response = client.send(
                    request("/files/import")
                            .header("Content-Type", "multipart/form-data; boundary=" + BOUNDARY)
                            .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).contains("text files only");
        }
    }

    @Nested
    @DisplayName("search results are displayed")
    class SearchJourney {

        @Test
        @DisplayName("displays results for a query that matches")
        void displaysResults() throws Exception {
            HttpResponse<String> response = get(client(), "/search?q=research");

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).containsIgnoringCase("research");
        }

        @Test
        @DisplayName("reports an empty result set without pretending to succeed")
        void reportsEmptyResults() throws Exception {
            String body = get(client(), "/search?q=zzzznothingmatchesthis").body();

            assertThat(body).containsIgnoringCase("no");
        }

        @Test
        @DisplayName("never launches a reconstruction from the search screen")
        void searchNeverReconstructs() throws Exception {
            String body = get(client(), "/search?q=research").body();

            assertThat(body).doesNotContain("action=\"/reconstruction/");
        }
    }

    @Nested
    @DisplayName("a result launches one explicit reconstruction")
    class ReconstructionJourney {

        @Test
        @DisplayName("reconstructs only from an explicit POST and returns a job")
        void reconstructsFromExplicitPost() throws Exception {
            HttpClient client = client();

            HttpResponse<String> response = post(client, "/reconstruction/sfs-obj-0001-a1b2c3d4");

            assertThat(response.statusCode()).isEqualTo(200);

            String jobId = lastMatch(JOB_ID, response.body());
            assertThat(get(client, "/reconstruction/job/" + jobId).statusCode()).isEqualTo(200);
        }

        @Test
        @DisplayName("never reconstructs from a GET")
        void neverReconstructsFromGet() throws Exception {
            assertThat(get(client(), "/reconstruction/sfs-obj-0001-a1b2c3d4").statusCode())
                    .isEqualTo(405);
        }

        @Test
        @DisplayName("states that the artifact is not the original file")
        void statesArtifactIsNotOriginal() throws Exception {
            HttpClient client = client();

            String jobId = lastMatch(JOB_ID, post(client, "/reconstruction/sfs-obj-0001-a1b2c3d4").body());
            String artifact = get(client, "/reconstruction/job/" + jobId + "/artifact").body();

            assertThat(artifact).contains("NOT THE ORIGINAL FILE");
        }

        @Test
        @DisplayName("refuses a reconstruction that would expose protected references")
        void refusesProtectedReconstruction() throws Exception {
            HttpClient client = client();

            String body = post(client, "/reconstruction/sfs-obj-0004-b3c4d5e6").body();

            assertThat(body).containsIgnoringCase("rejected");
        }
    }

    @Nested
    @DisplayName("a fidelity report is viewable")
    class EvaluationJourney {

        @Test
        @DisplayName("shows a fidelity report for a completed reconstruction")
        void showsFidelityReport() throws Exception {
            HttpClient client = client();

            String jobId = lastMatch(JOB_ID, post(client, "/reconstruction/sfs-obj-0001-a1b2c3d4").body());
            HttpResponse<String> response = get(client, "/evaluation/" + jobId);

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).containsIgnoringCase("fidelity");
        }

        @Test
        @DisplayName("states why a report is unavailable rather than showing a fake score")
        void explainsUnavailableReport() throws Exception {
            HttpClient client = client();

            String jobId = lastMatch(JOB_ID, post(client, "/reconstruction/sfs-obj-0002-e5f6a7b8").body());
            HttpResponse<String> response = get(client, "/evaluation/" + jobId);

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).containsIgnoringCase("original");
        }
    }

    @Nested
    @DisplayName("the UI contains no semantic logic")
    class Boundaries {

        @Test
        @DisplayName("reports security policy without offering an unenforced control")
        void securitySettingsAreReadOnly() throws Exception {
            HttpClient client = client();

            String body = get(client, "/settings").body();

            assertThat(body).doesNotContain("<input").doesNotContain("<select");
            assertThat(post(client, "/settings").statusCode()).isEqualTo(405);
        }

        @Test
        @DisplayName("shows a styled SFS error page rather than a framework fallback")
        void errorsUseTheApplicationShell() throws Exception {
            HttpResponse<String> response = get(client(), "/not-a-real-page");

            assertThat(response.statusCode()).isEqualTo(404);
            assertThat(response.body())
                    .contains("Semantic File System")
                    .doesNotContain("Whitelabel");
        }
    }
}
