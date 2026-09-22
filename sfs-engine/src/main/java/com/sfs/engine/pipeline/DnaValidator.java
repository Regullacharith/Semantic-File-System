package com.sfs.engine.pipeline;

import com.sfs.engine.core.SemanticContext;
import com.sfs.engine.core.Digests;

import java.util.ArrayList;
import java.util.List;

public final class DnaValidator {

    public static final int FULL_COMPLETENESS_WORD_THRESHOLD = 40;

    public List<String> validate(SemanticContext context,
                                 SemanticIntermediateRepresentation ir) {
        List<String> issues = new ArrayList<>();
        if (ir.summary() == null || ir.summary().isBlank()) {
            issues.add("summary is missing");
        }
        if (ir.embedding() == null || ir.embedding().length == 0) {
            issues.add("embedding is missing");
        }
        if (ir.structure().isEmpty()) {
            issues.add("structure is empty");
        }
        if (Digests.sha256Hex(context.content()).equals(context.contentSha256()) == false) {
            issues.add("content digest mismatch");
        }
        if (ir.tokens().size() >= FULL_COMPLETENESS_WORD_THRESHOLD) {
            if (ir.concepts().isEmpty()) {
                issues.add("no concepts were extracted");
            }
            if (ir.topics().isEmpty()) {
                issues.add("no topics were extracted");
            }
            if (ir.entities().isEmpty()) {
                issues.add("no entities were extracted");
            }
            if (ir.facts().isEmpty()) {
                issues.add("no facts were extracted");
            }
        }
        return issues;
    }
}
