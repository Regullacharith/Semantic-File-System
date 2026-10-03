package com.sfs.evaluation;

import com.sfs.contracts.evaluation.EvaluationAvailability;
import com.sfs.contracts.evaluation.EvaluationService;
import com.sfs.contracts.evaluation.FidelityReportView;
import com.sfs.contracts.reconstruction.ReconstructionService;
import com.sfs.core.dna.DnaRepository;
import com.sfs.lifecycle.store.RawContentStore;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

public final class FidelityEvaluationService implements EvaluationService {

    private final ReconstructionService reconstructionService;
    private final RawContentStore rawContentStore;
    private final DnaRepository dnaRepository;
    private final FidelityEvaluator evaluator = new FidelityEvaluator();
    private final java.util.concurrent.atomic.AtomicLong evaluationsServed =
            new java.util.concurrent.atomic.AtomicLong();

    public FidelityEvaluationService(ReconstructionService reconstructionService,
                                     RawContentStore rawContentStore,
                                     DnaRepository dnaRepository) {
        this.reconstructionService = Objects.requireNonNull(reconstructionService,
                "reconstructionService must not be null");
        this.rawContentStore = Objects.requireNonNull(rawContentStore,
                "rawContentStore must not be null");
        this.dnaRepository = Objects.requireNonNull(dnaRepository,
                "dnaRepository must not be null");
    }

    @Override
    public EvaluationAvailability findEvaluation(String jobId) {
        return reconstructionService.findJob(jobId)
                .map(job -> {
                    evaluationsServed.incrementAndGet();
                    return evaluate(job.jobId(), job.objectId(), job.hasArtifact());
                })
                .orElseGet(EvaluationAvailability::notEvaluated);
    }

    @Override
    public List<EvaluationAvailability> listEvaluations() {
        return reconstructionService.listJobs().stream()
                .map(job -> evaluate(job.jobId(), job.objectId(), job.hasArtifact()))
                .toList();
    }

    private EvaluationAvailability evaluate(String jobId, String objectId,
                                            boolean hasArtifact) {
        if (!hasArtifact) {
            return EvaluationAvailability.noArtifact();
        }
        byte[] original = rawContentStore.retrieve(objectId).orElse(null);
        if (original == null) {
            return EvaluationAvailability.originalUnavailable();
        }
        com.sfs.core.dna.StoredDna stored = dnaRepository.find(objectId)
                .orElseThrow(() -> new IllegalStateException(
                        "a completed reconstruction exists for " + objectId
                                + " but its semantic memory is missing; refusing "
                                + "to estimate fidelity"));
        EvaluationInput input = EvaluationInput.of(objectId,
                new String(original, StandardCharsets.UTF_8),
                reconstructionService.findArtifact(jobId)
                        .orElseThrow(() -> new IllegalStateException(
                                "artifact disappeared for job " + jobId))
                        .content(),
                stored.dna());
        FidelityReport report = evaluator.evaluate(jobId, input);
        return EvaluationAvailability.available(
                report.toView(evaluator.findings(report)));
    }

    public String evaluatorVersion() {
        return FidelityEvaluator.EVALUATOR_VERSION;
    }

    public long evaluationsServed() {
        return evaluationsServed.get();
    }
}
