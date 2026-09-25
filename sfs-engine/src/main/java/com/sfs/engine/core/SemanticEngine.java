package com.sfs.engine.core;

import com.sfs.adapters.resolve.AdapterResolver;
import com.sfs.adapters.spi.AdapterRequest;
import com.sfs.adapters.spi.AdapterResult;
import com.sfs.adapters.spi.FileTypeAdapter;
import com.sfs.engine.cache.AnalysisCache;
import com.sfs.engine.level.AnalysisLevel;
import com.sfs.engine.level.AnalysisLevelPolicy;
import com.sfs.engine.pipeline.PipelineResult;
import com.sfs.engine.pipeline.SemanticPipeline;
import com.sfs.engine.record.InMemorySemanticRecordStore;

import java.time.Clock;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class SemanticEngine implements AutoCloseable {

    public static final String ENGINE_VERSION = "sfs-engine/0.1";

    private final AnalysisInputProvider inputProvider;
    private final AdapterResolver adapterResolver;
    private final InMemorySemanticRecordStore recordStore;
    private final AnalysisCache cache;
    private final AnalysisLevelPolicy levelPolicy;
    private final AnalysisCompletionListener completionListener;
    private final Clock clock;
    private final SemanticPipeline pipeline = SemanticPipeline.v1();
    private final AnalysisJobRegistry jobRegistry = new AnalysisJobRegistry();
    private final ExecutorService worker;

    public SemanticEngine(AnalysisInputProvider inputProvider,
                          AdapterResolver adapterResolver,
                          InMemorySemanticRecordStore recordStore,
                          AnalysisCache cache,
                          AnalysisLevelPolicy levelPolicy,
                          AnalysisCompletionListener completionListener,
                          Clock clock) {
        this.inputProvider = Objects.requireNonNull(inputProvider, "inputProvider must not be null");
        this.adapterResolver = Objects.requireNonNull(adapterResolver, "adapterResolver must not be null");
        this.recordStore = Objects.requireNonNull(recordStore, "recordStore must not be null");
        this.cache = Objects.requireNonNull(cache, "cache must not be null");
        this.levelPolicy = Objects.requireNonNull(levelPolicy, "levelPolicy must not be null");
        this.completionListener = completionListener == null
                ? new AnalysisCompletionListener() { }
                : completionListener;
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.worker = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "semantic-engine-worker");
            thread.setDaemon(true);
            return thread;
        });
    }

    public AnalysisJob submit(String objectId) {
        return submit(objectId, levelPolicy.defaultLevel());
    }

    public AnalysisJob submit(String objectId, AnalysisLevel level) {
        AnalysisJob rejected = checkPreconditions(objectId, level);
        if (rejected != null) {
            return rejected;
        }
        AnalysisJob queued = jobRegistry.create(objectId, level, clock.instant());
        worker.execute(() -> runJob(queued));
        return queued;
    }

    public AnalysisJob analyzeNow(String objectId) {
        return analyzeNow(objectId, levelPolicy.defaultLevel());
    }

    public AnalysisJob analyzeNow(String objectId, AnalysisLevel level) {
        AnalysisJob rejected = checkPreconditions(objectId, level);
        if (rejected != null) {
            return rejected;
        }
        AnalysisJob queued = jobRegistry.create(objectId, level, clock.instant());
        return runJob(queued);
    }

    private AnalysisJob checkPreconditions(String objectId, AnalysisLevel level) {
        Objects.requireNonNull(objectId, "objectId must not be null");
        if (!levelPolicy.isEnabled(level)) {
            AnalysisJob queued = jobRegistry.create(objectId, level, clock.instant());
            return reject(queued, levelPolicy.refusalReason(level));
        }
        if (jobRegistry.hasActiveJob(objectId)) {
            AnalysisJob queued = jobRegistry.create(objectId, level, clock.instant());
            return reject(queued, "Another analysis job is already active for this object.");
        }
        return null;
    }

    private AnalysisJob runJob(AnalysisJob queued) {
        markRunning(queued);
        try {
            AnalysisInput input = inputProvider.input(queued.objectId())
                    .orElseThrow(() -> new IllegalStateException(
                            "No raw content is available for analysis."));
            String rawSha256 = Digests.sha256Hex(input.content());

            if (reusePriorWork(queued.objectId(), rawSha256)) {
                String dnaVersion = cache.get(queued.objectId()).orElseThrow().dnaVersion();
                AnalysisJob reused = complete(queued.jobId(), AnalysisJob.Status.REUSED,
                        true, null);
                completionListener.onAnalysisReused(queued.objectId(), dnaVersion);
                return reused;
            }

            FileTypeAdapter adapter = adapterResolver.resolve(
                    input.fileName(), input.contentType());
            AdapterResult adapted = adapter.adapt(new AdapterRequest(
                    queued.objectId(), input.fileName(), input.contentType(),
                    input.content()));

            SemanticContext context = new SemanticContext(
                    queued.objectId(), adapted.normalizedText(),
                    Digests.sha256Hex(adapted.normalizedText()),
                    queued.level(), ENGINE_VERSION,
                    recordStore.nextDnaVersion(queued.objectId()),
                    clock.instant());
            PipelineResult result = pipeline.run(context, adapted.structure());
            if (!result.success()) {
                return fail(queued.jobId(), result.failureReason());
            }
            recordStore.save(result.dna());
            cache.put(new AnalysisCache.CacheEntry(
                    queued.objectId(), rawSha256, ENGINE_VERSION,
                    versionString(result.dna())));
            completionListener.onAnalysisSuccess(
                    queued.objectId(),
                    versionString(result.dna()),
                    result.stageDurationsMs().values().stream()
                            .mapToLong(Long::longValue).sum());
            return complete(queued.jobId(), AnalysisJob.Status.COMPLETED,
                    false, result.stageDurationsMs());
        } catch (RuntimeException e) {
            String reason = e.getMessage() == null
                    ? e.getClass().getSimpleName()
                    : e.getMessage();
            return fail(queued.jobId(), reason);
        }
    }

    private boolean reusePriorWork(String objectId, String contentSha256) {
        return cache.get(objectId)
                .map(entry -> entry.contentSha256().equals(contentSha256)
                        && entry.engineVersion().equals(ENGINE_VERSION)
                        && recordStore.findSemanticDna(objectId).isPresent())
                .orElse(false);
    }

    private void markRunning(AnalysisJob queued) {
        jobRegistry.update(queued.jobId(), job -> new AnalysisJob(
                job.jobId(), job.objectId(), job.level(), AnalysisJob.Status.RUNNING,
                job.submittedAt(), clock.instant(), null, null, false, null));
    }

    private AnalysisJob complete(String jobId, AnalysisJob.Status status,
                                 boolean reusedPriorWork, java.util.Map<String, Long> durations) {
        return jobRegistry.update(jobId, job -> new AnalysisJob(
                job.jobId(), job.objectId(), job.level(), status,
                job.submittedAt(), job.startedAt(), clock.instant(),
                null, reusedPriorWork, durations));
    }

    private AnalysisJob fail(String jobId, String reason) {
        AnalysisJob failed = jobRegistry.update(jobId, job -> new AnalysisJob(
                job.jobId(), job.objectId(), job.level(), AnalysisJob.Status.FAILED,
                job.submittedAt(), job.startedAt(), clock.instant(), reason, false, null));
        jobRegistry.find(jobId).ifPresent(job ->
                completionListener.onAnalysisFailure(job.objectId(), reason));
        return failed;
    }

    private AnalysisJob reject(AnalysisJob queued, String reason) {
        return jobRegistry.update(queued.jobId(), job -> new AnalysisJob(
                job.jobId(), job.objectId(), job.level(), AnalysisJob.Status.REJECTED,
                job.submittedAt(), null, clock.instant(), reason, false, null));
    }

    private static String versionString(com.sfs.contracts.semantic.SemanticDnaView dna) {
        return dna.schemaVersion() + " v" + dna.dnaVersion();
    }

    public void addJobListener(AnalysisJobListener listener) {
        jobRegistry.addListener(listener);
    }

    public AnalysisJobRegistry jobRegistry() {
        return jobRegistry;
    }

    public AnalysisLevelPolicy levelPolicy() {
        return levelPolicy;
    }

    @Override
    public void close() {
        worker.shutdownNow();
    }
}
