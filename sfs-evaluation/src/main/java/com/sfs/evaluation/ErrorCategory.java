package com.sfs.evaluation;

public enum ErrorCategory {

    MISSING_SUMMARY("The reconstruction does not carry the recorded summary verbatim.",
            "Preserve the summary statement exactly as recorded in the Semantic DNA."),
    MISSING_HEADING("A recorded section heading is absent from the reconstruction.",
            "Review structure extraction and require the decoder to emit every recorded heading."),
    SECTION_ORDER_BROKEN("Recorded sections appear out of their documented order.",
            "Enforce the ordering rule in the decoder before any free generation."),
    MISSING_FACT("A recorded fact statement is absent from the reconstruction.",
            "Raise fact-extraction coverage or require the decoder to emit every recorded fact."),
    MISSING_CRITICAL_FACT("A fact marked critical did not survive the reconstruction.",
            "Treat this as a correctness failure: never ship a reconstruction that loses a critical fact."),
    MISSING_ENTITY("A recorded entity name is absent from the reconstruction.",
            "Raise entity-extraction coverage or require explicit entity emission."),
    ENTITY_SUBSTITUTION("An entity appears with a different surface form than recorded.",
            "Reproduce entity names exactly as recorded; never substitute paraphrases."),
    MISSING_RELATIONSHIP("A recorded relationship is not expressible in the reconstruction.",
            "Require relationship material to survive decoding, keeping subject and object present."),
    RELATIONSHIP_DIRECTION_UNCLEAR("A relationship lost its recorded direction.",
            "Emit relationships with subject and object in the recorded order."),
    UNSUPPORTED_CONTENT("The reconstruction contains content unsupported by the semantic material.",
            "Constrain the decoder to the encoded vocabulary; investigate unsupported output."),
    PROTECTED_VALUE_WITHHELD("A protected value was withheld, so exact reproduction is impossible.",
            "Expected and correct: protected values stay withheld until authorization exists.");

    private final String description;
    private final String advice;

    ErrorCategory(String description, String advice) {
        this.description = description;
        this.advice = advice;
    }

    public String description() {
        return description;
    }

    public String advice() {
        return advice;
    }

    public boolean correctness() {
        return this == MISSING_CRITICAL_FACT || this == ENTITY_SUBSTITUTION
                || this == UNSUPPORTED_CONTENT;
    }
}
