package com.sfs.core.identity;

import java.util.Objects;
import java.util.regex.Pattern;

public final class ObjectId {

    public static final String FORMAT_DESCRIPTION =
            "sfs-obj-<4 digits>-<alphanumeric suffix>";

    private static final Pattern PATTERN =
            Pattern.compile("^sfs-obj-[0-9]{4}-[a-zA-Z0-9]{1,32}$");

    private static final int MAX_LENGTH = 64;

    private final String value;

    private ObjectId(String value) {
        this.value = value;
    }

    public static ObjectId of(String value) {
        Objects.requireNonNull(value, "objectId must not be null");

        if (containsControlCharacter(value)) {
            throw new InvalidObjectIdException(
                    "Object ID must not contain control characters.");
        }

        String trimmed = value.strip();

        if (trimmed.isEmpty()) {
            throw new InvalidObjectIdException("Object ID must not be blank.");
        }
        if (trimmed.length() > MAX_LENGTH) {
            throw new InvalidObjectIdException(
                    "Object ID must not exceed " + MAX_LENGTH + " characters.");
        }
        if (!PATTERN.matcher(trimmed).matches()) {
            throw new InvalidObjectIdException(
                    "Object ID must match the format " + FORMAT_DESCRIPTION + ".");
        }

        return new ObjectId(trimmed);
    }

    public static boolean isValid(String value) {
        if (value == null || containsControlCharacter(value)) {
            return false;
        }
        String trimmed = value.strip();
        return trimmed.length() <= MAX_LENGTH && PATTERN.matcher(trimmed).matches();
    }

    private static boolean containsControlCharacter(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\n' || c == '\r' || c == '\u0000' || (Character.isISOControl(c) && !Character.isWhitespace(c))) {
                return true;
            }
        }
        return false;
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof ObjectId objectId && value.equals(objectId.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }

    public static final class InvalidObjectIdException extends IllegalArgumentException {

        public InvalidObjectIdException(String message) {
            super(message);
        }
    }
}
