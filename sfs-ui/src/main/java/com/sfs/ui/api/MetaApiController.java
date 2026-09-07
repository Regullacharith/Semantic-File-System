package com.sfs.ui.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class MetaApiController {

    private final com.sfs.app.service.FileApplicationService fileApplicationService;

    public MetaApiController(com.sfs.app.service.FileApplicationService fileApplicationService) {
        this.fileApplicationService = fileApplicationService;
    }


    private static final String API_VERSION = "v1";
    private static final String CONTRACTS_VERSION = "0.1";
    private static final String DNA_SCHEMA_VERSION = "sfs-dna/0.1";
    private static final String RULES_VERSION = "sfs-rules/0.1";

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "timestamp", Instant.now().toString());
    }

    @GetMapping("/meta/lifecycle")
    public com.sfs.app.api.response.LifecycleStatisticsResponse lifecycleStatistics(
            @RequestHeader(name = "X-SFS-Credential", required = false) String credential) {
        return fileApplicationService.lifecycleStatistics(credential);
    }

    @GetMapping("/version")
    public Map<String, Object> version() {
        return Map.of(
                "apiVersion", API_VERSION,
                "contractsVersion", CONTRACTS_VERSION,
                "dnaSchemaVersion", DNA_SCHEMA_VERSION,
                "rulesVersion", RULES_VERSION,
                "phase", " File Lifecycle Manager",
                "enforcedSubsystems", java.util.List.of("file-lifecycle"),
                "note", "The file lifecycle manager is the real subsystem. "
                        + "Analysis is a development stub; search, reconstruction and "
                        + "evaluation are mocked. Security boundaries are enforced with "
                        + "development identities in security .");
    }
}
