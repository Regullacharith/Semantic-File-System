package com.sfs.ui.mock;

import com.sfs.lifecycle.core.AnalysisDispatcher;
import com.sfs.lifecycle.core.FileLifecycleManager;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("mock")
public class StubAnalysisEngine implements AnalysisDispatcher {

    public static final String STUB_DNA_VERSION = "stub-dna/0.1";

    private final FileLifecycleManager fileLifecycleManager;

    public StubAnalysisEngine(FileLifecycleManager fileLifecycleManager) {
        this.fileLifecycleManager = fileLifecycleManager;
        fileLifecycleManager.bindAnalysisDispatcher(this);
    }

    @Override
    public void dispatch(String objectId) {
        fileLifecycleManager.completeAnalysisSuccess(objectId, STUB_DNA_VERSION);
    }
}
