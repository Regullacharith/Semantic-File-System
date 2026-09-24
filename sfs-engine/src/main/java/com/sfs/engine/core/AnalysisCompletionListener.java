package com.sfs.engine.core;

public interface AnalysisCompletionListener {

    default void onAnalysisSuccess(String objectId, String dnaVersion, Long durationMs) {
    }

    default void onAnalysisReused(String objectId, String dnaVersion) {
    }

    default void onAnalysisFailure(String objectId, String reason) {
    }
}
