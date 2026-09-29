package com.sfs.reconstruction;

import com.sfs.core.dna.SemanticDna;
import com.sfs.core.rules.ReconstructionPlan;

import java.util.Objects;

public record ModelInput(String objectId, SemanticDna dna, ReconstructionPlan plan) {

    public ModelInput {
        Objects.requireNonNull(objectId, "objectId must not be null");
        Objects.requireNonNull(dna, "dna must not be null");
        Objects.requireNonNull(plan, "plan must not be null");
        if (objectId.isBlank()) {
            throw new IllegalArgumentException("objectId must not be blank");
        }
        if (!dna.objectId().equals(objectId) || !plan.objectId().equals(objectId)) {
            throw new IllegalArgumentException(
                    "object identity must agree across the input, the DNA and the plan");
        }
    }

    public static ModelInput of(SemanticDna dna, ReconstructionPlan plan) {
        return new ModelInput(dna.objectId(), dna, plan);
    }

    public String dnaSha256() {
        return plan.dnaSha256();
    }
}
