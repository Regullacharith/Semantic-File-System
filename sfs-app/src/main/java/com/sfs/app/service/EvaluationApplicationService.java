package com.sfs.app.service;

import com.sfs.app.api.response.FidelityReportResponse;
import com.sfs.contracts.evaluation.EvaluationAvailability;
import com.sfs.contracts.evaluation.EvaluationService;

import java.util.List;
import java.util.Objects;

public class EvaluationApplicationService {

    private final EvaluationService evaluationService;

    public EvaluationApplicationService(EvaluationService evaluationService) {
        this.evaluationService =
                Objects.requireNonNull(evaluationService, "evaluationService must not be null");
    }

    public List<FidelityReportResponse> listEvaluations() {
        return evaluationService.listEvaluations().stream()
                .map(availability -> FidelityReportResponse.from(
                        availability.report() == null ? null : availability.report().jobId(),
                        availability))
                .toList();
    }

    public FidelityReportResponse getEvaluation(String jobId) {
        String validated = JobId.validate(jobId);

        EvaluationAvailability availability = evaluationService.findEvaluation(validated);

        if (availability.status() == EvaluationAvailability.Status.NOT_EVALUATED) {
            throw ApplicationException.evaluationNotFound();
        }

        return FidelityReportResponse.from(validated, availability);
    }
}
