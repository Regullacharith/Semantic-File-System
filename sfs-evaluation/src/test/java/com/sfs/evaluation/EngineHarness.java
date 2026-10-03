package com.sfs.evaluation;

import com.sfs.adapters.registry.AdapterRegistry;
import com.sfs.adapters.resolve.AdapterResolver;
import com.sfs.adapters.text.TextAdapter;
import com.sfs.contracts.file.FileImportRequest;
import com.sfs.contracts.file.FileOperationResult;
import com.sfs.contracts.file.FileService;
import com.sfs.contracts.file.FileStatus;
import com.sfs.contracts.file.FileSummary;
import com.sfs.contracts.security.Principal;
import com.sfs.core.dna.DnaRepository;
import com.sfs.core.dna.InMemoryDnaRepository;
import com.sfs.core.dna.SemanticDna;
import com.sfs.engine.cache.AnalysisCache;
import com.sfs.engine.core.AnalysisInput;
import com.sfs.engine.core.AnalysisJob;
import com.sfs.engine.core.SemanticEngine;
import com.sfs.engine.level.AnalysisLevelPolicy;
import com.sfs.engine.record.InMemorySemanticRecordStore;
import com.sfs.reconstruction.PlanConstraintInterface;
import com.sfs.reconstruction.engine.DnaRuleLoader;
import com.sfs.reconstruction.engine.ReconstructionArtifactFactory;
import com.sfs.reconstruction.engine.ReconstructionEngine;
import com.sfs.reconstruction.engine.ReconstructionJob;
import com.sfs.reconstruction.engine.ReconstructionPostProcessor;
import com.sfs.reconstruction.model.DeterministicBaselineRenderer;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class EngineHarness {

    private final InMemoryDnaRepository dnaRepository = new InMemoryDnaRepository();
    private final InMemorySemanticRecordStore store =
            new InMemorySemanticRecordStore(dnaRepository);
    private final Map<String, SemanticDna> dnaByObject = new LinkedHashMap<>();

    public SemanticDna analyze(GoldDoc doc) {
        byte[] bytes = doc.originalBytes();
        AnalysisInput input = new AnalysisInput(doc.objectId(), doc.fileName(),
                "text/plain", bytes);
        AdapterRegistry registry = new AdapterRegistry();
        registry.register(new TextAdapter());
        SemanticEngine engine = new SemanticEngine(
                id -> Optional.of(input), new AdapterResolver(registry), store,
                new AnalysisCache(), AnalysisLevelPolicy.v1(),
                new com.sfs.engine.core.AnalysisCompletionListener() { },
                Clock.systemUTC());
        AnalysisJob job = engine.analyzeNow(doc.objectId());
        if (job.status() != AnalysisJob.Status.COMPLETED) {
            throw new IllegalStateException("analysis failed for "
                    + doc.objectId());
        }
        SemanticDna dna = store.findStored(doc.objectId()).orElseThrow().dna();
        dnaByObject.put(doc.objectId(), dna);
        return dna;
    }

    public ReconstructionEngine reconstructionEngine() {
        TestFileService files = new TestFileService();
        for (GoldDoc doc : GoldDoc.GOLD) {
            files.register(doc.objectId(), doc.fileName(), FileStatus.ANALYZED);
        }
        return new ReconstructionEngine(
                new DnaRuleLoader(dnaRepository, files),
                new com.sfs.core.rules.ReconstructionPlanner(
                        new com.sfs.core.rules.RuleRepository()),
                new DeterministicBaselineRenderer(),
                new PlanConstraintInterface(),
                new ReconstructionPostProcessor(),
                new ReconstructionArtifactFactory());
    }

    public Map<String, SemanticDna> dnaByObject() {
        return Map.copyOf(dnaByObject);
    }

    public DnaRepository dnaRepository() {
        return dnaRepository;
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
        throw new IllegalStateException("job never reached a terminal state");
    }

    static final class TestFileService implements FileService {

        private final Map<String, FileSummary> files = new LinkedHashMap<>();

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
            throw new UnsupportedOperationException("not used in this harness");
        }

        @Override
        public FileOperationResult requestAnalysis(String objectId) {
            throw new UnsupportedOperationException("not used in this harness");
        }

        @Override
        public FileOperationResult softDelete(String objectId, Principal principal) {
            throw new UnsupportedOperationException("not used in this harness");
        }

        @Override
        public FileOperationResult memorize(String objectId, Principal principal) {
            throw new UnsupportedOperationException("not used in this harness");
        }

        @Override
        public FileOperationResult undoDelete(String objectId, Principal principal) {
            throw new UnsupportedOperationException("not used in this harness");
        }

        @Override
        public FileOperationResult purgeRawData(String objectId, Principal principal) {
            throw new UnsupportedOperationException("not used in this harness");
        }

        void register(String objectId, String displayName, FileStatus status) {
            java.time.Instant now = java.time.Instant.now();
            files.put(objectId, new FileSummary(objectId, displayName, status,
                    displayName.length(), now, now));
        }
    }

    private EngineHarness() {
    }

    public static EngineHarness harness() {
        return new EngineHarness();
    }

}
