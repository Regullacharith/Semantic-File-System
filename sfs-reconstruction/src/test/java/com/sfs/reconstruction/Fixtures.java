package com.sfs.reconstruction;

import com.sfs.core.dna.DnaIdentity;
import com.sfs.core.dna.Entity;
import com.sfs.core.dna.Fact;
import com.sfs.core.dna.FidelityProfile;
import com.sfs.core.dna.Relationship;
import com.sfs.core.dna.SemanticDna;
import com.sfs.core.dna.SemanticDnaBuilder;
import com.sfs.core.dna.StructureNode;
import com.sfs.core.rules.ReconstructionPlan;
import com.sfs.core.rules.ReconstructionPlanner;
import com.sfs.core.rules.RuleRepository;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Fixtures {

    public static final Instant T0 = Instant.parse("2026-04-10T09:00:00Z");

    public static final List<String> CORPUS_IDS = List.of(
            "sfs-obj-1001-quarterlyreport",
            "sfs-obj-1002-teammeetingminutes",
            "sfs-obj-1003-researchnotes",
            "sfs-obj-1004-credentials");

    private Fixtures() {
    }

    public static SemanticDna handDna() {
        return SemanticDnaBuilder.forIdentity(new DnaIdentity(
                        "sfs-obj-2001-reconmodel", "sfs-dna/0.2", 1,
                        "sfs-engine/0.1", T0))
                .summary("The platform migration plan moves the core database to "
                        + "new infrastructure during the second quarter.")
                .concepts(List.of("platform migration", "database infrastructure"))
                .topics(List.of("capacity planning"))
                .entities(List.of(
                        new Entity("Core Database", "system", 3),
                        new Entity("Migration Plan", "document", 1)))
                .facts(List.of(
                        new Fact("The migration window opens on April 7.", true, 0.95),
                        new Fact("Rollback takes at most two hours.", false, 0.8)))
                .relationships(List.of(new Relationship(
                        "Core Database", "migrates-to", "New Infrastructure")))
                .structure(List.of(
                        new StructureNode("Overview", 1, 0),
                        new StructureNode("Timeline", 1, 1),
                        new StructureNode("Risks", 2, 2)))
                .fidelity(new FidelityProfile(0.9, 0.85, "sfs-engine/0.1"))
                .build();
    }

    public static SemanticDna shuffledDna() {
        return SemanticDnaBuilder.forIdentity(new DnaIdentity(
                        "sfs-obj-2002-shuffled", "sfs-dna/0.2", 1,
                        "sfs-engine/0.1", T0))
                .summary("The platform migration plan moves the core database to "
                        + "new infrastructure during the second quarter.")
                .structure(List.of(
                        new StructureNode("Timeline", 1, 1),
                        new StructureNode("Overview", 1, 0),
                        new StructureNode("Risks", 2, 2)))
                .fidelity(new FidelityProfile(0.9, 0.85, "sfs-engine/0.1"))
                .build();
    }

    public static SemanticDna protectedDna() {
        return SemanticDnaBuilder.forIdentity(new DnaIdentity(
                        "sfs-obj-2003-protected", "sfs-dna/0.2", 1,
                        "sfs-engine/0.1", T0))
                .summary("The deployment credentials document lists the database "
                        + "password and the API key for the production platform.")
                .entities(List.of(new Entity("Production Platform", "system", 1)))
                .facts(List.of(new Fact(
                        "The production database requires a password.", true, 0.9)))
                .protectedReferences(List.of(new com.sfs.core.dna.ProtectedReference(
                        "ref-1003", "credential", "database password",
                        "Deployment Credentials section")))
                .fidelity(new FidelityProfile(0.9, 0.85, "sfs-engine/0.1"))
                .build();
    }

    public static ReconstructionPlan derivedPlan(SemanticDna dna) {
        return new ReconstructionPlanner(new RuleRepository()).plan(dna, T0);
    }

    public static ReconstructionPlan explicitPlan(SemanticDna dna) {
        return new ReconstructionPlan(
                dna.objectId(),
                dna.dnaVersion(),
                "0f9a2c4b",
                "sfs-rules/test",
                List.of("Overview", "Timeline", "Risks"),
                List.of(new com.sfs.core.rules.RequiredFact(
                        "The migration window opens on April 7.")),
                List.of(new com.sfs.core.rules.RequiredEntity("Core Database", 2)),
                List.of(new Relationship(
                        "Core Database", "migrates-to", "New Infrastructure")),
                new ReconstructionPlan.ContentContract(true, dna.summary(), 1, 1),
                new ReconstructionPlan.ValidationContract(true, true, 0.7),
                List.of());
    }

    public static Map<String, ModelInput> engineCorpus() {
        InMemoryCorpus corpus = new InMemoryCorpus();
        Map<String, ModelInput> inputs = new LinkedHashMap<>();
        ReconstructionPlanner planner =
                new ReconstructionPlanner(new RuleRepository());
        for (String objectId : CORPUS_IDS) {
            SemanticDna dna = corpus.dna(objectId);
            inputs.put(objectId, ModelInput.of(dna, planner.plan(dna, T0)));
        }
        return inputs;
    }

    private static final class InMemoryCorpus {

        private final com.sfs.engine.record.InMemorySemanticRecordStore store =
                new com.sfs.engine.record.InMemorySemanticRecordStore();

        SemanticDna dna(String objectId) {
            {
                com.sfs.engine.core.SemanticEngine engine =
                        new com.sfs.engine.core.SemanticEngine(
                                id -> java.util.Optional.of(new com.sfs.engine.core.AnalysisInput(
                                        id, fileName(id), "text/plain", bytes(fileName(id)))),
                                resolver(),
                                store,
                                new com.sfs.engine.cache.AnalysisCache(),
                                com.sfs.engine.level.AnalysisLevelPolicy.v1(),
                                new com.sfs.engine.core.AnalysisCompletionListener() { },
                                java.time.Clock.systemUTC());
                var job = engine.analyzeNow(objectId);
                if (job.status() != com.sfs.engine.core.AnalysisJob.Status.COMPLETED) {
                    throw new IllegalStateException(
                            "analysis did not complete for " + objectId);
                }
            }
            return store.findStored(objectId).orElseThrow().dna();
        }

        private String fileName(String objectId) {
            return switch (objectId) {
                case "sfs-obj-1001-quarterlyreport" -> "quarterly-report.txt";
                case "sfs-obj-1002-teammeetingminutes" -> "team-meeting-minutes.txt";
                case "sfs-obj-1003-researchnotes" -> "research-notes.txt";
                default -> "deployment-config.txt";
            };
        }

        private byte[] bytes(String fileName) {
            if ("deployment-config.txt".equals(fileName)) {
                return """
                        # Deployment

                        The deployment report covers the database platform for 2026.

                        # Credentials

                        password=hunter2
                        api_key=sk-live-9f8e7d6c5b4a
                        """.getBytes(StandardCharsets.UTF_8);
            }
            try {
                return Files.readAllBytes(Path.of("..", "sfs-engine", "src", "test",
                        "resources", "benchmarks", fileName));
            } catch (Exception e) {
                throw new IllegalStateException("benchmark fixture missing: "
                        + fileName, e);
            }
        }

        private com.sfs.adapters.resolve.AdapterResolver resolver() {
            com.sfs.adapters.registry.AdapterRegistry registry =
                    new com.sfs.adapters.registry.AdapterRegistry();
            registry.register(new com.sfs.adapters.text.TextAdapter());
            return new com.sfs.adapters.resolve.AdapterResolver(registry);
        }
    }
}
