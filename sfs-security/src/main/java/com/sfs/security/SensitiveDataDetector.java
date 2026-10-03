package com.sfs.security;

import com.sfs.contracts.semantic.ProtectedReferenceView.SensitiveType;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SensitiveDataDetector {

    private static final Pattern ASSIGNMENT = Pattern.compile(
            "(?i)\\b(password|passwd|pwd|secret|api[_-]?key|apikey|access[_-]?token|"
                    + "token|client[_-]?secret)\\b\\s*[:=]\\s*(\\S+)");
    private static final Pattern EMAIL = Pattern.compile(
            "[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern KEY_SHAPED = Pattern.compile(
            "\\b(?:sk|pk)[-_][A-Za-z0-9_-]{16,}\\b");
    private static final Pattern BEARER = Pattern.compile(
            "(?i)\\bbearer\\s+([A-Za-z0-9._-]{16,})\\b");
    private static final Pattern PHONE = Pattern.compile(
            "(?<!\\d)(\\+?\\d[\\d\\s().-]{8,18}\\d)(?!\\d)");
    private static final Pattern ACCOUNT = Pattern.compile(
            "(?i)\\b(account|acct)\\s*(?:number|no\\.?|id|#)\\s*[:=]?\\s*([A-Za-z0-9][A-Za-z0-9-]{5,})\\b");
    private static final Pattern ADDRESS = Pattern.compile(
            "(?i)\\b(address)\\s*[:=]\\s*(\\S.+)$");

    public List<SensitiveValue> detect(String text) {
        List<SensitiveValue> values = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String[] lines = text.split("\\r?\\n");
        for (int i = 0; i < lines.length; i++) {
            values.addAll(detectLine(lines[i], "line " + (i + 1)));
        }
        return List.copyOf(values);
    }

    public List<SensitiveValue> detectLine(String line, String location) {
        List<SensitiveValue> values = new ArrayList<>();
        if (line == null || line.isBlank()) {
            return values;
        }
        addAssignment(values, line, location);
        java.util.Set<String> assigned = values.stream()
                .map(SensitiveValue::exactValue)
                .collect(java.util.stream.Collectors.toSet());
        addMatch(values, assigned, line, location, KEY_SHAPED, SensitiveType.API_KEY,
                "key-shaped value");
        addMatch(values, assigned, line, location, BEARER, SensitiveType.ACCESS_TOKEN,
                "bearer token");
        addMatch(values, assigned, line, location, EMAIL, SensitiveType.EMAIL_ADDRESS,
                "contact address");
        addPhone(values, line, location);
        addMatch(values, assigned, line, location, ACCOUNT,
                SensitiveType.ACCOUNT_IDENTIFIER, "account identifier");
        addAddress(values, line, location);
        return values;
    }

    public boolean isSensitiveLine(String line) {
        return line != null && !line.isBlank() && !detectLine(line, "line").isEmpty();
    }

    public boolean isCredentialAssignment(String line) {
        return line != null && ASSIGNMENT.matcher(line).find();
    }

    private void addAssignment(List<SensitiveValue> into, String line,
                               String location) {
        Matcher matcher = ASSIGNMENT.matcher(line);
        while (matcher.find()) {
            into.add(new SensitiveValue(
                    assignmentType(matcher.group(1)), matcher.group(2),
                    "credential assignment", location, true));
        }
    }

    private void addMatch(List<SensitiveValue> into, java.util.Set<String> assigned,
                          String line, String location, Pattern pattern,
                          SensitiveType type, String role) {
        Matcher matcher = pattern.matcher(line);
        while (matcher.find()) {
            String value = matcher.groupCount() >= 1
                    ? matcher.group(matcher.groupCount())
                    : matcher.group();
            if (assigned.contains(value)) {
                continue;
            }
            into.add(new SensitiveValue(type, value, role, location, false));
            assigned.add(value);
        }
    }

    private void addPhone(List<SensitiveValue> into, String line, String location) {
        Matcher matcher = PHONE.matcher(line);
        while (matcher.find()) {
            String candidate = matcher.group(1);
            long digits = candidate.chars().filter(Character::isDigit).count();
            boolean yearShaped = candidate.matches("(?s)(19|20)\\d{2}[\\s().-].*");
            if (digits >= 9 && digits <= 15 && !yearShaped) {
                into.add(new SensitiveValue(SensitiveType.PHONE_NUMBER, candidate,
                        "contact number", location, false));
            }
        }
    }

    private void addAddress(List<SensitiveValue> into, String line, String location) {
        Matcher matcher = ADDRESS.matcher(line);
        while (matcher.find()) {
            into.add(new SensitiveValue(SensitiveType.POSTAL_ADDRESS,
                    matcher.group(2).strip(), "postal address", location, false));
        }
    }

    private SensitiveType assignmentType(String key) {
        String lower = key.toLowerCase(java.util.Locale.ROOT);
        if (lower.contains("passw") || lower.equals("pwd")) {
            return SensitiveType.PASSWORD;
        }
        if (lower.contains("api") && lower.contains("key")) {
            return SensitiveType.API_KEY;
        }
        if (lower.contains("token")) {
            return SensitiveType.ACCESS_TOKEN;
        }
        return SensitiveType.OTHER;
    }
}
