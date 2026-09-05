package com.sfs.ui.config;

import com.sfs.lifecycle.core.FileLifecycleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class LifecycleRecoveryRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LifecycleRecoveryRunner.class);

    private final FileLifecycleManager fileLifecycleManager;

    public LifecycleRecoveryRunner(FileLifecycleManager fileLifecycleManager) {
        this.fileLifecycleManager = fileLifecycleManager;
    }

    @Override
    public void run(ApplicationArguments args) {
        int recovered = fileLifecycleManager.recoverInterruptedMemorizations();
        if (recovered > 0) {
            log.warn("Rolled back {} interrupted memorization(s) to ANALYZED.", recovered);
        }
    }
}
