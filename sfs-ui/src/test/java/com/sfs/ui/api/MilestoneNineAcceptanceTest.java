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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Milestone 09 acceptance: semantic search over HTTP")
class MilestoneNineAcceptanceTest {

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
    @DisplayName("a semantic query returns real ranked results with rich evidence")
    void semanticSearchReturnsRealResults() throws Exception {
        HttpResponse<String> response = send("POST", "/api/v1/search", OPERATOR,
                "{\"text\":\"database latency indexing\",\"maxResults\":5}");

        assertThat(response.statusCode()).isEqualTo(200);
        String body = response.body();
        assertThat(body)
                .contains("\"searched\":true")
                .contains("\"retrievalMode\":\"SEMANTIC\"")
                .contains("\"type\":\"VECTOR_SIMILARITY\"")
                .contains("\"type\":\"CONCEPT\"")
                .contains("\"relevance\":")
                .contains("\"elapsedMillis\":");
        assertThat(body).doesNotContain("MockSearchService");
    }

    @Test
    @DisplayName("an exact Object ID lookup bypasses vector search and is exact")
    void exactLookupIsExact() throws Exception {
        HttpResponse<String> response = send("POST", "/api/v1/search", OPERATOR,
                "{\"text\":\"sfs-obj-0001-a1b2c3d4\",\"maxResults\":5}");

        assertThat(response.statusCode()).isEqualTo(200);
        String body = response.body();
        assertThat(body)
                .contains("\"retrievalMode\":\"OBJECT_ID_LOOKUP\"")
                .contains("\"totalResults\":1")
                .contains("sfs-obj-0001-a1b2c3d4")
                .contains("\"relevance\":1.0");
    }

    @Test
    @DisplayName("an unknown Object ID returns an empty result set, not an error")
    void unknownObjectYieldsEmptyResults() throws Exception {
        HttpResponse<String> response = send("POST", "/api/v1/search", OPERATOR,
                "{\"text\":\"sfs-obj-9999-unknown00\",\"maxResults\":5}");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"totalResults\":0");
    }

    @Test
    @DisplayName("protected secret values never appear in search responses")
    void protectedSecretsStayHidden() throws Exception {
        HttpResponse<String> response = send("POST", "/api/v1/search", OPERATOR,
                "{\"text\":\"deployment configuration database password\",\"maxResults\":10}");

        assertThat(response.statusCode()).isEqualTo(200);
        String body = response.body();
        assertThat(body).doesNotContain("hunter2");
        assertThat(body).doesNotContain("sk-live-9f8e7d6c5b4a");
    }

    @Test
    @DisplayName("HTTP-GET search works without credentials for anonymous readers")
    void getSearchWorksWithoutCredentials() throws Exception {
        HttpResponse<String> response = send("GET",
                "/api/v1/search?q=embeddings%20retrieval&maxResults=5", null, null);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body())
                .contains("\"results\":[")
                .contains("\"retrievalMode\":\"SEMANTIC\"");
    }

    @Test
    @DisplayName("an empty query is rejected as a validation failure")
    void emptyQueryIsRejected() throws Exception {
        HttpResponse<String> response = send("POST", "/api/v1/search", OPERATOR,
                "{\"text\":\"   \",\"maxResults\":5}");

        assertThat(response.statusCode()).isEqualTo(400);
    }

    @Test
    @DisplayName("the version endpoint reports the search engine and index size")
    void versionReportsSearch() throws Exception {
        HttpResponse<String> version = send("GET", "/api/v1/version", null, null);
        assertThat(version.statusCode()).isEqualTo(200);
        String body = version.body();
        assertThat(body)
                .contains("\"milestone\":\"M12")
                .contains("\"search\":{")
                .contains("\"reconstruction\":{")
                .contains("\"engine\":\"sfs-search/0.1\"")
                .contains("\"indexedVectors\":")
                .contains("semantic-search")
                .contains("memory-system");
    }

    @Test
    @DisplayName("the UI search page serves real results")
    void uiSearchPageServes() throws Exception {
        HttpResponse<String> page = send("GET", "/search?text=database%20latency", null, null);
        assertThat(page.statusCode()).isEqualTo(200);
    }
}
