package com.sfs.app.service;

import com.sfs.app.api.error.ApiErrorCode;

import java.util.regex.Pattern;

public final class JobId {

    public static final String FORMAT_DESCRIPTION = "job-<4 digits>";

    private static final Pattern PATTERN = Pattern.compile("^job-[0-9]{4}$");

    private static final int MAX_LENGTH = 32;

    private JobId() {
    }

    public static String validate(String jobId) {
        if (jobId == null) {
            throw new ApplicationException(ApiErrorCode.JOB_ID_INVALID,
                    "Job ID must not be null.");
        }

        for (int i = 0; i < jobId.length(); i++) {
            if (Character.isISOControl(jobId.charAt(i))) {
                throw new ApplicationException(ApiErrorCode.JOB_ID_INVALID,
                        "Job ID must not contain control characters.");
            }
        }

        String trimmed = jobId.strip();

        if (trimmed.length() > MAX_LENGTH || !PATTERN.matcher(trimmed).matches()) {
            throw new ApplicationException(ApiErrorCode.JOB_ID_INVALID,
                    "Job ID must match the format " + FORMAT_DESCRIPTION + ".");
        }

        return trimmed;
    }

    public static boolean isValid(String jobId) {
        try {
            validate(jobId);
            return true;
        } catch (ApplicationException e) {
            return false;
        }
    }
}
