package com.sfs.ui.controller;

import com.sfs.contracts.evaluation.EvaluationAvailability;
import com.sfs.contracts.evaluation.EvaluationService;
import com.sfs.contracts.evaluation.FidelityDimension;
import com.sfs.contracts.evaluation.FidelityReportView;
import com.sfs.contracts.evaluation.FidelityReportView.EvaluationFinding;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Verifies the evaluation and fidelity view.
 */
@WebMvcTest(EvaluationController.class)
@DisplayName("Evaluation and fidelity view")
class EvaluationControllerTest {

    private static final String JOB_ID = "job-0001";
    private static final String OBJECT_ID = "sfs-obj-0001-a1b2c3d4";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EvaluationService evaluationService;

    private static FidelityReportView report(double factual, int criticalPreserved) {
        Map<FidelityDimension, Double> scores = new EnumMap<>(FidelityDimension.class);
        scores.put(FidelityDimension.SEMANTIC, 0.91);
        scores.put(FidelityDimension.STRUCTURAL, 0.88);
        scores.put(FidelityDimension.FACTUAL, factual);
        scores.put(FidelityDimension.ENTITY, 0.94);
        scores.put(FidelityDimension.RELATIONSHIP, 0.83);
        scores.put(FidelityDimension.COMPLETENESS, 0.87);

        return new FidelityReportView(JOB_ID, OBJECT_ID, scores,
                criticalPreserved / 3.0, 3, criticalPreserved,
                List.of(new EvaluationFinding(FidelityDimension.FACTUAL, false,
                        "A supporting measurement is absent from the reconstruction.")),
                15_360, 2_048, "mock-evaluator/0.1", Instant.now());
    }

    // ------------------------------------------------------------ list view

    @Test
    @DisplayName("lists evaluation outcomes")
    void listsEvaluations() throws Exception {
        given(evaluationService.listEvaluations())
                .willReturn(List.of(EvaluationAvailability.available(report(0.76, 2))));

        mockMvc.perform(get("/evaluation"))
                .andExpect(status().isOk())
                .andExpect(view().name("evaluation"))
                .andExpect(content().string(containsString(JOB_ID)));
    }

    @Test
    @DisplayName("lists unmeasurable outcomes rather than omitting them")
    void listsUnmeasurableOutcomes() throws Exception {
        given(evaluationService.listEvaluations())
                .willReturn(List.of(EvaluationAvailability.originalUnavailable()));

        mockMvc.perform(get("/evaluation"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Not measurable")))
                .andExpect(content().string(containsString("original file has been deleted")));
    }

    @Test
    @DisplayName("shows an empty state when nothing has been evaluated")
    void showsEmptyState() throws Exception {
        given(evaluationService.listEvaluations()).willReturn(List.of());

        mockMvc.perform(get("/evaluation"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No reconstructions have been evaluated")));
    }

    // ---------------------------------------------------------- report view

    @Test
    @DisplayName("reports every dimension separately")
    void reportsEveryDimensionSeparately() throws Exception {
        given(evaluationService.findEvaluation(JOB_ID))
                .willReturn(EvaluationAvailability.available(report(0.76, 2)));

        var actions = mockMvc.perform(get("/evaluation/" + JOB_ID))
                .andExpect(status().isOk())
                .andExpect(view().name("evaluation-report"));

        for (FidelityDimension dimension : FidelityDimension.values()) {
            actions.andExpect(content().string(containsString(dimension.getLabel())));
        }

        actions.andExpect(content().string(containsString("91%")))  // semantic
               .andExpect(content().string(containsString("76%")))  // factual
               .andExpect(content().string(containsString("94%"))); // entity
    }

    @Test
    @DisplayName("presents no single aggregate fidelity figure")
    void presentsNoAggregateScore() throws Exception {
        given(evaluationService.findEvaluation(JOB_ID))
                .willReturn(EvaluationAvailability.available(report(0.76, 2)));

        mockMvc.perform(get("/evaluation/" + JOB_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Overall fidelity"))))
                .andExpect(content().string(containsString("never combined into a single figure")));
    }

    @Test
    @DisplayName("shows critical fact loss separately from the factual score")
    void showsCriticalFactLossSeparately() throws Exception {
        given(evaluationService.findEvaluation(JOB_ID))
                .willReturn(EvaluationAvailability.available(report(0.76, 2)));

        mockMvc.perform(get("/evaluation/" + JOB_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("2 of 3")))
                .andExpect(content().string(containsString("At least one critical fact was lost")));
    }

    @Test
    @DisplayName("flags a correctness-critical dimension below the concern threshold")
    void flagsCorrectnessConcern() throws Exception {
        given(evaluationService.findEvaluation(JOB_ID))
                .willReturn(EvaluationAvailability.available(report(0.55, 3)));

        mockMvc.perform(get("/evaluation/" + JOB_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("correctness concern")));
    }

    @Test
    @DisplayName("reports storage alongside fidelity, not on its own")
    void reportsStorageAlongsideFidelity() throws Exception {
        given(evaluationService.findEvaluation(JOB_ID))
                .willReturn(EvaluationAvailability.available(report(0.76, 2)));

        mockMvc.perform(get("/evaluation/" + JOB_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Storage and fidelity together")))
                .andExpect(content().string(containsString("knowledge preservation density")));
    }

    @Test
    @DisplayName("states that the displayed figures were not measured")
    void disclosesMockEvaluator() throws Exception {
        given(evaluationService.findEvaluation(JOB_ID))
                .willReturn(EvaluationAvailability.available(report(0.76, 2)));

        mockMvc.perform(get("/evaluation/" + JOB_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Nothing was measured")));
    }

    // ------------------------------------------------------- unmeasurable

    @Test
    @DisplayName("shows no score at all when the original no longer exists")
    void showsNoScoreWhenOriginalIsGone() throws Exception {
        given(evaluationService.findEvaluation(JOB_ID))
                .willReturn(EvaluationAvailability.originalUnavailable());

        mockMvc.perform(get("/evaluation/" + JOB_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("nothing to compare")))
                .andExpect(content().string(containsString("no score is estimated")))
                .andExpect(content().string(not(containsString("score-bar"))))
                .andExpect(content().string(not(containsString("Fidelity by dimension"))));
    }

    @Test
    @DisplayName("explains that a rejected reconstruction has nothing to evaluate")
    void explainsMissingArtifact() throws Exception {
        given(evaluationService.findEvaluation(JOB_ID))
                .willReturn(EvaluationAvailability.noArtifact());

        mockMvc.perform(get("/evaluation/" + JOB_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("produced no artifact")))
                .andExpect(content().string(not(containsString("score-bar"))));
    }

    @Test
    @DisplayName("returns 404 for a job that has no evaluation")
    void unknownJobReturns404() throws Exception {
        given(evaluationService.findEvaluation(any()))
                .willReturn(EvaluationAvailability.notEvaluated());

        mockMvc.perform(get("/evaluation/job-9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("is read-only and cannot trigger re-evaluation")
    void isReadOnly() throws Exception {
        given(evaluationService.findEvaluation(JOB_ID))
                .willReturn(EvaluationAvailability.available(report(0.76, 2)));

        mockMvc.perform(get("/evaluation/" + JOB_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("method=\"post\""))));

        mockMvc.perform(post("/evaluation/" + JOB_ID))
                .andExpect(status().is4xxClientError());
    }
}
