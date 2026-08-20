package com.sfs.contracts.evaluation;

import java.util.List;

/**
 * Application-facing contract for reconstruction evaluation.
 *
 * <p><strong>Read-only.</strong> Evaluation observes; it does not reconstruct. This interface
 * declares no method that produces or modifies an artifact.
 *
 * <p><strong>Every result is honest about measurability.</strong> Methods return
 * {@link EvaluationAvailability} rather than a bare report, so an implementation cannot
 * quietly substitute an estimate when no original document survives for comparison.
 *
 * <p><strong>Ownership.</strong> Milestone 12 owns the evaluators, the benchmark corpus and
 * the regression harness. This interface declares only what the Milestone 01 fidelity view
 * requires.
 */
public interface EvaluationService {

    /**
     * Retrieves the evaluation for one reconstruction job.
     *
     * @param jobId reconstruction job identity
     * @return the measured report, or an explanation of why none exists
     */
    EvaluationAvailability findEvaluation(String jobId);

    /**
     * Lists every evaluation outcome, most recent first.
     *
     * <p>Includes unmeasurable cases, so the interface can show how often fidelity could not
     * be measured rather than silently omitting those jobs.
     *
     * @return an immutable list, never null, possibly empty
     */
    List<EvaluationAvailability> listEvaluations();
}
