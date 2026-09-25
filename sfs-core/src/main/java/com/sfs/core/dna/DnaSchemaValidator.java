package com.sfs.core.dna;

import java.util.ArrayList;
import java.util.List;

public final class DnaSchemaValidator {

    public static final String CURRENT_SCHEMA_VERSION = "sfs-dna/0.2";

    public List<String> validate(SemanticDna dna) {
        List<String> issues = new ArrayList<>();
        if (!CURRENT_SCHEMA_VERSION.equals(dna.schemaVersion())) {
            issues.add("schema version must be " + CURRENT_SCHEMA_VERSION
                    + " but was " + dna.schemaVersion());
        }
        if (dna.summary() == null || dna.summary().isBlank()) {
            issues.add("summary is required");
        }
        if (dna.embedding() == null) {
            issues.add("embedding reference is required");
        }
        if (dna.structure().isEmpty()) {
            issues.add("structure is required");
        }
        if (dna.fidelity() == null) {
            issues.add("fidelity profile is required");
        }
        if (dna.behaviour() == null) {
            issues.add("behaviour profile is required");
        }
        if (dna.security() == null) {
            issues.add("security profile is required");
        }
        boolean declaredProtected = dna.security() != null
                && dna.security().containsProtectedReferences();
        if (!dna.security().protectedReferences().isEmpty() && !declaredProtected) {
            issues.add("security profile flag is inconsistent with its references");
        }
        for (Fact fact : dna.facts()) {
            if (fact.confidence() < 0.0 || fact.confidence() > 1.0) {
                issues.add("fact confidence out of range");
                break;
            }
        }
        for (int i = 0; i < dna.structure().size(); i++) {
            if (dna.structure().get(i).order() != i) {
                issues.add("structure order must be contiguous from zero");
                break;
            }
        }
        return issues;
    }

    public boolean isValid(SemanticDna dna) {
        return validate(dna).isEmpty();
    }
}
