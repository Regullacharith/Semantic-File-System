package com.sfs.engine.pipeline;

import com.sfs.contracts.security.SecretSubmission;
import com.sfs.contracts.security.SecretVault;
import com.sfs.contracts.semantic.ProtectedReferenceView;
import com.sfs.engine.core.SemanticContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ProtectedValueAnalyzer implements Analyzer {

    private final com.sfs.security.SensitiveDataDetector detector =
            new com.sfs.security.SensitiveDataDetector();
    private final com.sfs.security.SecurityPolicyEngine policyEngine =
            com.sfs.security.SecurityPolicyEngine.v1();
    private final SecretVault vault;

    public ProtectedValueAnalyzer() {
        this(null);
    }

    public ProtectedValueAnalyzer(SecretVault vault) {
        this.vault = vault;
    }

    @Override
    public String name() {
        return "protected-values";
    }

    @Override
    public void perform(SemanticContext context, SemanticIntermediateRepresentation ir) {
        Map<String, ProtectedReferenceView> references = new LinkedHashMap<>();
        List<com.sfs.security.SensitiveValue> values =
                detector.detect(context.content());
        for (com.sfs.security.SensitiveValue value : values) {
            com.sfs.security.PolicyDecision decision = policyEngine.decide(value);
            if (references.containsKey(decision.referenceId())) {
                continue;
            }
            if (vault != null && decision.resolvable()
                    && decision.handling() == com.sfs.contracts.security.HandlingPolicy.ENCRYPT) {
                vault.store(new SecretSubmission(decision.referenceId(),
                        context.objectId(), value.type(), value.exactValue()));
            }
            references.put(decision.referenceId(), new ProtectedReferenceView(
                    decision.referenceId(),
                    value.type(),
                    value.semanticRole(),
                    value.location(),
                    decision.resolvable()));
        }
        ir.setProtectedReferences(List.copyOf(references.values()));
    }
}
