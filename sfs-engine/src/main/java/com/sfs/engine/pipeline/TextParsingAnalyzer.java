package com.sfs.engine.pipeline;

import com.sfs.engine.core.SemanticContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TextParsingAnalyzer implements Analyzer {

    @Override
    public String name() {
        return "text-parsing";
    }

    @Override
    public void perform(SemanticContext context, SemanticIntermediateRepresentation ir) {
        ir.setParagraphs(splitParagraphs(ir.rawText()));
        ir.setSentences(splitSentences(ir.paragraphs()));
        List<String> tokens = tokenize(ir.rawText());
        ir.setTokens(tokens);
        ir.setWordFrequency(countFrequencies(tokens));
        ir.setBigramFrequency(countBigrams(tokens));
    }

    private static List<String> splitParagraphs(String text) {
        List<String> paragraphs = new ArrayList<>();
        for (String block : text.strip().split("\n\\s*\n")) {
            if (!block.isBlank()) {
                paragraphs.add(block.strip());
            }
        }
        return paragraphs;
    }

    private static List<String> splitSentences(List<String> paragraphs) {
        List<String> sentences = new ArrayList<>();
        for (String paragraph : paragraphs) {
            for (String line : paragraph.split("\n")) {
                String stripped = stripHeadingMarkers(line).strip();
                if (stripped.isEmpty() || isHeadingLike(line.strip())) {
                    continue;
                }
                for (String sentence : stripped.split("(?<=[.!?])\\s+")) {
                    if (!sentence.isBlank()) {
                        sentences.add(sentence.strip());
                    }
                }
            }
        }
        return sentences;
    }

    static boolean isHeadingLike(String line) {
        if (line.startsWith("#")) {
            return true;
        }
        if (line.matches("\\d{1,2}(\\.\\d{1,2}){0,3}\\.?\\s+[A-Z].{2,78}")) {
            return true;
        }
        return isAllCapsLine(line);
    }

    private static boolean isAllCapsLine(String line) {
        long letters = line.chars().filter(Character::isLetter).count();
        long upper = line.chars()
                .filter(c -> Character.isLetter(c) && Character.isUpperCase(c))
                .count();
        return letters >= 3 && letters == upper && line.length() <= 60;
    }

    static String stripHeadingMarkers(String line) {
        return line.replaceFirst("^#{1,6}\\s+", "")
                .replaceFirst("^\\d+(\\.\\d+)*\\.?\\s+", "");
    }

    private static List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        for (String raw : text.toLowerCase(java.util.Locale.ROOT).split("[^\\p{L}\\p{Nd}']+" )) {
            if (raw.length() >= 2) {
                tokens.add(raw);
            }
        }
        return tokens;
    }

    private static Map<String, Long> countFrequencies(List<String> tokens) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String token : tokens) {
            counts.merge(token, 1L, Long::sum);
        }
        return counts;
    }

    private static Map<String, Long> countBigrams(List<String> tokens) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (int i = 0; i + 1 < tokens.size(); i++) {
            String first = tokens.get(i);
            String second = tokens.get(i + 1);
            if (StopWords.ALL.contains(first) || StopWords.ALL.contains(second)) {
                continue;
            }
            counts.merge(first + " " + second, 1L, Long::sum);
        }
        return counts;
    }
}
