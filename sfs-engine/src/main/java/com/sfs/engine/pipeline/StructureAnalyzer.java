package com.sfs.engine.pipeline;

import com.sfs.contracts.semantic.SemanticDnaView;
import com.sfs.engine.core.SemanticContext;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class StructureAnalyzer implements Analyzer {

    private static final Pattern MARKDOWN_HEADING = Pattern.compile("^(#{1,6})\\s+(.+)$");
    private static final Pattern NUMBERED_HEADING = Pattern.compile(
            "^(\\d+(\\.\\d+){0,3})\\.?\\s+([A-Z].{2,78})$");

    @Override
    public String name() {
        return "structure";
    }

    @Override
    public void perform(SemanticContext context, SemanticIntermediateRepresentation ir) {
        List<SemanticDnaView.StructureNodeView> nodes = new ArrayList<>();
        int order = 0;
        for (String line : ir.rawLines()) {
            String stripped = line.strip();
            if (stripped.isEmpty()) {
                continue;
            }
            var markdown = MARKDOWN_HEADING.matcher(stripped);
            if (markdown.matches()) {
                nodes.add(new SemanticDnaView.StructureNodeView(
                        markdown.group(2).strip(), markdown.group(1).length(), order++));
                continue;
            }
            var numbered = NUMBERED_HEADING.matcher(stripped);
            if (numbered.matches() && !endsWithSentencePunctuation(numbered.group(3))) {
                int depth = (int) numbered.group(1).chars().filter(c -> c == '.').count() + 1;
                nodes.add(new SemanticDnaView.StructureNodeView(
                        numbered.group(3).strip(), Math.min(depth, 6), order++));
                continue;
            }
            if (isAllCapsHeading(stripped)) {
                nodes.add(new SemanticDnaView.StructureNodeView(
                        stripped, 1, order++));
            }
        }
        if (nodes.isEmpty()) {
            nodes.add(new SemanticDnaView.StructureNodeView("Body", 1, 0));
        }
        ir.setStructure(nodes);
    }

    private static boolean endsWithSentencePunctuation(String text) {
        return text.strip().endsWith(".")
                && !text.strip().endsWith("..");
    }

    private static boolean isAllCapsHeading(String line) {
        if (line.length() < 3 || line.length() > 60) {
            return false;
        }
        if (line.endsWith(".") || line.endsWith(",") || line.endsWith(";")) {
            return false;
        }
        long letters = line.chars().filter(Character::isLetter).count();
        long upper = line.chars().filter(c -> Character.isLetter(c)
                && Character.isUpperCase(c)).count();
        return letters >= 3 && upper == letters;
    }
}
