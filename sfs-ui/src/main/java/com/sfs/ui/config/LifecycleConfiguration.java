package com.sfs.ui.config;

import com.sfs.contracts.file.FileService;
import com.sfs.lifecycle.core.FileLifecycleManager;
import com.sfs.lifecycle.identity.ObjectIdService;
import com.sfs.ui.mock.DevDataSeeder;
import com.sfs.lifecycle.store.InMemoryRawContentStore;
import com.sfs.lifecycle.store.RawContentStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class LifecycleConfiguration {

    @Bean
    public Clock sfsClock() {
        return Clock.systemUTC();
    }

    @Bean
    public ObjectIdService objectIdService() {
        return DevDataSeeder.scriptedObjectIdService();
    }

    @Bean
    public RawContentStore rawContentStore() {
        return new InMemoryRawContentStore();
    }

    @Bean
    public FileLifecycleManager fileLifecycleManager(Clock sfsClock,
                                                     RawContentStore rawContentStore,
                                                     ObjectIdService objectIdService) {
        return new FileLifecycleManager(sfsClock, rawContentStore, objectIdService);
    }

    @Bean
    public FileService fileService(FileLifecycleManager fileLifecycleManager) {
        return fileLifecycleManager;
    }
}
