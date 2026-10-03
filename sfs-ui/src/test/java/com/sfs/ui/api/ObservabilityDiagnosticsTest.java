package com.sfs.ui.api;

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
@DisplayName("Milestone 14 diagnostics: metrics, observability and configuration")
class ObservabilityDiagnosticsTest {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @LocalServerPort
    private int port;

    private HttpResponse<String> version() throws Exception {
        return CLIENT.send(HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/api/v1/version"))
                .GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    @DisplayName("the version endpoint reports the observability blocks and identity")
    void blocks() throws Exception {
        HttpResponse<String> response = version();

        assertThat(response.statusCode()).isEqualTo(200);
        String body = response.body();
        assertThat(body)
                .contains("\"milestone\":\"M14")
                .contains("infrastructure-observability")
                .contains("\"metrics\":{")
                .contains("sfs-metrics/0.1")
                .contains("\"observability\":{")
                .contains("sfs-log/0.1")
                .contains("X-SFS-Trace-Id")
                .contains("\"configuration\":{")
                .contains("sfs-config/0.1")
                .contains("\"secureStoreBytes\":")
                .contains("\"masterKeyBytes\":");
    }

    @Test
    @DisplayName("the configuration block is externalized and loopback-safe")
    void configuration() throws Exception {
        String body = version().body();

        assertThat(body).contains("\"server.address\":\"127.0.0.1\"");
        assertThat(body).contains("\"loopbackEnforced\":\"true\"");
        assertThat(body).contains("\"sfs.memory.path\":");
        assertThat(body).contains("\"sfs.security.keys-dir\":");
    }

    @Test
    @DisplayName("served requests are recorded as structured metrics")
    void requestsAreRecorded() throws Exception {
        version();
        version();

        String body = version().body();

        assertThat(body).contains("requests.served");
        assertThat(body).contains("http.request");
    }
}
