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
    private final com.sfs.memory.H2MemoryDatabase memoryDatabase;
    private final com.sfs.memory.VectorIndex vectorIndex;
    private final com.sfs.contracts.search.SearchService searchService;
    private final com.sfs.reconstruction.SFSReconstructionModel reconstructionModel;
    private final com.sfs.reconstruction.engine.ReconstructionEngine reconstructionEngine;
    private final com.sfs.contracts.evaluation.EvaluationService evaluationService;
    private final com.sfs.security.SecurityPolicyEngine securityPolicyEngine;
    private final com.sfs.contracts.security.SecretVault secretVault;
    private final com.sfs.security.SecurityAuditLog securityAuditLog;

    public MetaApiController(com.sfs.app.service.FileApplicationService fileApplicationService,
                             com.sfs.adapters.registry.AdapterRegistry adapterRegistry,
                             com.sfs.adapters.resolve.AdapterResolver adapterResolver,
                             com.sfs.core.rules.RuleRepository ruleRepository,
                             com.sfs.memory.H2MemoryDatabase memoryDatabase,
                             com.sfs.memory.VectorIndex vectorIndex,
                             com.sfs.contracts.search.SearchService searchService,
                             com.sfs.reconstruction.SFSReconstructionModel reconstructionModel,
                             com.sfs.reconstruction.engine.ReconstructionEngine reconstructionEngine,
                             com.sfs.contracts.evaluation.EvaluationService evaluationService,
                             com.sfs.security.SecurityPolicyEngine securityPolicyEngine,
                             com.sfs.contracts.security.SecretVault secretVault,
                             com.sfs.security.SecurityAuditLog securityAuditLog) {
        this.fileApplicationService = fileApplicationService;
        this.adapterRegistry = adapterRegistry;
        this.adapterResolver = adapterResolver;
        this.ruleRepository = ruleRepository;
        this.memoryDatabase = memoryDatabase;
        this.vectorIndex = vectorIndex;
        this.searchService = searchService;
        this.reconstructionModel = reconstructionModel;
        this.reconstructionEngine = reconstructionEngine;
        this.evaluationService = evaluationService;
        this.securityPolicyEngine = securityPolicyEngine;
        this.secretVault = secretVault;
        this.securityAuditLog = securityAuditLog;
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
        body.put("milestone", "M13 — Security & Privacy System");
        body.put("enforcedSubsystems",
                java.util.List.of("file-lifecycle", "semantic-engine", "adapter-framework",
                        "semantic-representation", "reconstruction-rules", "memory-system",
                        "semantic-search", "reconstruction-model",
                        "reconstruction-engine", "evaluation-fidelity",
                        "security-privacy"));
        body.put("rules", Map.of(
                "schemaVersion", com.sfs.core.rules.RuleSetCanonical.RULES_SCHEMA_VERSION,
                "boundRuleSets", ruleRepository.size(),
                "planReuses", ruleRepository.hits(),
                "planDerivations", ruleRepository.derivations(),
                "versionConflicts", ruleRepository.versionConflicts()));
        Map<String, Object> storage = new java.util.LinkedHashMap<>(
                memoryDatabase.storageStats());
        storage.put("vectorEntries", vectorIndex.size());
        body.put("storage", storage);
        Map<String, Object> search = new java.util.LinkedHashMap<>();
        search.put("engine", "sfs-search/0.1");
        search.put("embeddingDimensions", 64);
        search.put("indexedVectors", vectorIndex.size());
        search.put("retrievalModes", java.util.List.of("OBJECT_ID_LOOKUP", "SEMANTIC"));
        body.put("search", search);
        Map<String, Object> reconstruction = new java.util.LinkedHashMap<>();
        reconstruction.put("engine", com.sfs.reconstruction.engine.ReconstructionEngine.ENGINE_ID);
        reconstruction.put("model", reconstructionModel.modelId());
        reconstruction.put("flow", "real; single-click reconstruction is produced by "
                + "the reconstruction engine");
        reconstruction.put("modelStatus", "real deterministic baseline; the decoder "
                + "slot is swappable");
        reconstruction.put("evaluation", "real; measured by "
                + com.sfs.evaluation.FidelityEvaluator.EVALUATOR_VERSION);
        reconstruction.putAll(reconstructionEngine.diagnostics());
        body.put("reconstruction", reconstruction);
        Map<String, Object> evaluation = new java.util.LinkedHashMap<>();
        evaluation.put("evaluator", com.sfs.evaluation.FidelityEvaluator.EVALUATOR_VERSION);
        evaluation.put("dimensions", java.util.List.of("SEMANTIC", "STRUCTURAL",
                "FACTUAL", "ENTITY", "RELATIONSHIP", "COMPLETENESS"));
        evaluation.put("criticalFactChecks", "explicit");
        evaluation.put("calibration", "stated confidence vs observed survival, per bin");
        body.put("evaluation", evaluation);
        Map<String, Object> security = new java.util.LinkedHashMap<>();
        security.put("detector", "sfs-security/0.1");
        security.put("policyTypes", securityPolicyEngine.policies().size());
        security.put("storedSecrets", secretVault.size());
        security.put("keyStorage", "master key under data/sfs-keys, ciphertext under "
                + "data/sfs-secure; keys are never stored with ciphertext");
        security.put("passwordPolicy", "non-reversible by default");
        security.put("auditEvents", securityAuditLog.totalEvents());
        body.put("security", security);
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
                + "framework, the semantic representation system, the reconstruction "
                + "rules system, the memory system, the semantic search engine, the "
                + "reconstruction model, the reconstruction engine, the evaluation and "
                + "fidelity system and the security and privacy system are real "
                + "subsystems. Access uses development bootstrap identities with "
                + "hashed credentials; production identity management is deferred.");
        return body;
    }
}
