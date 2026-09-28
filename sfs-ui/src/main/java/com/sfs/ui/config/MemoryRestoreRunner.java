package com.sfs.ui.config;

import com.sfs.lifecycle.core.FileLifecycleManager;
import com.sfs.lifecycle.core.LifecyclePersistence;
import com.sfs.memory.MemoryDnaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(0)
public class MemoryRestoreRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MemoryRestoreRunner.class);

    private final FileLifecycleManager fileLifecycleManager;
    private final LifecyclePersistence lifecyclePersistence;
    private final MemoryDnaRepository dnaRepository;

    public MemoryRestoreRunner(FileLifecycleManager fileLifecycleManager,
                               LifecyclePersistence lifecyclePersistence,
                               MemoryDnaRepository dnaRepository) {
        this.fileLifecycleManager = fileLifecycleManager;
        this.lifecyclePersistence = lifecyclePersistence;
        this.dnaRepository = dnaRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        var files = lifecyclePersistence.loadFiles();
        var events = lifecyclePersistence.loadEvents();
        if (!files.isEmpty()) {
            fileLifecycleManager.restore(files, events);
        }
        dnaRepository.rebuildVectorIndex();
        log.info("Memory DB restored: {} object(s), {} lifecycle event(s), {} vector entr(ies).",
                files.size(), events.size(), dnaRepository.vectorIndexSize());
    }
}
