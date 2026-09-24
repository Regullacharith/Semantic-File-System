package com.sfs.engine.pipeline;

import com.sfs.contracts.semantic.SemanticDnaView;
import com.sfs.engine.core.SemanticContext;

public final class DnaBuilderAnalyzer implements Analyzer {

    public static final String SCHEMA_VERSION = "sfs-dna/0.1";

    @Override
    public String name() {
        return "dna-builder";
    }

    @Override
    public void perform(SemanticContext context, SemanticIntermediateRepresentation ir) {
        double extractionConfidence = extractionConfidence(ir);
        double structuralCompleteness = structuralCompleteness(ir);
        SemanticDnaView dna = new SemanticDnaView(
                context.objectId(),
                SCHEMA_VERSION,
                context.dnaVersion(),
                ir.summary(),
                ir.concepts(),
                ir.topics(),
                ir.entities(),
                ir.facts(),
                ir.relationships(),
                ir.structure(),
                ir.protectedReferences(),
                ir.embedding().length,
                new SemanticDnaView.FidelityProfileView(
                        extractionConfidence, structuralCompleteness, context.engineVersion()));
        ir.setDnaDraft(dna);
    }

    static double extractionConfidence(SemanticIntermediateRepresentation ir) {
        double confidence = 0.55
                + 0.03 * Math.min(ir.facts().size(), 8)
                + 0.02 * Math.min(ir.entities().size(), 8);
        return Math.min(0.95, confidence);
    }

    static double structuralCompleteness(SemanticIntermediateRepresentation ir) {
        double completeness = 0.4 + 0.12 * Math.min(ir.structure().size(), 4);
        return Math.min(0.95, completeness);
    }
}
