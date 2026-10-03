package com.sfs.evaluation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Map;

public final class ImprovementAdvisor {

    public List<ImprovementSuggestion> advise(List<FidelityReport> reports) {
        Objects.requireNonNull(reports, "reports must not be null");
        Map<ErrorCategory, Long> occurrences = new LinkedHashMap<>();
        for (FidelityReport report : reports) {
            for (ErrorCategory error : report.errors()) {
                occurrences.merge(error, 1L, Long::sum);
            }
        }
        List<ImprovementSuggestion> suggestions = new ArrayList<>();
        for (Map.Entry<ErrorCategory, Long> entry : occurrences.entrySet()) {
            suggestions.add(new ImprovementSuggestion(entry.getKey(),
                    entry.getValue(), entry.getKey().advice(),
                    entry.getKey().correctness()));
        }
        suggestions.sort(Comparator
                .comparing(ImprovementSuggestion::correctness).reversed()
                .thenComparing(ImprovementSuggestion::occurrences,
                        Comparator.reverseOrder())
                .thenComparing(suggestion -> suggestion.category().name()));
        return List.copyOf(suggestions);
    }
}
