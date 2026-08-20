package com.sfs.contracts.evaluation;

import java.util.Objects;
import java.util.Optional;

/**
 * Whether fidelity could be measured for a reconstruction, and if not, why not.
 *
 * <p><strong>This type exists to prevent a fabricated score.</strong> Fidelity is the measured
 * difference between an original document and a reconstructed one. When the original no longer
 * exists — which is the normal end state for a memorized object — there is nothing to compare
 * against and no score can honestly be produced.
 *
 * <p>Returning an {@code Optional&lt;FidelityReportView&gt;} alone would leave the interface
 * unable to distinguish "not evaluated yet" from "cannot be evaluated, and here is why". That
 * ambiguity invites showing a placeholder number. This type makes the reason explicit and
 * displayable.
 *
 * @param status why a report is or is not available, never null
 * @param report the measured report, present only when {@link Status#AVAILABLE}
 * @param reason human-readable explanation when unavailable, never blank in that case
 */
public record EvaluationAvailability(Status status, FidelityReportView report, String reason) {

    public EvaluationAvailability {
        Objects.requireNonNull(status, "status must not be null");

        if (status == Status.AVAILABLE) {
            Objects.requireNonNull(report, "an available evaluation must carry a report");
        } else {
            if (report != null) {
                throw new IllegalArgumentException(
                        "a report may only accompany an available evaluation");
            }
            Objects.requireNonNull(reason, "an unavailable evaluation must explain why");
            if (reason.isBlank()) {
                throw new IllegalArgumentException("reason must not be blank");
            }
        }
    }

    /**
     * A measured report is available.
     *
     * @param report the report, never null
     * @return an available result
     */
    public static EvaluationAvailability available(FidelityReportView report) {
        return new EvaluationAvailability(Status.AVAILABLE, report, null);
    }

    /**
     * The original document no longer exists, so no comparison is possible.
     *
     * @return an unavailable result explaining the absence
     */
    public static EvaluationAvailability originalUnavailable() {
        return new EvaluationAvailability(Status.ORIGINAL_UNAVAILABLE, null,
                "The original file has been deleted, so there is nothing to compare the "
                        + "reconstruction against. Fidelity cannot be measured for this object, "
                        + "and no score is estimated.");
    }

    /**
     * The reconstruction did not produce an artifact, so there is nothing to evaluate.
     *
     * @return an unavailable result
     */
    public static EvaluationAvailability noArtifact() {
        return new EvaluationAvailability(Status.NO_ARTIFACT, null,
                "This reconstruction produced no artifact, so there is nothing to evaluate.");
    }

    /**
     * Evaluation has not been run for this job.
     *
     * @return an unavailable result
     */
    public static EvaluationAvailability notEvaluated() {
        return new EvaluationAvailability(Status.NOT_EVALUATED, null,
                "This reconstruction has not been evaluated yet.");
    }

    /**
     * Whether a measured report is present.
     *
     * @return {@code true} if a report can be displayed
     */
    public boolean isAvailable() {
        return status == Status.AVAILABLE;
    }

    /**
     * The report, if one was produced.
     *
     * @return the report, or empty
     */
    public Optional<FidelityReportView> reportIfAvailable() {
        return Optional.ofNullable(report);
    }

    /** Why a fidelity report is or is not available. */
    public enum Status {

        /** A comparison was performed and produced measurements. */
        AVAILABLE("Measured"),

        /**
         * The original file is gone, so no comparison is possible.
         *
         * <p>Expected for memorized objects, and not a defect.
         */
        ORIGINAL_UNAVAILABLE("Not measurable"),

        /** The reconstruction was rejected or failed, so no artifact exists to evaluate. */
        NO_ARTIFACT("No artifact"),

        /** Evaluation has not been run. */
        NOT_EVALUATED("Not evaluated");

        private final String label;

        Status(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }
}
