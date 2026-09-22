package com.sfs.engine.inspect;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class FileInspector {

    public static final class TextRefusedException extends RuntimeException {

        public TextRefusedException(String message) {
            super(message);
        }
    }

    private static final double MIN_PRINTABLE_RATIO = 0.9;

    public InspectionReport inspect(byte[] content) {
        Objects.requireNonNull(content, "content must not be null");
        if (content.length == 0) {
            throw new TextRefusedException("The file content is empty.");
        }
        for (byte b : content) {
            if (b == 0) {
                throw new TextRefusedException(
                        "The content contains binary data and is not text.");
            }
        }
        String text = strictDecode(content);
        int wordCount = countWords(text);
        if (wordCount == 0) {
            throw new TextRefusedException("The content carries no words to analyze.");
        }
        double printableRatio = printableRatio(content);
        if (printableRatio < MIN_PRINTABLE_RATIO) {
            throw new TextRefusedException(
                    "The content is not printable text (printable ratio "
                            + String.format(java.util.Locale.ROOT, "%.2f", printableRatio) + ").");
        }
        return new InspectionReport(
                content.length,
                countLines(text),
                wordCount,
                countParagraphs(text),
                printableRatio);
    }

    private static String strictDecode(byte[] content) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(java.nio.ByteBuffer.wrap(content))
                    .toString();
        } catch (CharacterCodingException e) {
            throw new TextRefusedException("The content is not valid UTF-8 text.");
        }
    }

    private static int countWords(String text) {
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
        String[] blocks = text.strip().split("\n\\s*\n");
        int count = 0;
        for (String block : blocks) {
            if (!block.isBlank()) {
                count++;
            }
        }
        return Math.max(count, 1);
    }

    private static double printableRatio(byte[] content) {
        int printable = 0;
        for (byte b : content) {
            char c = (char) (b & 0xFF);
            if (c == '\t' || c == '\n' || c == '\r' || (c >= 32 && c < 127) || c >= 160) {
                printable++;
            }
        }
        return (double) printable / content.length;
    }
}
