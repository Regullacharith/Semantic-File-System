package com.sfs.ui.config;

import com.sfs.adapters.registry.AdapterRegistry;
import com.sfs.adapters.resolve.AdapterResolver;
import com.sfs.adapters.resolve.UnsupportedFileTypeException;
import com.sfs.adapters.text.TextAdapter;
import com.sfs.app.service.JobRegistry;
import com.sfs.app.service.ReconstructionApplicationService;
import com.sfs.engine.cache.AnalysisCache;
import com.sfs.engine.core.AnalysisCompletionListener;
import com.sfs.engine.core.AnalysisInput;
import com.sfs.engine.core.AnalysisInputProvider;
import com.sfs.engine.core.AnalysisJob;
import com.sfs.engine.core.AnalysisJobListener;
import com.sfs.engine.core.SemanticEngine;
import com.sfs.lifecycle.core.ImportAcceptancePolicy;
import com.sfs.lifecycle.core.LifecyclePersistence;
import com.sfs.lifecycle.store.RawContentStore;
import com.sfs.memory.H2LifecyclePersistence;
import com.sfs.memory.H2MemoryDatabase;
import com.sfs.memory.H2RawContentStore;
import com.sfs.memory.MemoryDnaRepository;
import com.sfs.memory.VectorIndex;
import com.sfs.engine.level.AnalysisLevelPolicy;
import com.sfs.engine.record.InMemorySemanticRecordStore;
import com.sfs.lifecycle.core.AnalysisDispatcher;
import com.sfs.lifecycle.core.FileLifecycleManager;
import com.sfs.lifecycle.store.RawContentStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class EngineWiringConfiguration {

    @Bean
    public InMemorySemanticRecordStore semanticRecordStore(
            com.sfs.core.dna.DnaRepository dnaRepository) {
        return new InMemorySemanticRecordStore(dnaRepository);
    }

    @Bean
    public AnalysisCache analysisCache() {
        return new AnalysisCache();
    }

    @Bean
    public AnalysisLevelPolicy analysisLevelPolicy() {
        return AnalysisLevelPolicy.v1();
    }

    @Bean(destroyMethod = "close")
    public H2MemoryDatabase memoryDatabase(
            @org.springframework.beans.factory.annotation.Value("${sfs.memory.path:data/sfs-memory}")
            String memoryPath) {
        boolean absolute = memoryPath.matches("^[A-Za-z]:[\\\\/].*")
                || memoryPath.startsWith("/")
                || memoryPath.startsWith("~");
        H2MemoryDatabase database = new H2MemoryDatabase(
                "jdbc:h2:file:" + (absolute ? "" : "./") + memoryPath
                        + ";AUTO_SERVER=FALSE");
        database.initialize();
        return database;
    }

    @Bean
    public VectorIndex vectorIndex() {
        return new VectorIndex();
    }

    @Bean
    public com.sfs.core.dna.DnaRepository dnaRepository(H2MemoryDatabase memoryDatabase,
                                                        VectorIndex vectorIndex) {
        return new MemoryDnaRepository(memoryDatabase, vectorIndex);
    }

    @Bean
    public RawContentStore rawContentStore(H2MemoryDatabase memoryDatabase) {
        return new H2RawContentStore(memoryDatabase);
    }

    @Bean
    public LifecyclePersistence lifecyclePersistence(H2MemoryDatabase memoryDatabase) {
        return new H2LifecyclePersistence(memoryDatabase);
    }

    @Bean
    public com.sfs.contracts.search.SearchService searchService(
            VectorIndex vectorIndex,
            H2MemoryDatabase memoryDatabase) {
        return new com.sfs.search.SearchEngine(vectorIndex, memoryDatabase);
    }

    @Bean
    public com.sfs.core.rules.RuleRepository ruleRepository(
            H2MemoryDatabase memoryDatabase) {
        com.sfs.core.rules.RuleRepository repository = new com.sfs.core.rules.RuleRepository();
        for (com.sfs.core.rules.RuleSet loaded : memoryDatabase.loadRuleSets()) {
            repository.save(loaded, java.time.Instant.now());
        }
        repository.setDerivationSink(set -> memoryDatabase.saveRuleSet(
                set.objectId(), set.dnaVersion(),
                com.sfs.core.rules.RuleSetCanonical.serialize(set),
                com.sfs.core.rules.RuleSetCanonical.integrityHash(set),
                java.time.Instant.now()));
        return repository;
    }

    @Bean
    public com.sfs.core.rules.ReconstructionPlanner reconstructionPlanner(
            com.sfs.core.rules.RuleRepository ruleRepository) {
        return new com.sfs.core.rules.ReconstructionPlanner(ruleRepository);
    }

    @Bean
    public com.sfs.reconstruction.SFSReconstructionModel reconstructionModel() {
        return new com.sfs.reconstruction.model.DeterministicBaselineRenderer();
    }

    @Bean(destroyMethod = "shutdown")
    public com.sfs.reconstruction.engine.ReconstructionEngine reconstructionEngine(
            com.sfs.core.dna.DnaRepository dnaRepository,
            com.sfs.core.rules.ReconstructionPlanner reconstructionPlanner,
            com.sfs.reconstruction.SFSReconstructionModel reconstructionModel,
            com.sfs.contracts.file.FileService fileService) {
        return new com.sfs.reconstruction.engine.ReconstructionEngine(
                new com.sfs.reconstruction.engine.DnaRuleLoader(
                        dnaRepository, fileService),
                reconstructionPlanner,
                reconstructionModel,
                new com.sfs.reconstruction.PlanConstraintInterface(),
                new com.sfs.reconstruction.engine.ReconstructionPostProcessor(),
                new com.sfs.reconstruction.engine.ReconstructionArtifactFactory());
    }

    @Bean
    public com.sfs.contracts.evaluation.EvaluationService evaluationService(
            com.sfs.contracts.reconstruction.ReconstructionService reconstructionService,
            com.sfs.lifecycle.store.RawContentStore rawContentStore,
            com.sfs.core.dna.DnaRepository dnaRepository) {
        return new com.sfs.evaluation.FidelityEvaluationService(
                reconstructionService, rawContentStore, dnaRepository);
    }

    @Bean
    public AdapterRegistry adapterRegistry() {
        AdapterRegistry registry = new AdapterRegistry();
        registry.register(new TextAdapter());
        return registry;
    }

    @Bean
    public AdapterResolver adapterResolver(AdapterRegistry adapterRegistry) {
        return new AdapterResolver(adapterRegistry);
    }

    @Bean
    public AnalysisInputProvider analysisInputProvider(RawContentStore rawContentStore,
                                                       FileLifecycleManager fileLifecycleManager) {
        return objectId -> fileLifecycleManager.registeredFile(objectId)
                .flatMap(file -> rawContentStore.retrieve(objectId)
                        .map(bytes -> new AnalysisInput(
                                objectId,
                                file.metadata().fileName(),
                                file.metadata().contentType(),
                                bytes)));
    }

    @Bean
    public ImportAcceptancePolicy importAcceptancePolicy(AdapterResolver adapterResolver) {
        return (fileName, contentType) -> adapterResolver.find(fileName, contentType).isPresent()
                ? java.util.Optional.empty()
                : java.util.Optional.of(
                        new UnsupportedFileTypeException(fileName, contentType).getMessage());
    }

    @Bean
    public AnalysisCompletionListener lifecycleCompletionListener(FileLifecycleManager manager) {
        return new AnalysisCompletionListener() {

            @Override
            public void onAnalysisSuccess(String objectId, String dnaVersion, Long durationMs) {
                manager.completeAnalysisSuccess(objectId, dnaVersion, durationMs);
            }

            @Override
            public void onAnalysisReused(String objectId, String dnaVersion) {
                manager.completeAnalysisSuccess(objectId, dnaVersion);
            }

            @Override
            public void onAnalysisFailure(String objectId, String reason) {
                manager.completeAnalysisFailure(objectId, reason);
            }
        };
    }

    @Bean(destroyMethod = "close")
    public SemanticEngine semanticEngine(AnalysisInputProvider analysisInputProvider,
                                         AdapterResolver adapterResolver,
                                         InMemorySemanticRecordStore semanticRecordStore,
                                         AnalysisCache analysisCache,
                                         AnalysisLevelPolicy analysisLevelPolicy,
                                         AnalysisCompletionListener lifecycleCompletionListener,
                                         Clock sfsClock) {
        return new SemanticEngine(analysisInputProvider, adapterResolver,
                semanticRecordStore, analysisCache,
                analysisLevelPolicy, lifecycleCompletionListener, sfsClock);
    }

    @Bean
    public AnalysisDispatcher semanticEngineDispatcher(SemanticEngine semanticEngine,
                                                       FileLifecycleManager fileLifecycleManager) {
        AnalysisDispatcher dispatcher = objectId -> {
            AnalysisJob job = semanticEngine.submit(objectId);
            return job.status() == AnalysisJob.Status.REJECTED ? null : job.jobId();
        };
        fileLifecycleManager.bindAnalysisDispatcher(dispatcher);
        return dispatcher;
    }

    @Bean
    public AnalysisJobListener analysisJobBridge(SemanticEngine semanticEngine,
                                                 ReconstructionApplicationService reconstruction) {
        AnalysisJobListener listener = (previous, current) ->
                reconstruction.jobRegistry().register(toRecord(current));
        semanticEngine.addJobListener(listener);
        return listener;
    }

    private static JobRegistry.JobRecord toRecord(AnalysisJob job) {
        String status = switch (job.status()) {
            case QUEUED -> "QUEUED";
            case RUNNING -> "RUNNING";
            case COMPLETED, REUSED -> "COMPLETED";
            case FAILED -> "FAILED";
            case REJECTED -> "REJECTED";
        };
        String reason = switch (job.status()) {
            case REUSED -> "Analysis reused prior work for unchanged content.";
            case FAILED, REJECTED -> job.failureReason();
            default -> null;
        };
        return new JobRegistry.JobRecord(job.jobId(), job.objectId(),
                JobRegistry.TYPE_ANALYSIS, status, job.submittedAt(),
                job.isTerminal() ? job.completedAt() : null, reason);
    }
}
