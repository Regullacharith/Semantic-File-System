package com.sfs.engine.pipeline;

import com.sfs.engine.core.SemanticContext;

public final class SummaryAnalyzer implements Analyzer {

    private static final int MAX_SUMMARY_LENGTH = 240;
    private static final int MAX_SUMMARY_SENTENCES = 2;

    @Override
    public String name() {
        return "summary";
    }

    @Override
    public void perform(SemanticContext context, SemanticIntermediateRepresentation ir) {
        StringBuilder summary = new StringBuilder();
        int sentencesUsed = 0;
        for (String sentence : ir.sentences()) {
            String stripped = TextParsingAnalyzer.stripHeadingMarkers(sentence).strip();
            if (stripped.isEmpty()) {
                continue;
            }
            if (!summary.isEmpty()) {
                summary.append(' ');
            }
            summary.append(stripped);
            sentencesUsed++;
            if (sentencesUsed >= MAX_SUMMARY_SENTENCES
                    || summary.length() >= MAX_SUMMARY_LENGTH) {
                break;
            }
        }
        if (sentencesUsed == 0) {
            summary = new StringBuilder(summaryFromHeadings(ir));
        }
        ir.setSummary(cap(summary.toString().strip()));
    }

    private static String summaryFromHeadings(SemanticIntermediateRepresentation ir) {
        StringBuilder headings = new StringBuilder();
        int headingsUsed = 0;
        for (String line : ir.rawLines()) {
            String stripped = line.strip();
            if (stripped.isEmpty() || !TextParsingAnalyzer.isHeadingLike(stripped)) {
                continue;
            }
            String heading = TextParsingAnalyzer.stripHeadingMarkers(stripped).strip();
            if (heading.isEmpty()) {
                continue;
            }
            if (!headings.isEmpty()) {
                headings.append("; ");
            }
            headings.append(heading);
            headingsUsed++;
            if (headingsUsed >= MAX_SUMMARY_SENTENCES + 1
                    || headings.length() >= MAX_SUMMARY_LENGTH) {
                break;
            }
        }
        return headings.toString();
    }

    private static String cap(String summary) {
        if (summary.length() <= MAX_SUMMARY_LENGTH) {
            return summary;
        }
        return summary.substring(0, MAX_SUMMARY_LENGTH - 1).strip() + "…";
    }
}
