package com.sfs.ui.config;

import com.sfs.app.service.JobRegistry;
import com.sfs.app.service.ReconstructionApplicationService;
import com.sfs.engine.cache.AnalysisCache;
import com.sfs.engine.core.AnalysisCompletionListener;
import com.sfs.engine.core.AnalysisJob;
import com.sfs.engine.core.AnalysisJobListener;
import com.sfs.engine.core.ContentSource;
import com.sfs.engine.core.SemanticEngine;
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
    public InMemorySemanticRecordStore semanticRecordStore() {
        return new InMemorySemanticRecordStore();
    }

    @Bean
    public AnalysisCache analysisCache() {
        return new AnalysisCache();
    }

    @Bean
    public AnalysisLevelPolicy analysisLevelPolicy() {
        return AnalysisLevelPolicy.v1();
    }

    @Bean
    public ContentSource engineContentSource(RawContentStore rawContentStore) {
        return rawContentStore::retrieve;
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
    public SemanticEngine semanticEngine(ContentSource engineContentSource,
                                         InMemorySemanticRecordStore semanticRecordStore,
                                         AnalysisCache analysisCache,
                                         AnalysisLevelPolicy analysisLevelPolicy,
                                         AnalysisCompletionListener lifecycleCompletionListener,
                                         Clock sfsClock) {
        return new SemanticEngine(engineContentSource, semanticRecordStore, analysisCache,
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
