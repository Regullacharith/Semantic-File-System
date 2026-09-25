package com.sfs.engine.record;

import com.sfs.contracts.semantic.SemanticDnaView;
import com.sfs.contracts.semantic.SemanticRecordService;
import com.sfs.core.dna.InMemoryDnaRepository;
import com.sfs.core.dna.SemanticDna;
import com.sfs.core.dna.StoredDna;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class InMemorySemanticRecordStore implements SemanticRecordService {

    private final InMemoryDnaRepository repository = new InMemoryDnaRepository();

    public StoredDna save(SemanticDna dna) {
        Objects.requireNonNull(dna, "dna must not be null");
        return repository.save(dna, Instant.now());
    }

    @Override
    public Optional<SemanticDnaView> findSemanticDna(String objectId) {
        if (objectId == null || objectId.isBlank()) {
            return Optional.empty();
        }
        return repository.find(objectId).map(stored -> DnaViewMapper.toView(stored.dna()));
    }

    public Optional<StoredDna> findStored(String objectId) {
        if (objectId == null || objectId.isBlank()) {
            return Optional.empty();
        }
        return repository.find(objectId);
    }

    public List<StoredDna> history(String objectId) {
        return repository.history(objectId);
    }

    public int nextDnaVersion(String objectId) {
        return repository.nextDnaVersion(objectId);
    }

    public boolean remove(String objectId) {
        return repository.remove(objectId);
    }

    public int size() {
        return repository.objectCount();
    }
}
