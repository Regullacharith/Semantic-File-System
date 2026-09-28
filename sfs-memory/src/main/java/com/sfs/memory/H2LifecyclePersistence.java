package com.sfs.memory;

import com.sfs.lifecycle.core.LifecyclePersistence;
import com.sfs.lifecycle.model.LifecycleEvent;
import com.sfs.lifecycle.model.SemanticFile;

import java.util.List;
import java.util.Objects;

public final class H2LifecyclePersistence implements LifecyclePersistence {

    private final H2MemoryDatabase database;

    public H2LifecyclePersistence(H2MemoryDatabase database) {
        this.database = Objects.requireNonNull(database, "database must not be null");
    }

    @Override
    public void persistFile(SemanticFile file) {
        database.saveObjectState(file);
    }

    @Override
    public void persistEvent(LifecycleEvent event) {
        database.saveLifecycleEvent(event);
    }

    @Override
    public List<SemanticFile> loadFiles() {
        return database.loadObjectStates();
    }

    @Override
    public List<LifecycleEvent> loadEvents() {
        return database.loadLifecycleEvents();
    }
}
