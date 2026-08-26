package com.sfs.ui.api;

public final class ApiExceptions {

    private ApiExceptions() {
    }

    public static class PayloadRejected extends RuntimeException {
        public PayloadRejected(String message) {
            super(message);
        }
    }

    public static class PayloadTooLarge extends RuntimeException {
        public PayloadTooLarge(String message) {
            super(message);
        }
    }

    public static class UnsupportedPayload extends RuntimeException {
        public UnsupportedPayload(String message) {
            super(message);
        }
    }
}
