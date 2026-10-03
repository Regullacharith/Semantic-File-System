package com.sfs.ui.api;

import com.sfs.core.observe.ObservabilityRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Milestone 14 security: secrets never reach error bodies or diagnostics")
class ObservabilitySecurityTest {

    private static final String SECRET = "hunter2-secret-zz-probe";

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @LocalServerPort
    private int port;

    @Autowired
    private ObservabilityRegistry observabilityRegistry;

    private HttpResponse<String> send(String method, String path, String body)
            throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json");
        if (body != null) {
            builder.POST(HttpRequest.BodyPublishers.ofString(body));
        }
        return CLIENT.send(builder.method(method,
                body == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    @DisplayName("a malformed body carrying a secret is rejected without echoing it")
    void malformedSecretNotEchoed() throws Exception {
        HttpResponse<String> response = send("POST", "/api/v1/search",
                "{\"text\":\"" + SECRET + "\" \"");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).doesNotContain(SECRET);
    }

    @Test
    @DisplayName("an unknown path carrying a secret-shaped segment produces no echo")
    void unknownPathNotEchoed() throws Exception {
        HttpResponse<String> response = send("GET",
                "/api/v1/objects/" + SECRET + "/dna", null);

        assertThat(response.statusCode()).isIn(400, 404);
        assertThat(response.body()).doesNotContain(SECRET);
    }

    @Test
    @DisplayName("structured events carry identifiers and outcomes but never input values")
    void eventsCarryNoValues() throws Exception {
        send("POST", "/api/v1/search",
                "{\"text\":\"" + SECRET + "\" \"");

        assertThat(observabilityRegistry.recentEvents())
                .allSatisfy(event -> {
                    String rendered = event.toString() + event.toLogLine();
                    assertThat(rendered).doesNotContain(SECRET);
                });
    }

    @Test
    @DisplayName("every response carries a trace id and an incoming id is honored")
    void traceIds() throws Exception {
        HttpResponse<String> generated = send("GET", "/api/v1/version", null);
        String traceId = generated.headers().firstValue("X-SFS-Trace-Id")
                .orElse("missing");
        assertThat(traceId).matches("[0-9a-f]{16}");

        HttpResponse<String> echoed = CLIENT.send(HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/api/v1/version"))
                .header("X-SFS-Trace-Id", "aaaabbbbccccdddd")
                .GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(echoed.headers().firstValue("X-SFS-Trace-Id"))
                .contains("aaaabbbbccccdddd");
    }
}
