package com.sfs.engine.pipeline;

import com.sfs.contracts.semantic.SemanticDnaView;
import com.sfs.engine.core.SemanticContext;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RelationshipAnalyzer implements Analyzer {

    private static final int MAX_RELATIONSHIPS = 6;
    private static final int MAX_OBJECT_WORDS = 6;
    private static final Pattern PATTERN = Pattern.compile(
            "^([A-Z][\\w&.-]*(?:\\s+[A-Z][\\w&.-]*){0,3})\\s+"
                    + "(is|are|was|were|has|have|hosts?|supports?|includes?|contains?|uses?|"
                    + "provides?|precedes|causes?|caused|covers?|stores?|feeds?|replaces?|feeds)\\s+"
                    + "(.+)$");

    @Override
    public String name() {
        return "relationships";
    }

    @Override
    public void perform(SemanticContext context, SemanticIntermediateRepresentation ir) {
        List<SemanticDnaView.RelationshipView> relationships = new ArrayList<>();
        for (String sentence : ir.sentences()) {
            Matcher matcher = PATTERN.matcher(sentence.strip());
            if (!matcher.matches()) {
                continue;
            }
            String subject = matcher.group(1).strip();
            String type = matcher.group(2).toLowerCase(java.util.Locale.ROOT);
            String object = objectOf(matcher.group(3));
            if (subject.isEmpty() || object.isEmpty()) {
                continue;
            }
            relationships.add(new SemanticDnaView.RelationshipView(subject, type, object));
            if (relationships.size() >= MAX_RELATIONSHIPS) {
                break;
            }
        }
        ir.setRelationships(relationships);
    }

    private static String objectOf(String remainder) {
        String stripped = remainder.strip().replaceAll("[.!?]+$", "");
        String[] words = stripped.split("\\s+");
        StringBuilder object = new StringBuilder();
        for (int i = 0; i < Math.min(words.length, MAX_OBJECT_WORDS); i++) {
            if (!object.isEmpty()) {
                object.append(' ');
            }
            object.append(words[i]);
        }
        return object.toString().strip();
    }
}
