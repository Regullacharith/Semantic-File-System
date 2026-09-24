package com.sfs.engine.pipeline;

import com.sfs.contracts.semantic.SemanticDnaView;
import com.sfs.engine.core.SemanticContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

public final class FactAnalyzer implements Analyzer {

    private static final int MAX_FACTS = 8;
    private static final int MAX_STATEMENT_LENGTH = 220;
    private static final Pattern PERCENT = Pattern.compile("\\d+([.,]\\d+)?\\s*(percent|%)");
    private static final Pattern DATE = Pattern.compile(
            "(Q[1-4]\\s+\\d{4}|\\b(19|20)\\d{2}\\b|\\d{4}-\\d{2}-\\d{2})");
    private static final Pattern SIGNAL_WORDS = Pattern.compile(
            "(?i)(decreas|increas|reduc|improv|schedul|target|deliver|launch|release|complete|adopt|migrat|verify|precedes|hosts|feeds)");

    @Override
    public String name() {
        return "facts";
    }

    @Override
    public void perform(SemanticContext context, SemanticIntermediateRepresentation ir) {
        List<SemanticDnaView.FactView> facts = new ArrayList<>();
        for (String sentence : ir.sentences()) {
            int signals = signalsIn(sentence);
            if (signals == 0) {
                continue;
            }
            boolean critical = PERCENT.matcher(sentence).find()
                    || DATE.matcher(sentence).find();
            double confidence = Math.min(0.95, 0.72 + 0.05 * signals);
            facts.add(new SemanticDnaView.FactView(trim(sentence), critical, confidence));
        }
        facts.sort(Comparator.comparing(SemanticDnaView.FactView::critical).reversed()
                .thenComparing(fact -> -fact.confidence()));
        ir.setFacts(facts.size() > MAX_FACTS ? new ArrayList<>(facts.subList(0, MAX_FACTS)) : facts);
    }

    private int signalsIn(String sentence) {
        int signals = 0;
        if (PERCENT.matcher(sentence).find()) {
            signals++;
        }
        if (DATE.matcher(sentence).find()) {
            signals++;
        }
        if (SIGNAL_WORDS.matcher(sentence).find()) {
            signals++;
        }
        return signals;
    }

    private static String trim(String sentence) {
        String stripped = sentence.strip();
        if (stripped.length() <= MAX_STATEMENT_LENGTH) {
            return stripped;
        }
        return stripped.substring(0, MAX_STATEMENT_LENGTH - 1).strip() + "…";
    }
}
