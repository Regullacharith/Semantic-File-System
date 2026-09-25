package com.sfs.adapters.text;

import com.sfs.adapters.spi.TextMetrics;

import java.nio.charset.StandardCharsets;

final class TextMetricsCalculator {

    private TextMetricsCalculator() {
    }

    static TextMetrics of(String text) {
        return new TextMetrics(
                text.getBytes(StandardCharsets.UTF_8).length,
                countLines(text),
                countWords(text),
                countParagraphs(text),
                printableRatio(text));
    }

    static int countWords(String text) {
        int count = 0;
        boolean inWord = false;
        for (int i = 0; i < text.length(); i++) {
            boolean letterOrDigit = Character.isLetterOrDigit(text.charAt(i));
            if (letterOrDigit && !inWord) {
                count++;
            }
            inWord = letterOrDigit;
        }
        return count;
    }

    private static int countLines(String text) {
        int lines = 1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                lines++;
            }
        }
        return lines;
    }

    private static int countParagraphs(String text) {
        String stripped = text.strip();
        if (stripped.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (String block : stripped.split("\n\\s*\n")) {
            if (!block.isBlank()) {
                count++;
            }
        }
        return Math.max(count, 1);
    }

    private static double printableRatio(String text) {
        if (text.isEmpty()) {
            return 1.0;
        }
        int printable = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\t' || c == '\n' || c == '\r' || (c >= 32 && c < 127) || c >= 160) {
                printable++;
            }
        }
        return (double) printable / text.length();
    }
}
