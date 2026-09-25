package com.sfs.ui.mock;

import com.sfs.adapters.registry.AdapterRegistry;
import com.sfs.adapters.resolve.AdapterResolver;
import com.sfs.adapters.text.TextAdapter;
import com.sfs.engine.cache.AnalysisCache;
import com.sfs.engine.core.AnalysisCompletionListener;
import com.sfs.engine.core.AnalysisInput;
import com.sfs.engine.core.AnalysisInputProvider;
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
        AnalysisInputProvider inputProvider = objectId -> lifecycle.registeredFile(objectId)
                .flatMap(file -> raw.retrieve(objectId)
                        .map(bytes -> new AnalysisInput(objectId,
                                file.metadata().fileName(),
                                file.metadata().contentType(),
                                bytes)));
        AdapterRegistry registry = new AdapterRegistry();
        registry.register(new TextAdapter());
        SemanticEngine engine = new SemanticEngine(
                inputProvider,
                new AdapterResolver(registry),
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
