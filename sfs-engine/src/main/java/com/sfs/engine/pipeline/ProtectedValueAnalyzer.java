package com.sfs.engine.pipeline;

import com.sfs.contracts.semantic.ProtectedReferenceView;
import com.sfs.engine.core.SemanticContext;
import com.sfs.engine.core.Digests;

import java.util.ArrayList;
import java.util.List;

public final class ProtectedValueAnalyzer implements Analyzer {

    private final ProtectedValueDetector detector = new ProtectedValueDetector();

    @Override
    public String name() {
        return "protected-values";
    }

    @Override
    public void perform(SemanticContext context, SemanticIntermediateRepresentation ir) {
        List<ProtectedReferenceView> references = new ArrayList<>();
        List<String> lines = ir.rawLines();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (!detector.isSensitiveLine(line)) {
                continue;
            }
            references.add(new ProtectedReferenceView(
                    "sfs-ref-" + Digests.sha256Hex(line.strip()).substring(0, 12),
                    sensitivityOf(line),
                    detector.isCredentialAssignment(line)
                            ? "credential assignment"
                            : "contact address",
                    "line " + (i + 1),
                    true));
        }
        ir.setProtectedReferences(references);
    }

    private static ProtectedReferenceView.SensitiveType sensitivityOf(String line) {
        String lower = line.toLowerCase(java.util.Locale.ROOT);
        if (lower.contains("password") || lower.contains("passwd") || lower.contains("pwd")) {
            return ProtectedReferenceView.SensitiveType.PASSWORD;
        }
        if (lower.matches(".*(api[_-]?key|apikey).*")) {
            return ProtectedReferenceView.SensitiveType.API_KEY;
        }
        if (lower.contains("token")) {
            return ProtectedReferenceView.SensitiveType.ACCESS_TOKEN;
        }
        if (ProtectedReferenceView.SensitiveType.EMAIL_ADDRESS != null && line.contains("@")) {
            return ProtectedReferenceView.SensitiveType.EMAIL_ADDRESS;
        }
        return ProtectedReferenceView.SensitiveType.OTHER;
    }

}
