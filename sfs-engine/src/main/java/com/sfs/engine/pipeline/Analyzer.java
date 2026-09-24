package com.sfs.engine.pipeline;

import com.sfs.engine.core.SemanticContext;

public interface Analyzer {

    String name();

    void perform(SemanticContext context, SemanticIntermediateRepresentation representation);
}
