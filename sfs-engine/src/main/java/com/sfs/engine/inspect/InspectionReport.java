package com.sfs.engine.inspect;

import java.util.Objects;

public record InspectionReport(
        int byteLength,
        int lineCount,
        int wordCount,
        int paragraphCount,
        double printableRatio) {

    public InspectionReport {
        Objects.requireNonNull(byteLength, "byteLength must not be null");
        if (byteLength < 0) {
            throw new IllegalArgumentException("byteLength must not be negative");
        }
        if (lineCount < 0 || wordCount < 0 || paragraphCount < 0) {
            throw new IllegalArgumentException("counts must not be negative");
        }
        if (printableRatio < 0.0 || printableRatio > 1.0) {
            throw new IllegalArgumentException("printableRatio must be between 0.0 and 1.0");
        }
    }
}
