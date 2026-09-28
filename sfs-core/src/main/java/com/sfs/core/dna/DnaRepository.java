package com.sfs.core.dna;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface DnaRepository {

    StoredDna save(SemanticDna dna, Instant at);

    Optional<StoredDna> find(String objectId);

    List<StoredDna> history(String objectId);

    int nextDnaVersion(String objectId);

    boolean remove(String objectId);

    int objectCount();
}
