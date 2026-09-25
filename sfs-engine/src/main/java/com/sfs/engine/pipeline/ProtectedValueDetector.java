package com.sfs.engine.pipeline;

import java.util.regex.Pattern;

public final class ProtectedValueDetector {

    private static final Pattern ASSIGNMENT = Pattern.compile(
            "(?i)\\b(password|passwd|pwd|secret|api[_-]?key|apikey|access[_-]?token|token|"
                    + "client[_-]?secret)\\b\\s*[:=]\\s*(\\S+)");
    private static final Pattern EMAIL = Pattern.compile(
            "[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");

    public boolean isSensitiveLine(String line) {
        if (line == null) {
            return false;
        }
        return ASSIGNMENT.matcher(line).find() || EMAIL.matcher(line).find();
    }

    public boolean isCredentialAssignment(String line) {
        return line != null && ASSIGNMENT.matcher(line).find();
    }
}
