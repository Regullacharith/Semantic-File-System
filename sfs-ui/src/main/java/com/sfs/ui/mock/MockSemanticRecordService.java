package com.sfs.ui.mock;

import com.sfs.contracts.semantic.ProtectedReferenceView;
import com.sfs.contracts.semantic.SemanticDnaView;
import com.sfs.contracts.semantic.SemanticRecordService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory stand-in for the Semantic Representation and Memory subsystems.
 */
@Service
@Profile("mock")
public class MockSemanticRecordService implements SemanticRecordService {

    private static final String SCHEMA_VERSION = "sfs-dna/0.1";
    private static final String ANALYZER_VERSION = "mock-analyzer/0.1";

    private static final int EMBEDDING_DIMENSIONS = 384;

    private final Map<String, SemanticDnaView> recordsByObjectId = buildRecords();

    @Override
    public Optional<SemanticDnaView> findSemanticDna(String objectId) {
        if (objectId == null || objectId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(recordsByObjectId.get(objectId));
    }

    private static Map<String, SemanticDnaView> buildRecords() {
        return Map.of(
                "sfs-obj-0001-a1b2c3d4", researchSummary(),
                "sfs-obj-0002-e5f6a7b8", archivedReport(),
                "sfs-obj-0003-c9d0e1f2", meetingNotes(),
                "sfs-obj-0004-b3c4d5e6", deploymentConfig());
    }

    private static SemanticDnaView researchSummary() {
        return new SemanticDnaView(
                "sfs-obj-0001-a1b2c3d4",
                SCHEMA_VERSION,
                2,
                "Overview of semantic file storage research, covering representation design "
                        + "and reconstruction fidelity targets.",
                List.of("semantic representation", "knowledge preservation", "reconstruction"),
                List.of("research", "storage"),
                List.of(
                        new SemanticDnaView.EntityView("Semantic DNA", "Concept", 12),
                        new SemanticDnaView.EntityView("Memory Database", "Component", 5),
                        new SemanticDnaView.EntityView("Vector Index", "Component", 3)),
                List.of(
                        new SemanticDnaView.FactView(
                                "Fidelity is measured across semantic, structural and factual dimensions.",
                                true, 0.94),
                        new SemanticDnaView.FactView(
                                "The reconstruction target of 87 to 92 percent is experimental, not guaranteed.",
                                true, 0.91),
                        new SemanticDnaView.FactView(
                                "Embeddings support retrieval but do not replace explicit facts.",
                                false, 0.88)),
                List.of(
                        new SemanticDnaView.RelationshipView(
                                "Semantic DNA", "is stored in", "Memory Database"),
                        new SemanticDnaView.RelationshipView(
                                "Vector Index", "supports", "Semantic Search")),
                List.of(
                        new SemanticDnaView.StructureNodeView("Introduction", 1, 0),
                        new SemanticDnaView.StructureNodeView("Representation Design", 1, 1),
                        new SemanticDnaView.StructureNodeView("Fidelity Targets", 2, 2)),
                List.of(),
                EMBEDDING_DIMENSIONS,
                new SemanticDnaView.FidelityProfileView(0.92, 0.88, ANALYZER_VERSION));
    }

    private static SemanticDnaView archivedReport() {
        return new SemanticDnaView(
                "sfs-obj-0002-e5f6a7b8",
                SCHEMA_VERSION,
                1,
                "Quarterly report on database performance and indexing strategy. The raw file "
                        + "has been deleted; this semantic memory survives.",
                List.of("database performance", "indexing", "query latency"),
                List.of("database", "performance"),
                List.of(
                        new SemanticDnaView.EntityView("PostgreSQL", "Technology", 8),
                        new SemanticDnaView.EntityView("Q3 2026", "Time period", 4)),
                List.of(
                        new SemanticDnaView.FactView(
                                "Query latency decreased by 40 percent after indexing changes.",
                                true, 0.96),
                        new SemanticDnaView.FactView(
                                "The reporting period covers Q3 2026.", true, 0.99),
                        new SemanticDnaView.FactView(
                                "Index rebuilds were scheduled outside business hours.",
                                false, 0.82)),
                List.of(
                        new SemanticDnaView.RelationshipView(
                                "indexing changes", "caused", "latency reduction"),
                        new SemanticDnaView.RelationshipView(
                                "PostgreSQL", "hosts", "production workload")),
                List.of(
                        new SemanticDnaView.StructureNodeView("Summary", 1, 0),
                        new SemanticDnaView.StructureNodeView("Measurements", 1, 1),
                        new SemanticDnaView.StructureNodeView("Recommendations", 1, 2)),
                List.of(),
                EMBEDDING_DIMENSIONS,
                new SemanticDnaView.FidelityProfileView(0.89, 0.91, ANALYZER_VERSION));
    }

    private static SemanticDnaView meetingNotes() {
        return new SemanticDnaView(
                "sfs-obj-0003-c9d0e1f2",
                SCHEMA_VERSION,
                1,
                "Project planning notes covering milestone sequence, ownership and delivery order.",
                List.of("project planning", "milestones", "delivery"),
                List.of("planning", "process"),
                List.of(
                        new SemanticDnaView.EntityView("Milestone 01", "Milestone", 6),
                        new SemanticDnaView.EntityView("Milestone 09", "Milestone", 2)),
                List.of(
                        new SemanticDnaView.FactView(
                                "The user interface layer is delivered before the search engine.",
                                true, 0.93),
                        new SemanticDnaView.FactView(
                                "Each task is verified before the next begins.", false, 0.85)),
                List.of(new SemanticDnaView.RelationshipView(
                        "Milestone 01", "precedes", "Milestone 09")),
                List.of(
                        new SemanticDnaView.StructureNodeView("Agenda", 1, 0),
                        new SemanticDnaView.StructureNodeView("Decisions", 1, 1)),
                List.of(),
                EMBEDDING_DIMENSIONS,
                new SemanticDnaView.FidelityProfileView(0.87, 0.79, ANALYZER_VERSION));
    }

    private static SemanticDnaView deploymentConfig() {
        return new SemanticDnaView(
                "sfs-obj-0004-b3c4d5e6",
                SCHEMA_VERSION,
                1,
                "Deployment configuration notes for the staging environment. Sensitive values "
                        + "are held as protected references and are absent from this representation.",
                List.of("deployment", "configuration", "credentials"),
                List.of("operations"),
                List.of(
                        new SemanticDnaView.EntityView("staging environment", "Environment", 7),
                        new SemanticDnaView.EntityView("payment gateway", "Service", 2)),
                List.of(
                        new SemanticDnaView.FactView(
                                "The staging environment authenticates to the payment gateway.",
                                true, 0.90),
                        new SemanticDnaView.FactView(
                                "Credentials are rotated every ninety days.", false, 0.84)),
                List.of(new SemanticDnaView.RelationshipView(
                        "staging environment", "authenticates to", "payment gateway")),
                List.of(
                        new SemanticDnaView.StructureNodeView("Environment", 1, 0),
                        new SemanticDnaView.StructureNodeView("Credentials", 1, 1)),
                List.of(
                        new ProtectedReferenceView(
                                "ref-7f3a9c21",
                                ProtectedReferenceView.SensitiveType.API_KEY,
                                "Authenticates the staging environment to the payment gateway",
                                "Credentials section, line 12",
                                true),
                        new ProtectedReferenceView(
                                "ref-2b8e4d05",
                                ProtectedReferenceView.SensitiveType.PASSWORD,
                                "Database account password for the staging replica",
                                "Credentials section, line 18",
                                false)),
                EMBEDDING_DIMENSIONS,
                new SemanticDnaView.FidelityProfileView(0.85, 0.83, ANALYZER_VERSION));
    }
}
