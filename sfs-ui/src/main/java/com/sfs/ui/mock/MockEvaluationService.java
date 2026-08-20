package com.sfs.ui.mock;

import com.sfs.contracts.evaluation.EvaluationAvailability;
import com.sfs.contracts.evaluation.EvaluationService;
import com.sfs.contracts.evaluation.FidelityDimension;
import com.sfs.contracts.evaluation.FidelityReportView;
import com.sfs.contracts.evaluation.FidelityReportView.EvaluationFinding;
import com.sfs.contracts.file.FileService;
import com.sfs.contracts.file.FileStatus;
import com.sfs.contracts.reconstruction.ReconstructionJobView;
import com.sfs.contracts.reconstruction.ReconstructionService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * In-memory stand-in for the Evaluation and Fidelity subsystem.
 */
@Service
@Profile("mock")
public class MockEvaluationService implements EvaluationService {

    private static final String EVALUATOR_VERSION = "mock-evaluator/0.1";

    private final ReconstructionService reconstructionService;
    private final FileService fileService;

    public MockEvaluationService(ReconstructionService reconstructionService,
                                 FileService fileService) {
        this.reconstructionService = reconstructionService;
        this.fileService = fileService;
    }

    @Override
    public EvaluationAvailability findEvaluation(String jobId) {
        return reconstructionService.findJob(jobId)
                .map(this::evaluate)
                .orElseGet(EvaluationAvailability::notEvaluated);
    }

    @Override
    public List<EvaluationAvailability> listEvaluations() {
        return reconstructionService.listJobs().stream()
                .map(this::evaluate)
                .toList();
    }

    private EvaluationAvailability evaluate(ReconstructionJobView job) {
        if (!job.hasArtifact()) {
            return EvaluationAvailability.noArtifact();
        }

        boolean originalGone = fileService.findByObjectId(job.objectId())
                .map(file -> file.status() == FileStatus.MEMORIZED)
                .orElse(true);

        if (originalGone) {
            return EvaluationAvailability.originalUnavailable();
        }

        return EvaluationAvailability.available(buildReport(job));
    }

    private FidelityReportView buildReport(ReconstructionJobView job) {
        Map<FidelityDimension, Double> scores = new EnumMap<>(FidelityDimension.class);
        scores.put(FidelityDimension.SEMANTIC, 0.91);
        scores.put(FidelityDimension.STRUCTURAL, 0.88);
        scores.put(FidelityDimension.FACTUAL, 0.76);
        scores.put(FidelityDimension.ENTITY, 0.94);
        scores.put(FidelityDimension.RELATIONSHIP, 0.83);
        scores.put(FidelityDimension.COMPLETENESS, 0.87);

        List<EvaluationFinding> findings = List.of(
                new EvaluationFinding(FidelityDimension.SEMANTIC, true,
                        "Overall meaning of each section is preserved."),
                new EvaluationFinding(FidelityDimension.STRUCTURAL, true,
                        "All three sections appear in their original order."),
                new EvaluationFinding(FidelityDimension.FACTUAL, true,
                        "The 40 percent latency reduction is preserved exactly."),
                new EvaluationFinding(FidelityDimension.FACTUAL, false,
                        "A supporting measurement from the original is absent from the "
                                + "reconstruction."),
                new EvaluationFinding(FidelityDimension.ENTITY, true,
                        "Entity names are reproduced without substitution."),
                new EvaluationFinding(FidelityDimension.RELATIONSHIP, false,
                        "One relationship is present but its direction is ambiguous in the "
                                + "reconstructed wording."));

        return new FidelityReportView(
                job.jobId(),
                job.objectId(),
                scores,
                0.67, 3, 2,
                findings,
                15_360,
                job.artifactBytes(),
                EVALUATOR_VERSION,
                Instant.now());
    }
}
