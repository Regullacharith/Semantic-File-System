package com.sfs.contracts.semantic;

import java.util.Optional;

/**
 * Application-facing contract for inspecting semantic memory.
 */
public interface SemanticRecordService {

    Optional<SemanticDnaView> findSemanticDna(String objectId);
}
