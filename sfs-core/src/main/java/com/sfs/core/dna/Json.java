package com.sfs.core.dna;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Json {

    private Json() {
    }

    public static String write(Object value) {
        StringBuilder out = new StringBuilder();
        writeValue(value, out);
        return out.toString();
    }

    public static Object parse(String json) {
        Parser parser = new Parser(json);
        Object value = parser.parseValue();
        parser.skipWhitespace();
        if (!parser.atEnd()) {
            throw new IllegalArgumentException("trailing content after JSON value");
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> object(Object value) {
        if (!(value instanceof Map)) {
            throw new IllegalArgumentException("expected a JSON object");
        }
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    public static List<Object> array(Object value) {
        if (!(value instanceof List)) {
            throw new IllegalArgumentException("expected a JSON array");
        }
        return (List<Object>) value;
    }

    public static String string(Object value) {
        if (!(value instanceof String s)) {
            throw new IllegalArgumentException("expected a JSON string");
        }
        return s;
    }

    public static double number(Object value) {
        if (!(value instanceof Double d)) {
            throw new IllegalArgumentException("expected a JSON number");
        }
        return d;
    }

    public static boolean bool(Object value) {
        if (!(value instanceof Boolean b)) {
            throw new IllegalArgumentException("expected a JSON boolean");
        }
        return b;
    }

    private static void writeValue(Object value, StringBuilder out) {
        if (value == null) {
            out.append("null");
            return;
        }
        if (value instanceof String s) {
            writeString(s, out);
            return;
        }
        if (value instanceof Double d) {
            if (!Double.isFinite(d)) {
                throw new IllegalArgumentException("JSON numbers must be finite");
            }
            out.append(d.doubleValue() == Math.floor(d) && !d.isInfinite()
                    && Math.abs(d) < 1e15
                    ? Long.toString(d.longValue())
                    : d.toString());
            return;
        }
        if (value instanceof Boolean b) {
            out.append(b ? "true" : "false");
            return;
        }
        if (value instanceof Map<?, ?> map) {
            out.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) {
                    out.append(',');
                }
                first = false;
                writeString(String.valueOf(entry.getKey()), out);
                out.append(':');
                writeValue(entry.getValue(), out);
            }
            out.append('}');
            return;
        }
        if (value instanceof List<?> list) {
            out.append('[');
            boolean first = true;
            for (Object item : list) {
                if (!first) {
                    out.append(',');
                }
                first = false;
                writeValue(item, out);
            }
            out.append(']');
            return;
        }
        throw new IllegalArgumentException(
                "unsupported JSON value type " + value.getClass().getName());
    }

    private static void writeString(String s, StringBuilder out) {
        out.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        out.append('"');
    }

    private static final class Parser {

        private final String text;
        private int position;

        private Parser(String text) {
            this.text = text;
        }

        private boolean atEnd() {
            return position >= text.length();
        }

        private void skipWhitespace() {
            while (!atEnd() && Character.isWhitespace(text.charAt(position))) {
                position++;
            }
        }

        private char peek() {
            if (atEnd()) {
                throw new IllegalArgumentException("unexpected end of JSON input");
            }
            return text.charAt(position);
        }

        private void expect(char c) {
            if (atEnd() || text.charAt(position) != c) {
                throw new IllegalArgumentException(
                        "expected '" + c + "' at position " + position);
            }
            position++;
        }

        private Object parseValue() {
            skipWhitespace();
            char c = peek();
            return switch (c) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't' -> parseKeyword("true", Boolean.TRUE);
                case 'f' -> parseKeyword("false", Boolean.FALSE);
                case 'n' -> parseKeyword("null", null);
                default -> parseNumber();
            };
        }

        private Object parseKeyword(String keyword, Object value) {
            if (!text.startsWith(keyword, position)) {
                throw new IllegalArgumentException(
                        "invalid JSON literal at position " + position);
            }
            position += keyword.length();
            return value;
        }

        private Map<String, Object> parseObject() {
            expect('{');
            Map<String, Object> map = new LinkedHashMap<>();
            skipWhitespace();
            if (peek() == '}') {
                position++;
                return map;
            }
            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                map.put(key, parseValue());
                skipWhitespace();
                char c = peek();
                if (c == ',') {
                    position++;
                    continue;
                }
                if (c == '}') {
                    position++;
                    return map;
                }
                throw new IllegalArgumentException(
                        "expected ',' or '}' at position " + position);
            }
        }

        private List<Object> parseArray() {
            expect('[');
            List<Object> list = new ArrayList<>();
            skipWhitespace();
            if (peek() == ']') {
                position++;
                return list;
            }
            while (true) {
                list.add(parseValue());
                skipWhitespace();
                char c = peek();
                if (c == ',') {
                    position++;
                    continue;
                }
                if (c == ']') {
                    position++;
                    return list;
                }
                throw new IllegalArgumentException(
                        "expected ',' or ']' at position " + position);
            }
        }

        private String parseString() {
            expect('"');
            StringBuilder out = new StringBuilder();
            while (true) {
                if (atEnd()) {
                    throw new IllegalArgumentException("unterminated JSON string");
                }
                char c = text.charAt(position++);
                if (c == '"') {
                    return out.toString();
                }
                if (c == '\\') {
                    char escape = text.charAt(position++);
                    switch (escape) {
                        case '"' -> out.append('"');
                        case '\\' -> out.append('\\');
                        case '/' -> out.append('/');
                        case 'b' -> out.append('\b');
                        case 'f' -> out.append('\f');
                        case 'n' -> out.append('\n');
                        case 'r' -> out.append('\r');
                        case 't' -> out.append('\t');
                        case 'u' -> {
                            if (position + 4 > text.length()) {
                                throw new IllegalArgumentException("invalid unicode escape");
                            }
                            String hex = text.substring(position, position + 4);
                            position += 4;
                            out.append((char) Integer.parseInt(hex, 16));
                        }
                        default -> throw new IllegalArgumentException(
                                "invalid escape '\\" + escape + "'");
                    }
                } else {
                    out.append(c);
                }
            }
        }

        private Double parseNumber() {
            int start = position;
            while (!atEnd()) {
                char c = text.charAt(position);
                if ((c >= '0' && c <= '9') || c == '-' || c == '+' || c == '.'
                        || c == 'e' || c == 'E') {
                    position++;
                } else {
                    break;
                }
            }
            try {
                return Double.parseDouble(text.substring(start, position));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "invalid JSON number at position " + start);
            }
        }
    }
}
