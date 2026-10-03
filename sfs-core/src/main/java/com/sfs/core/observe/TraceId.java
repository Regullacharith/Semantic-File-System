package com.sfs.core.observe;

import java.security.SecureRandom;
import java.util.Objects;

public record TraceId(String value) {

    private static final SecureRandom RANDOM = new SecureRandom();

    public TraceId {
        Objects.requireNonNull(value, "value must not be null");
        if (!value.matches("[0-9a-f]{16}")) {
            throw new IllegalArgumentException(
                    "a trace id is 16 lowercase hexadecimal characters");
        }
    }

    public static TraceId generate() {
        byte[] bytes = new byte[8];
        RANDOM.nextBytes(bytes);
        StringBuilder builder = new StringBuilder(16);
        for (byte b : bytes) {
            builder.append(String.format("%02x", b));
        }
        return new TraceId(builder.toString());
    }

    public static boolean isValid(String candidate) {
        if (candidate == null) {
            return false;
        }
        try {
            new TraceId(candidate);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
