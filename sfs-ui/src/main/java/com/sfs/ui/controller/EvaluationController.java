package com.sfs.ui.controller;

import com.sfs.contracts.evaluation.EvaluationAvailability;
import com.sfs.contracts.evaluation.EvaluationService;
import com.sfs.contracts.evaluation.FidelityDimension;
import com.sfs.ui.view.NavigationItem;
import com.sfs.ui.view.PageViewModel;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;

/**
 * Evaluation and fidelity view.
 */
@Controller
public class EvaluationController {

    private static final String VIEW_EVALUATION = "evaluation";
    private static final String VIEW_REPORT = "evaluation-report";

    private static final String ATTR_PAGE = "page";
    private static final String ATTR_EVALUATIONS = "evaluations";
    private static final String ATTR_EVALUATION = "evaluation";
    private static final String ATTR_DIMENSIONS = "dimensions";

    private static final double CONCERN_THRESHOLD = 0.80;

    private final EvaluationService evaluationService;

    public EvaluationController(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @GetMapping("/evaluation")
    public String listEvaluations(Model model) {
        List<EvaluationAvailability> evaluations = evaluationService.listEvaluations();

        model.addAttribute(ATTR_PAGE, PageViewModel.of("Evaluation", NavigationItem.EVALUATION));
        model.addAttribute(ATTR_EVALUATIONS, evaluations);

        return VIEW_EVALUATION;
    }

    @GetMapping("/evaluation/{jobId}")
    public String showReport(@PathVariable String jobId, Model model) {
        EvaluationAvailability evaluation = evaluationService.findEvaluation(jobId);

        if (evaluation.status() == EvaluationAvailability.Status.NOT_EVALUATED) {
            throw new EvaluationNotFoundException();
        }

        model.addAttribute(ATTR_PAGE,
                PageViewModel.of("Fidelity report", NavigationItem.EVALUATION));
        model.addAttribute(ATTR_EVALUATION, evaluation);

        model.addAttribute(ATTR_DIMENSIONS, List.of(FidelityDimension.values()));
        model.addAttribute("concernThreshold", CONCERN_THRESHOLD);

        return VIEW_REPORT;
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    public static class EvaluationNotFoundException extends RuntimeException {

        public EvaluationNotFoundException() {
            super("No evaluation exists for that reconstruction job.");
        }
    }
}
