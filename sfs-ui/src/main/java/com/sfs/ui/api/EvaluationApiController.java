package com.sfs.ui.api;

import com.sfs.app.api.response.FidelityReportResponse;
import com.sfs.app.api.response.SecuritySettingsResponse;
import com.sfs.app.service.EvaluationApplicationService;
import com.sfs.app.service.SecurityApplicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class EvaluationApiController {

    private final EvaluationApplicationService evaluationApplicationService;
    private final SecurityApplicationService securityApplicationService;

    public EvaluationApiController(EvaluationApplicationService evaluationApplicationService,
                                   SecurityApplicationService securityApplicationService) {
        this.evaluationApplicationService = evaluationApplicationService;
        this.securityApplicationService = securityApplicationService;
    }

    @GetMapping("/evaluations")
    public List<FidelityReportResponse> listEvaluations() {
        return evaluationApplicationService.listEvaluations();
    }

    @GetMapping("/evaluations/{jobId}")
    public FidelityReportResponse getEvaluation(@PathVariable String jobId) {
        return evaluationApplicationService.getEvaluation(jobId);
    }

    @GetMapping("/security/settings")
    public SecuritySettingsResponse getSecuritySettings() {
        return securityApplicationService.getSettings();
    }
}
