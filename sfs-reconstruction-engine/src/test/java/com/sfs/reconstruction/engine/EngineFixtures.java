package com.sfs.reconstruction.engine;

import com.sfs.contracts.file.FileImportRequest;
import com.sfs.contracts.file.FileOperationResult;
import com.sfs.contracts.file.FileService;
import com.sfs.contracts.file.FileStatus;
import com.sfs.contracts.file.FileSummary;
import com.sfs.core.dna.DnaIdentity;
import com.sfs.core.dna.DnaRepository;
import com.sfs.core.dna.Entity;
import com.sfs.core.dna.Fact;
import com.sfs.core.dna.FidelityProfile;
import com.sfs.core.dna.InMemoryDnaRepository;
import com.sfs.core.dna.ProtectedReference;
import com.sfs.core.dna.Relationship;
import com.sfs.core.dna.SemanticDna;
import com.sfs.core.dna.SemanticDnaBuilder;
import com.sfs.core.dna.StructureNode;
import com.sfs.reconstruction.ConstraintInterface;
import com.sfs.reconstruction.PlanConstraintInterface;
import com.sfs.reconstruction.SFSReconstructionModel;
import com.sfs.reconstruction.model.DeterministicBaselineRenderer;

import com.sfs.contracts.security.Principal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class EngineFixtures {

    public static final Instant T0 = Instant.parse("2026-05-01T08:00:00Z");

    private EngineFixtures() {
    }

    public static SemanticDna structuredDna(String objectId) {
        return SemanticDnaBuilder.forIdentity(new DnaIdentity(
                        objectId, "sfs-dna/0.2", 1, "sfs-engine/0.1", T0))
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

    public static SemanticDna minimalDna(String objectId) {
        return SemanticDnaBuilder.forIdentity(new DnaIdentity(
                        objectId, "sfs-dna/0.2", 1, "sfs-engine/0.1", T0))
                .summary("A short note that records the storage decision for 2026.")
                .fidelity(new FidelityProfile(0.8, 0.6, "sfs-engine/0.1"))
                .build();
    }

    public static SemanticDna protectedDna(String objectId) {
        return SemanticDnaBuilder.forIdentity(new DnaIdentity(
                        objectId, "sfs-dna/0.2", 1, "sfs-engine/0.1", T0))
                .summary("The deployment credentials document lists the database "
                        + "password and the API key for the production platform.")
                .entities(List.of(new Entity("Production Platform", "system", 1)))
                .facts(List.of(new Fact(
                        "The production database requires a password.", true, 0.9)))
                .protectedReferences(List.of(new ProtectedReference(
                        "ref-2001", "credential", "database password",
                        "Credentials section")))
                .fidelity(new FidelityProfile(0.9, 0.85, "sfs-engine/0.1"))
                .build();
    }

    public static DnaRepository repositoryWith(SemanticDna... dnas) {
        InMemoryDnaRepository repository = new InMemoryDnaRepository();
        for (SemanticDna dna : dnas) {
            repository.save(dna, T0);
        }
        return repository;
    }

    public static TestFileService filesWith(String objectId, String displayName,
                                            FileStatus status) {
        TestFileService files = new TestFileService();
        files.register(objectId, displayName, status);
        return files;
    }

    public static ReconstructionEngine engine(DnaRepository dnaRepository,
                                              FileService fileService) {
        return new ReconstructionEngine(
                new DnaRuleLoader(dnaRepository, fileService),
                new com.sfs.core.rules.ReconstructionPlanner(
                        new com.sfs.core.rules.RuleRepository()),
                new DeterministicBaselineRenderer(),
                new PlanConstraintInterface(),
                new ReconstructionPostProcessor(),
                new ReconstructionArtifactFactory());
    }

    public static ReconstructionEngine engineWithModel(DnaRepository dnaRepository,
                                                       FileService fileService,
                                                       SFSReconstructionModel model) {
        return new ReconstructionEngine(
                new DnaRuleLoader(dnaRepository, fileService),
                new com.sfs.core.rules.ReconstructionPlanner(
                        new com.sfs.core.rules.RuleRepository()),
                model,
                new PlanConstraintInterface(),
                new ReconstructionPostProcessor(),
                new ReconstructionArtifactFactory());
    }

    public static void awaitTerminal(ReconstructionEngine engine, String jobId) {
        long deadline = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadline) {
            Optional<ReconstructionJob> job = engine.findJobRecord(jobId);
            if (job.isPresent() && job.get().terminal()) {
                return;
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("await interrupted", e);
            }
        }
        throw new IllegalStateException("job " + jobId + " never reached a "
                + "terminal state");
    }

    public static final class TestFileService implements FileService {

        private final Map<String, FileSummary> files = new HashMap<>();

        void register(String objectId, String displayName, FileStatus status) {
            files.put(objectId, new FileSummary(objectId, displayName, status,
                    displayName.length(), T0, T0));
        }

        @Override
        public List<FileSummary> listFiles() {
            return List.copyOf(files.values());
        }

        @Override
        public Optional<FileSummary> findByObjectId(String objectId) {
            return Optional.ofNullable(files.get(objectId));
        }

        @Override
        public FileOperationResult importFile(FileImportRequest request) {
            throw new UnsupportedOperationException("not used by the engine");
        }

        @Override
        public FileOperationResult requestAnalysis(String objectId) {
            throw new UnsupportedOperationException("not used by the engine");
        }

        @Override
        public FileOperationResult softDelete(String objectId, Principal principal) {
            throw new UnsupportedOperationException("not used by the engine");
        }

        @Override
        public FileOperationResult memorize(String objectId, Principal principal) {
            throw new UnsupportedOperationException("not used by the engine");
        }

        @Override
        public FileOperationResult undoDelete(String objectId, Principal principal) {
            throw new UnsupportedOperationException("not used by the engine");
        }

        @Override
        public FileOperationResult purgeRawData(String objectId, Principal principal) {
            throw new UnsupportedOperationException("not used by the engine");
        }
    }
}
