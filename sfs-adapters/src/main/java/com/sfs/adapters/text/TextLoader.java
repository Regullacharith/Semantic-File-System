package com.sfs.adapters.text;

import com.sfs.adapters.spi.AdapterRefusedException;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class TextLoader {

    public static final double MIN_PRINTABLE_RATIO = 0.9;

    public String load(byte[] content) {
        Objects.requireNonNull(content, "content must not be null");
        if (content.length == 0) {
            throw new AdapterRefusedException("The file content is empty.");
        }
        for (byte b : content) {
            if (b == 0) {
                throw new AdapterRefusedException(
                        "The content contains binary data and is not text.");
            }
        }
        String text = strictDecode(content);
        if (TextMetricsCalculator.countWords(text) == 0) {
            throw new AdapterRefusedException("The content carries no words to analyze.");
        }
        double printableRatio = printableRatioOf(text);
        if (printableRatio < MIN_PRINTABLE_RATIO) {
            throw new AdapterRefusedException(
                    "The content is not printable text (printable ratio "
                            + String.format(java.util.Locale.ROOT, "%.2f", printableRatio) + ").");
        }
        return text;
    }

    private static String strictDecode(byte[] content) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(java.nio.ByteBuffer.wrap(content))
                    .toString();
        } catch (CharacterCodingException e) {
            throw new AdapterRefusedException("The content is not valid UTF-8 text.");
        }
    }

    private static double printableRatioOf(String text) {
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
