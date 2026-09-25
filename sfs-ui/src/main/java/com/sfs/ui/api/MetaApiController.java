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
    private final com.sfs.adapters.registry.AdapterRegistry adapterRegistry;
    private final com.sfs.adapters.resolve.AdapterResolver adapterResolver;
    private final com.sfs.core.rules.RuleRepository ruleRepository;

    public MetaApiController(com.sfs.app.service.FileApplicationService fileApplicationService,
                             com.sfs.adapters.registry.AdapterRegistry adapterRegistry,
                             com.sfs.adapters.resolve.AdapterResolver adapterResolver,
                             com.sfs.core.rules.RuleRepository ruleRepository) {
        this.fileApplicationService = fileApplicationService;
        this.adapterRegistry = adapterRegistry;
        this.adapterResolver = adapterResolver;
        this.ruleRepository = ruleRepository;
    }


    private static final String API_VERSION = "v1";
    private static final String CONTRACTS_VERSION = "0.1";
    private static final String DNA_SCHEMA_VERSION = "sfs-dna/0.2";
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
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("apiVersion", API_VERSION);
        body.put("contractsVersion", CONTRACTS_VERSION);
        body.put("dnaSchemaVersion", DNA_SCHEMA_VERSION);
        body.put("rulesVersion", RULES_VERSION);
        body.put("milestone", "M07 — Reconstruction Rules System");
        body.put("enforcedSubsystems",
                java.util.List.of("file-lifecycle", "semantic-engine", "adapter-framework",
                        "semantic-representation", "reconstruction-rules"));
        body.put("rules", Map.of(
                "schemaVersion", com.sfs.core.rules.RuleSetCanonical.RULES_SCHEMA_VERSION,
                "boundRuleSets", ruleRepository.size(),
                "planReuses", ruleRepository.hits(),
                "planDerivations", ruleRepository.derivations(),
                "versionConflicts", ruleRepository.versionConflicts()));
        body.put("adapters", adapterRegistry.descriptors().stream()
                .map(descriptor -> Map.of(
                        "id", descriptor.id(),
                        "displayName", descriptor.displayName(),
                        "version", descriptor.version(),
                        "extensions", java.util.List.copyOf(
                                descriptor.supportedExtensions()),
                        "contentTypes", java.util.List.copyOf(
                                descriptor.supportedContentTypes()),
                        "capabilities", java.util.List.copyOf(descriptor.capabilities())))
                .toList());
        body.put("adapterResolutions", adapterResolver.resolutions());
        body.put("adapterRefusals", adapterResolver.refusals());
        body.put("note", "The file lifecycle manager, the semantic engine, the adapter "
                + "framework, the semantic representation system and the reconstruction "
                + "rules system are real subsystems. Search, reconstruction rendering and "
                + "evaluation are mocked. Security boundaries are enforced with "
                + "development identities until the security milestone.");
        return body;
    }
}
