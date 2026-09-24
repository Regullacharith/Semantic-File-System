package com.sfs.engine.core;

public interface AnalysisJobListener {

    void onTransition(AnalysisJob previous, AnalysisJob current);
}
