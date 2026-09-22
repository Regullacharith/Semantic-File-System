package com.sfs.engine.pipeline;

import com.sfs.contracts.semantic.ProtectedReferenceView;
import com.sfs.engine.core.SemanticContext;
import com.sfs.engine.core.Digests;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ProtectedValueAnalyzer implements Analyzer {

    private static final Pattern ASSIGNMENT = Pattern.compile(
            "(?i)\\b(password|passwd|pwd|secret|api[_-]?key|apikey|access[_-]?token|token|"
                    + "client[_-]?secret)\\b\\s*[:=]\\s*(\\S+)");
    private static final Pattern EMAIL = Pattern.compile(
            "[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");

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
            Matcher assignment = ASSIGNMENT.matcher(line);
            while (assignment.find()) {
                references.add(reference(line, i + 1, typeOf(assignment.group(1)),
                        "credential assignment"));
            }
            Matcher email = EMAIL.matcher(line);
            while (email.find()) {
                references.add(reference(line, i + 1,
                        ProtectedReferenceView.SensitiveType.EMAIL_ADDRESS, "contact address"));
            }
        }
        ir.setProtectedReferences(references);
    }

    private static ProtectedReferenceView.SensitiveType typeOf(String keyword) {
        String normalized = keyword.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("password") || normalized.equals("passwd") || normalized.equals("pwd")) {
            return ProtectedReferenceView.SensitiveType.PASSWORD;
        }
        if (normalized.contains("api") && normalized.contains("key")) {
            return ProtectedReferenceView.SensitiveType.API_KEY;
        }
        if (normalized.contains("token")) {
            return ProtectedReferenceView.SensitiveType.ACCESS_TOKEN;
        }
        return ProtectedReferenceView.SensitiveType.OTHER;
    }

    private static ProtectedReferenceView reference(String line, int lineNumber,
                                                    ProtectedReferenceView.SensitiveType type,
                                                    String role) {
        String referenceId = "sfs-ref-"
                + Digests.sha256Hex(line.strip()).substring(0, 12);
        return new ProtectedReferenceView(referenceId, type, role,
                "line " + lineNumber, true);
    }
}
