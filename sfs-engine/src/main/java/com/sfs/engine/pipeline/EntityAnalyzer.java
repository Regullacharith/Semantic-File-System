package com.sfs.engine.pipeline;

import com.sfs.contracts.semantic.SemanticDnaView;
import com.sfs.engine.core.SemanticContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class EntityAnalyzer implements Analyzer {

    private static final int MAX_ENTITIES = 8;
    private static final Pattern CAPITALIZED_SEQUENCE = Pattern.compile(
            "(?:[A-Z][\\w&.-]*|\\d+)(?:(?:\\s+|-)(?:[A-Z][\\w&.-]*|\\d+))*");
    private static final Pattern QUARTER = Pattern.compile("Q[1-4]\\s+\\d{4}");
    private static final Pattern YEAR = Pattern.compile("\\b(19|20)\\d{2}\\b");
    private static final Pattern MILESTONE = Pattern.compile("(?i)\\bmilestone\\b");

    @Override
    public String name() {
        return "entities";
    }

    @Override
    public void perform(SemanticContext context, SemanticIntermediateRepresentation ir) {
        Map<String, EntityCandidate> candidates = new LinkedHashMap<>();
        for (String sentence : ir.sentences()) {
            collect(sentence, candidates);
        }
        List<SemanticDnaView.EntityView> entities = candidates.values().stream()
                .filter(EntityAnalyzer::isGenuineCandidate)
                .sorted((a, b) -> Integer.compare(b.mentions(), a.mentions()))
                .limit(MAX_ENTITIES)
                .map(candidate -> new SemanticDnaView.EntityView(
                        candidate.name(), typeOf(candidate.name()), candidate.mentions()))
                .toList();
        ir.setEntities(entities);
    }

    private void collect(String sentence, Map<String, EntityCandidate> candidates) {
        Matcher matcher = CAPITALIZED_SEQUENCE.matcher(sentence);
        while (matcher.find()) {
            String name = clean(matcher.group());
            if (name.length() < 2) {
                continue;
            }
            boolean atSentenceStart = matcher.start() == 0;
            candidates.computeIfAbsent(name, EntityCandidate::new)
                    .record(atSentenceStart);
        }
    }

    private static boolean isGenuineCandidate(EntityCandidate candidate) {
        String name = candidate.name();
        if (name.matches("\\d+")) {
            return false;
        }
        if (name.contains(" ") || name.matches(".*\\d.*")) {
            return true;
        }
        if (candidate.midSentenceMentions() > 0) {
            return true;
        }
        return !StopWords.ALL.contains(name.toLowerCase(java.util.Locale.ROOT));
    }

    private static String clean(String raw) {
        return raw.replaceAll("[.,;:!?)\"]+$", "").strip();
    }

    static String typeOf(String name) {
        if (MILESTONE.matcher(name).find()) {
            return "Milestone";
        }
        if (QUARTER.matcher(name).find()) {
            return "Time period";
        }
        if (YEAR.matcher(name).find()) {
            return "Time period";
        }
        String withoutSpaces = name.replace(" ", "");
        long letters = withoutSpaces.chars().filter(Character::isLetter).count();
        long upper = withoutSpaces.chars()
                .filter(c -> Character.isLetter(c) && Character.isUpperCase(c)).count();
        if (letters >= 3 && upper == letters) {
            return "Acronym";
        }
        return "Named entity";
    }

    private static final class EntityCandidate {

        private final String name;
        private int mentions;
        private int midSentenceMentions;

        private EntityCandidate(String name) {
            this.name = name;
        }

        private String name() {
            return name;
        }

        private int mentions() {
            return mentions;
        }

        private int midSentenceMentions() {
            return midSentenceMentions;
        }

        private void record(boolean atSentenceStart) {
            mentions++;
            if (!atSentenceStart) {
                midSentenceMentions++;
            }
        }
    }
}
