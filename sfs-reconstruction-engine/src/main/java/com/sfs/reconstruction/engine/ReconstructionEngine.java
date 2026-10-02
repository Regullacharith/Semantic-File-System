package com.sfs.reconstruction.engine;

import com.sfs.contracts.reconstruction.ReconstructionArtifact;
import com.sfs.contracts.reconstruction.ReconstructionJobView;
import com.sfs.contracts.reconstruction.ReconstructionService;
import com.sfs.contracts.reconstruction.ReconstructionStatus;
import com.sfs.core.dna.DnaCanonical;
import com.sfs.core.rules.ReconstructionPlan;
import com.sfs.core.rules.ReconstructionPlanner;
import com.sfs.core.rules.RulePlanningException;
import com.sfs.reconstruction.ConstraintInterface;
import com.sfs.reconstruction.ModelInput;
import com.sfs.reconstruction.ModelOutput;
import com.sfs.reconstruction.SFSReconstructionModel;
import com.sfs.reconstruction.encode.EncodedFact;
import com.sfs.reconstruction.encode.EncodedEntity;
import com.sfs.reconstruction.encode.EncodedRelationship;
import com.sfs.reconstruction.encode.SemanticDnaEncoder;
import com.sfs.reconstruction.encode.UnifiedRepresentation;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public final class ReconstructionEngine implements ReconstructionService {

    public static final String ENGINE_ID = "sfs-reconstruction-engine/0.1";

    private final DnaRuleLoader loader;
    private final ReconstructionPlanner planner;
    private final SFSReconstructionModel model;
    private final ConstraintInterface constraints;
    private final ReconstructionPostProcessor postProcessor;
    private final ReconstructionArtifactFactory artifactFactory;
    private final SemanticDnaEncoder encoder = new SemanticDnaEncoder();

    private final Map<String, ReconstructionJob> jobs = new ConcurrentHashMap<>();
    private final Map<String, ReconstructionArtifact> artifacts = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();
    private final ExecutorService worker;

    public ReconstructionEngine(DnaRuleLoader loader,
                                ReconstructionPlanner planner,
                                SFSReconstructionModel model,
                                ConstraintInterface constraints,
                                ReconstructionPostProcessor postProcessor,
                                ReconstructionArtifactFactory artifactFactory) {
        this.loader = Objects.requireNonNull(loader, "loader must not be null");
        this.planner = Objects.requireNonNull(planner, "planner must not be null");
        this.model = Objects.requireNonNull(model, "model must not be null");
        this.constraints = Objects.requireNonNull(constraints,
                "constraints must not be null");
        this.postProcessor = Objects.requireNonNull(postProcessor,
                "postProcessor must not be null");
        this.artifactFactory = Objects.requireNonNull(artifactFactory,
                "artifactFactory must not be null");
        this.worker = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "sfs-reconstruction-engine");
            thread.setDaemon(true);
            return thread;
        });
    }

    @Override
    public ReconstructionJobView requestReconstruction(String objectId) {
        Instant now = Instant.now();
        String jobId = "job-%04d".formatted(sequence.incrementAndGet());

        ReconstructedSource source;
        try {
            source = loader.load(objectId);
        } catch (ReconstructionRefusalException e) {
            return store(ReconstructionJob.refused(
                    jobId, objectId == null ? "unavailable" : objectId,
                    "unavailable", now, e.getMessage())).toView();
        }

        if (source.dna().dna().security().containsProtectedReferences()) {
            ReconstructionJobView.ConstraintFinding finding =
                    new ReconstructionJobView.ConstraintFinding(
                            ReconstructionJobView.ConstraintFinding.Severity.VIOLATION,
                            "Protected values",
                            source.dna().dna().security().protectedReferences().size()
                                    + " protected value(s) are withheld from semantic "
                                    + "memory and resolve only under authorization. "
                                    + "Reconstruction is refused before it runs.");
            return store(new ReconstructionJob(
                    jobId, objectId, source.file().displayName(),
                    ReconstructionStatus.REJECTED, source.dnaVersion(),
                    "pending", model.modelId(), now, now, null, 0,
                    List.of(finding), null, null, null)).toView();
        }

        ReconstructionJob queued = new ReconstructionJob(
                jobId, objectId, source.file().displayName(),
                ReconstructionStatus.QUEUED, source.dnaVersion(), "pending",
                model.modelId(), now, null, null, 0, List.of(), null, null, null);
        store(queued);
        worker.execute(() -> run(jobId, source));
        return queued.toView();
    }

    private void run(String jobId, ReconstructedSource source) {
        mutate(jobId, job -> job.terminal() ? job : job.running(Instant.now()));
        try {
            long planStart = System.nanoTime();
            ReconstructionPlan plan = planner.plan(source.dna().dna());
            long planNanos = System.nanoTime() - planStart;
            mutate(jobId, job -> job.terminal() ? job
                    : job.withRules(plan.rulesVersion()));

            ModelInput input = ModelInput.of(source.dna().dna(), plan);
            long modelStart = System.nanoTime();
            ModelOutput output = model.reconstruct(input);
            long modelNanos = System.nanoTime() - modelStart;

            long verifyStart = System.nanoTime();
            ConstraintInterface.Result result =
                    constraints.judge(input, output.draftText());
            UnifiedRepresentation representation = encoder.encode(input);
            long verifyNanos = System.nanoTime() - verifyStart;

            VerificationResult verification = VerificationResult.from(result,
                    representation.content().facts().stream()
                            .filter(EncodedFact::critical).count() > 0
                            ? (int) representation.content().facts().stream()
                            .filter(EncodedFact::critical).count()
                            : 0,
                    representation.content().entities().size(),
                    representation.relationships().size());

            List<ReconstructionJobView.ConstraintFinding> findings =
                    findingsOf(result, plan);

            if (!result.satisfied()) {
                mutate(jobId, job -> job.terminal() ? job
                        : job.rejected(Instant.now(), findings, verification));
                return;
            }

            String labeled = postProcessor.label(input.objectId(),
                    source.dnaVersion(), plan.rulesVersion(), output.modelId(),
                    output.draftText());
            ReconstructionArtifact artifact = artifactFactory.create(
                    jobId, source.file().displayName(), labeled);
            int dnaBytes = DnaCanonical.serialize(source.dna().dna())
                    .getBytes(StandardCharsets.UTF_8).length;
            ReconstructionMetadata metadata = new ReconstructionMetadata(
                    input.objectId(), source.dnaVersion(), plan.dnaSha256(),
                    plan.rulesVersion(), output.modelId(), planNanos, modelNanos,
                    verifyNanos, dnaBytes,
                    labeled.getBytes(StandardCharsets.UTF_8).length);

            Instant at = Instant.now();
            mutate(jobId, job -> job.terminal() ? job
                    : job.completed(at, artifact.fileName(), artifactBytes(artifact),
                            findings, verification, metadata));
            artifacts.put(jobId, artifact);
        } catch (Exception e) {
            String reason = worker.isShutdown()
                    ? "The reconstruction engine stopped before this job finished; "
                            + "request the object again."
                    : "Reconstruction failed: " + e.getClass().getSimpleName();
            mutate(jobId, job -> job.terminal() ? job
                    : job.failed(Instant.now(), reason));
        }
    }

    private List<ReconstructionJobView.ConstraintFinding> findingsOf(
            ConstraintInterface.Result result, ReconstructionPlan plan) {
        List<ReconstructionJobView.ConstraintFinding> findings = new ArrayList<>();
        findings.add(new ReconstructionJobView.ConstraintFinding(
                result.satisfied()
                        ? ReconstructionJobView.ConstraintFinding.Severity.SATISFIED
                        : ReconstructionJobView.ConstraintFinding.Severity.VIOLATION,
                "Plan rules (" + plan.rulesVersion() + ")",
                result.satisfied()
                        ? "All declarative rule checks passed: "
                                + plan.requiredFacts().size() + " required fact(s), "
                                + plan.requiredEntities().size() + " required entit(ies), "
                                + plan.requiredRelationships().size()
                                + " required relationship(s)"
                                + (plan.sectionOrder().isEmpty()
                                        ? ""
                                        : ", " + plan.sectionOrder().size()
                                                + " section(s) in order")
                                + "; every critical fact and traceability check passed."
                        : "Rule violations: " + String.join("; ", result.violations())));
        for (String warning : result.warnings()) {
            findings.add(new ReconstructionJobView.ConstraintFinding(
                    ReconstructionJobView.ConstraintFinding.Severity.WARNING,
                    "Plan warning", warning));
        }
        return findings;
    }

    private long artifactBytes(ReconstructionArtifact artifact) {
        return artifact.content().getBytes(StandardCharsets.UTF_8).length;
    }

    private ReconstructionJob store(ReconstructionJob job) {
        jobs.put(job.jobId(), job);
        return job;
    }

    private void mutate(String jobId, java.util.function.UnaryOperator<ReconstructionJob> transition) {
        jobs.computeIfPresent(jobId, (id, job) -> transition.apply(job));
    }

    @Override
    public Optional<ReconstructionJobView> findJob(String jobId) {
        if (jobId == null || jobId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(jobs.get(jobId)).map(ReconstructionJob::toView);
    }

    public Optional<ReconstructionJob> findJobRecord(String jobId) {
        if (jobId == null || jobId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(jobs.get(jobId));
    }

    @Override
    public List<ReconstructionJobView> listJobs() {
        return jobs.values().stream()
                .sorted(Comparator.comparing(ReconstructionJob::requestedAt).reversed()
                        .thenComparing(ReconstructionJob::jobId, Comparator.reverseOrder()))
                .map(ReconstructionJob::toView)
                .toList();
    }

    @Override
    public Optional<ReconstructionArtifact> findArtifact(String jobId) {
        if (jobId == null || jobId.isBlank()) {
            return Optional.empty();
        }
        return findJob(jobId)
                .filter(ReconstructionJobView::hasArtifact)
                .flatMap(job -> Optional.ofNullable(artifacts.get(jobId)));
    }

    public Map<String, Integer> diagnostics() {
        Map<String, Integer> counters = new ConcurrentHashMap<>();
        counters.put("engineJobsTotal", jobs.size());
        counters.put("engineJobsCompleted", count(ReconstructionStatus.COMPLETED));
        counters.put("engineJobsRejected", count(ReconstructionStatus.REJECTED));
        counters.put("engineJobsFailed", count(ReconstructionStatus.FAILED));
        counters.put("engineJobsInFlight", jobs.size()
                - count(ReconstructionStatus.COMPLETED)
                - count(ReconstructionStatus.REJECTED)
                - count(ReconstructionStatus.FAILED));
        counters.put("engineArtifactsAvailable", artifacts.size());
        return Map.copyOf(counters);
    }

    private int count(ReconstructionStatus status) {
        return (int) jobs.values().stream()
                .filter(job -> job.status() == status).count();
    }

    public void shutdown() {
        worker.shutdown();
        try {
            if (!worker.awaitTermination(5, TimeUnit.SECONDS)) {
                worker.shutdownNow();
            }
        } catch (InterruptedException e) {
            worker.shutdownNow();
            Thread.currentThread().interrupt();
        }
        Instant now = Instant.now();
        for (ReconstructionJob job : jobs.values()) {
            if (!job.terminal()) {
                mutate(job.jobId(), running -> running.terminal() ? running
                        : running.failed(now,
                        "The reconstruction engine stopped before this job "
                                + "finished; request the object again."));
            }
        }
    }
}
