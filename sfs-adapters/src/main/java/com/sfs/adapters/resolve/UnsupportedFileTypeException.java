package com.sfs.adapters.resolve;

public final class UnsupportedFileTypeException extends RuntimeException {

    private final String fileName;
    private final String contentType;

    public UnsupportedFileTypeException(String fileName, String contentType) {
        super(message(fileName, contentType));
        this.fileName = fileName;
        this.contentType = contentType;
    }

    private static String message(String fileName, String contentType) {
        String describedType = contentType == null || contentType.isBlank()
                ? "no content type"
                : "content type '" + contentType + "'";
        return "No registered adapter supports '" + fileName + "' (" + describedType
                + "). The input is not treated as text.";
    }

    public String fileName() {
        return fileName;
    }

    public String contentType() {
        return contentType;
    }
}
