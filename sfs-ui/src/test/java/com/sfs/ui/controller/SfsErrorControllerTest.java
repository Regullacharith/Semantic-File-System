package com.sfs.ui.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Error page")
class SfsErrorControllerTest {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @LocalServerPort
    private int port;

    private HttpResponse<String> get(String path) throws Exception {
        return send(path, "GET");
    }

    private HttpResponse<String> send(String path, String method) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(10))
                .method(method, HttpRequest.BodyPublishers.noBody())
                .build();

        return CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    @DisplayName("renders the SFS shell instead of the Whitelabel page")
    void unknownAddressRendersSfsShell() throws Exception {
        HttpResponse<String> response = get("/no-such-page");

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.body())
                .contains("Semantic File System")
                .contains("Page not found");
    }

    @Test
    @DisplayName("never shows the Whitelabel text or a framework detail")
    void neverShowsWhitelabelText() throws Exception {
        String body = get("/no-such-page").body();

        assertThat(body)
                .doesNotContain("Whitelabel")
                .doesNotContain("no explicit mapping")
                .doesNotContain("org.springframework")
                .doesNotContain("java.lang")
                .doesNotContain("Exception");
    }

    @Test
    @DisplayName("keeps the primary navigation usable")
    void errorPageKeepsNavigation() throws Exception {
        String body = get("/no-such-page").body();

        assertThat(body)
                .contains("href=\"/files\"")
                .contains("href=\"/search\"")
                .contains("href=\"/settings\"");
    }

    @Test
    @DisplayName("states that nothing was changed")
    void statesNothingWasChanged() throws Exception {
        assertThat(get("/no-such-page").body()).contains("Nothing was imported");
    }

    @Test
    @DisplayName("shows the address that was requested")
    void showsRequestedAddress() throws Exception {
        assertThat(get("/definitely-not-here").body()).contains("/definitely-not-here");
    }

    @Test
    @DisplayName("serves the styled page for an unknown object, job and report")
    void unknownResourcesUseTheErrorPage() throws Exception {
        for (String path : new String[]{
                "/objects/sfs-obj-9999-nothere",
                "/reconstruction/job/job-9999",
                "/evaluation/sfs-obj-9999-nothere"}) {

            HttpResponse<String> response = get(path);

            assertThat(response.statusCode())
                    .as("status for %s", path)
                    .isEqualTo(404);
            assertThat(response.body())
                    .as("body for %s", path)
                    .contains("Semantic File System")
                    .doesNotContain("Whitelabel");
        }
    }

    @Test
    @DisplayName("renders a styled page when a method is not allowed")
    void methodNotAllowedUsesTheErrorPage() throws Exception {
        HttpResponse<String> response = send("/settings", "POST");

        assertThat(response.statusCode()).isEqualTo(405);
        assertThat(response.body())
                .contains("Semantic File System")
                .doesNotContain("Whitelabel");
    }

    @Test
    @DisplayName("uses the local stylesheet and no external asset or script")
    void errorPageUsesLocalStylesheetOnly() throws Exception {
        String body = get("/no-such-page").body();

        assertThat(body)
                .contains("/css/sfs.css")
                .doesNotContain("<script")
                .doesNotContain("https://cdn")
                .doesNotContain("http://fonts");
    }

    @Test
    @DisplayName("escapes a requested address so it cannot inject markup")
    void escapesRequestedAddress() throws Exception {
        String body = get("/%3Cscript%3Ealert(1)%3C/script%3E").body();

        assertThat(body).doesNotContain("<script>alert(1)</script>");
    }

    @Test
    @DisplayName("keeps every real page working")
    void realPagesStillWork() throws Exception {
        for (String path : new String[]{
                "/", "/files", "/search", "/objects", "/reconstruction", "/evaluation", "/settings"}) {

            assertThat(get(path).statusCode())
                    .as("status for %s", path)
                    .isEqualTo(200);
        }
    }
}
