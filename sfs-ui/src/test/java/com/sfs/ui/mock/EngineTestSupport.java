package com.sfs.ui.mock;

import com.sfs.engine.cache.AnalysisCache;
import com.sfs.engine.core.AnalysisCompletionListener;
import com.sfs.engine.core.AnalysisJob;
import com.sfs.engine.core.SemanticEngine;
import com.sfs.engine.level.AnalysisLevelPolicy;
import com.sfs.engine.record.InMemorySemanticRecordStore;
import com.sfs.lifecycle.core.AnalysisDispatcher;
import com.sfs.lifecycle.core.FileLifecycleManager;
import com.sfs.lifecycle.identity.ObjectIdService;
import com.sfs.lifecycle.store.InMemoryRawContentStore;
import com.sfs.ui.config.DevDataSeeder;

import java.time.Clock;

public final class EngineTestSupport {

    public record EngineSuite(
            FileLifecycleManager lifecycle,
            InMemorySemanticRecordStore records,
            SemanticEngine engine) {
    }

    private EngineTestSupport() {
    }

    public static EngineSuite seeded() {
        InMemoryRawContentStore raw = new InMemoryRawContentStore();
        FileLifecycleManager lifecycle = new FileLifecycleManager(
                Clock.systemUTC(), raw, DevDataSeeder.scriptedObjectIdService(), null);
        InMemorySemanticRecordStore records = new InMemorySemanticRecordStore();
        SemanticEngine engine = new SemanticEngine(
                raw::retrieve,
                records,
                new AnalysisCache(),
                AnalysisLevelPolicy.v1(),
                new AnalysisCompletionListener() {

                    @Override
                    public void onAnalysisSuccess(String objectId, String dnaVersion,
                                                  Long durationMs) {
                        lifecycle.completeAnalysisSuccess(objectId, dnaVersion, durationMs);
                    }

                    @Override
                    public void onAnalysisFailure(String objectId, String reason) {
                        lifecycle.completeAnalysisFailure(objectId, reason);
                    }
                },
                Clock.systemUTC());
        AnalysisDispatcher dispatcher = objectId -> {
            AnalysisJob job = engine.submit(objectId);
            return job.status() == AnalysisJob.Status.REJECTED ? null : job.jobId();
        };
        lifecycle.bindAnalysisDispatcher(dispatcher);
        try {
            new DevDataSeeder(lifecycle, engine).run(null);
        } catch (java.lang.InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("seed analysis was interrupted", e);
        } catch (Exception e) {
            throw new IllegalStateException("seed analysis failed", e);
        }
        return new EngineSuite(lifecycle, records, engine);
    }
}
