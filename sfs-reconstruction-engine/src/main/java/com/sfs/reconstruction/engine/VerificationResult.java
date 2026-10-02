package com.sfs.reconstruction.engine;

import com.sfs.reconstruction.ConstraintInterface;

import java.util.List;
import java.util.Objects;

public record VerificationResult(
        boolean satisfied,
        List<String> violations,
        List<String> warnings,
        int checkedCriticalFacts,
        int checkedEntities,
        int checkedRelationships) {

    public VerificationResult {
        Objects.requireNonNull(violations, "violations must not be null");
        Objects.requireNonNull(warnings, "warnings must not be null");
        violations = List.copyOf(violations);
        warnings = List.copyOf(warnings);
        if (checkedCriticalFacts < 0 || checkedEntities < 0
                || checkedRelationships < 0) {
            throw new IllegalArgumentException("checked counts must not be negative");
        }
    }

    public static VerificationResult from(ConstraintInterface.Result result,
                                          int checkedCriticalFacts,
                                          int checkedEntities,
                                          int checkedRelationships) {
        Objects.requireNonNull(result, "result must not be null");
        return new VerificationResult(result.satisfied(), result.violations(),
                result.warnings(), checkedCriticalFacts, checkedEntities,
                checkedRelationships);
    }
}
