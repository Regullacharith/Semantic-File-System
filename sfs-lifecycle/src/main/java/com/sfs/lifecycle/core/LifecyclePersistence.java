package com.sfs.lifecycle.core;

import com.sfs.lifecycle.model.LifecycleEvent;
import com.sfs.lifecycle.model.SemanticFile;

import java.util.List;

public interface LifecyclePersistence {

    void persistFile(SemanticFile file);

    void persistEvent(LifecycleEvent event);

    List<SemanticFile> loadFiles();

    List<LifecycleEvent> loadEvents();
}
