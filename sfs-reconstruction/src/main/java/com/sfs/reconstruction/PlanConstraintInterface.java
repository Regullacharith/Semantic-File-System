package com.sfs.reconstruction;

import com.sfs.core.rules.PlanChecker;
import com.sfs.core.rules.PlanCompliance;
import com.sfs.reconstruction.encode.EncodedFact;
import com.sfs.reconstruction.encode.SemanticDnaEncoder;
import com.sfs.reconstruction.encode.UnifiedRepresentation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public final class PlanConstraintInterface implements ConstraintInterface {

    private static final int MIN_TOKEN_LENGTH = 4;

    private final PlanChecker planChecker = new PlanChecker();
    private final SemanticDnaEncoder encoder = new SemanticDnaEncoder();

    @Override
    public Result judge(ModelInput input, String candidateDraft) {
        Objects.requireNonNull(input, "input must not be null");
        String draft = candidateDraft == null ? "" : candidateDraft;
        List<String> violations = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (draft.isBlank()) {
            violations.add("the draft is empty; nothing was reconstructed");
        }

        UnifiedRepresentation representation = encoder.encode(input);
        if (representation.protectedReferenceCount() > 0) {
            violations.add(representation.protectedReferenceCount()
                    + " protected value(s) are withheld from semantic memory; the draft "
                    + "cannot reproduce them and inventing substitutes is prohibited");
        }

        warnings.addAll(input.plan().warnings());
        PlanCompliance compliance = planChecker.check(input.plan(), draft);
        violations.addAll(compliance.violations());
        warnings.addAll(compliance.warnings());

        if (input.plan().validation().requireAllCriticalFacts()) {
            for (EncodedFact fact : representation.content().facts()) {
                if (fact.critical() && !containsNormalized(draft, fact.statement())) {
                    violations.add("critical fact missing from the draft: "
                            + fact.statement());
                }
            }
        }

        violations.addAll(unsupportedTokens(representation, draft));
        return new Result(violations, warnings);
    }

    private List<String> unsupportedTokens(UnifiedRepresentation representation,
                                           String draft) {
        Set<String> vocabulary = representation.vocabulary();
        Set<String> unknown = new TreeSet<>();
        for (String token : draft.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) {
            if (token.length() >= MIN_TOKEN_LENGTH && !vocabulary.contains(token)) {
                unknown.add(token);
            }
        }
        if (unknown.isEmpty()) {
            return List.of();
        }
        return List.of("the draft contains content unsupported by the semantic "
                + "representation: " + String.join(", ", unknown));
    }

    private boolean containsNormalized(String draft, String statement) {
        return PlanChecker.normalize(draft).contains(PlanChecker.normalize(statement));
    }
}
