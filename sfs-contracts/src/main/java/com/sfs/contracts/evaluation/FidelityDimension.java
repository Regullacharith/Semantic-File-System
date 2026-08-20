package com.sfs.contracts.evaluation;

/**
 * The dimensions along which reconstruction fidelity is measured.
 *
 * <p><strong>These are reported separately, never collapsed into one number.</strong> The
 * specification is explicit that semantic, structural and factual/content fidelity are
 * measured independently, and that a semantic score must not hide factual failures. A
 * reconstruction can read beautifully and still lose a date, a quantity or an entity name;
 * one aggregate figure would conceal exactly the failure that matters most.
 *
 * <p>Each dimension declares whether a low score represents a <em>correctness</em> failure
 * rather than a stylistic difference, so the interface can distinguish "worded differently"
 * from "states something untrue".
 */
public enum FidelityDimension {

    /**
     * How closely the reconstructed meaning matches the source.
     *
     * <p>Tolerant of rewording by design: V1 targets semantic equivalence, not byte equality.
     */
    SEMANTIC("Semantic similarity",
            "How closely the reconstructed meaning matches the original.",
            false),

    /**
     * How closely hierarchy, section order and organisation match.
     *
     * <p>Structural drift is a real loss but not a false statement.
     */
    STRUCTURAL("Structural similarity",
            "How closely hierarchy, section order and organisation are preserved.",
            false),

    /**
     * Whether claims, numbers, dates and quantities survived intact.
     *
     * <p>A correctness dimension: a lost or altered fact means the artifact asserts something
     * the original did not.
     */
    FACTUAL("Factual / content fidelity",
            "Whether claims, numbers, dates and quantities are preserved.",
            true),

    /** Whether named entities survived without substitution or invention. */
    ENTITY("Entity preservation",
            "Whether named entities are preserved without substitution.",
            true),

    /** Whether typed, directional relationships survived with their direction intact. */
    RELATIONSHIP("Relationship preservation",
            "Whether relationships are preserved with their direction intact.",
            true),

    /** How much of the required semantic information survived reconstruction. */
    COMPLETENESS("Completeness",
            "How much of the required semantic information survived.",
            false);

    private final String label;
    private final String description;
    private final boolean correctnessCritical;

    FidelityDimension(String label, String description, boolean correctnessCritical) {
        this.label = label;
        this.description = description;
        this.correctnessCritical = correctnessCritical;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Whether a shortfall in this dimension means the artifact states something incorrect,
     * as opposed to merely differing in wording or arrangement.
     *
     * @return {@code true} for factual, entity and relationship dimensions
     */
    public boolean isCorrectnessCritical() {
        return correctnessCritical;
    }
}
