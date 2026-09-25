package com.sfs.engine.core;

import java.util.Optional;

public interface AnalysisInputProvider {

    Optional<AnalysisInput> input(String objectId);
}
