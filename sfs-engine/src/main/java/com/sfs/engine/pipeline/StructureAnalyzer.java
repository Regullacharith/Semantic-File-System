package com.sfs.engine.pipeline;

import com.sfs.adapters.text.StructuralParser;
import com.sfs.engine.core.SemanticContext;

public final class StructureAnalyzer implements Analyzer {

    private final StructuralParser structuralParser = new StructuralParser();

    @Override
    public String name() {
        return "structure";
    }

    @Override
    public void perform(SemanticContext context, SemanticIntermediateRepresentation ir) {
        if (!ir.structure().isEmpty()) {
            return;
        }
        ir.setStructure(structuralParser.parse(ir.rawText()));
    }
}
