package com.sfs.adapters.text;

import java.text.Normalizer;
import java.util.Objects;

public final class TextNormalizer {

    public String normalize(String text) {
        Objects.requireNonNull(text, "text must not be null");
        String withoutBom = text.startsWith("\uFEFF") ? text.substring(1) : text;
        String unified = withoutBom.replace("\r\n", "\n").replace('\r', '\n');
        boolean endsWithNewline = unified.endsWith("\n");
        String body = endsWithNewline ? unified.substring(0, unified.length() - 1) : unified;
        if (body.isEmpty()) {
            return "";
        }
        StringBuilder result = new StringBuilder(body.length());
        int blankRun = 0;
        for (String line : body.split("\n", -1)) {
            String trimmed = line.stripTrailing();
            if (trimmed.isBlank()) {
                blankRun++;
                if (blankRun <= 1) {
                    result.append('\n');
                }
            } else {
                blankRun = 0;
                result.append(trimmed).append('\n');
            }
        }
        String normalized = endsWithNewline
                ? result.toString()
                : result.substring(0, result.length() - 1);
        return Normalizer.normalize(normalized, Normalizer.Form.NFC);
    }
}
