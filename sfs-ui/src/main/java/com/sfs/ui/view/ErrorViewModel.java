package com.sfs.ui.view;

import java.util.Objects;

public record ErrorViewModel(
        int status,
        String title,
        String summary,
        String guidance,
        String requestedPath) {

    private static final int MAX_PATH_LENGTH = 200;

    public ErrorViewModel {
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(summary, "summary must not be null");
        Objects.requireNonNull(guidance, "guidance must not be null");

        if (status < 400 || status > 599) {
            throw new IllegalArgumentException("status must be an HTTP error status, got " + status);
        }
        if (title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        if (summary.isBlank()) {
            throw new IllegalArgumentException("summary must not be blank");
        }
        if (guidance.isBlank()) {
            throw new IllegalArgumentException("guidance must not be blank");
        }
    }

    public static ErrorViewModel forStatus(int status, String requestedPath) {
        int resolved = (status < 400 || status > 599) ? 500 : status;
        String path = truncate(requestedPath);

        return switch (resolved) {
            case 400 -> new ErrorViewModel(
                    resolved,
                    "Request not understood",
                    "The request could not be read, so nothing was changed.",
                    "Check the address and try again from a page below.",
                    path);
            case 403 -> new ErrorViewModel(
                    resolved,
                    "Not permitted",
                    "This action is not permitted, so it was refused and nothing was changed.",
                    "Protected operations require authorization that arrives with Milestone 13.",
                    path);
            case 404 -> new ErrorViewModel(
                    resolved,
                    "Page not found",
                    "There is nothing at this address. No file, semantic record or job was affected.",
                    "The address may be mistyped, or the item may never have existed.",
                    path);
            case 405 -> new ErrorViewModel(
                    resolved,
                    "Action not allowed here",
                    "That action is not available at this address, so it was refused.",
                    "Operations that change state must be started from their own explicit control.",
                    path);
            case 500 -> new ErrorViewModel(
                    resolved,
                    "Something went wrong",
                    "The request failed. Any operation it started may not have completed.",
                    "The failure has been logged. Check the application log for the recorded error.",
                    path);
            default -> new ErrorViewModel(
                    resolved,
                    resolved < 500 ? "Request refused" : "Server error",
                    resolved < 500
                            ? "The request was refused, so nothing was changed."
                            : "The request failed before it could complete.",
                    "Return to a page below and try again.",
                    path);
        };
    }

    public boolean hasRequestedPath() {
        return requestedPath != null && !requestedPath.isBlank();
    }

    public boolean isServerError() {
        return status >= 500;
    }

    private static String truncate(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        String trimmed = path.strip();
        return trimmed.length() <= MAX_PATH_LENGTH
                ? trimmed
                : trimmed.substring(0, MAX_PATH_LENGTH) + "…";
    }
}
