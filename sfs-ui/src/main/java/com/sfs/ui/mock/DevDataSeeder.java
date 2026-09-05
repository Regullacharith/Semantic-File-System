package com.sfs.ui.mock;

import com.sfs.contracts.file.FileImportRequest;
import com.sfs.contracts.security.Capability;
import com.sfs.contracts.security.Principal;
import com.sfs.lifecycle.core.FileLifecycleManager;
import com.sfs.lifecycle.identity.ObjectIdService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.Set;

@Component
@Profile("mock")
public class DevDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    private static final Principal SEED_PRINCIPAL = new Principal(
            "dev-seed", "Development seed", Set.of(Capability.values()));

    private static final long[] DEV_SEED_SUFFIXES = {
            0xa1b2c3d4L, 0xe5f6a7b8L, 0xc9d0e1f2L, 0xb3c4d5e6L
    };

    public static ObjectIdService scriptedObjectIdService() {
        return new ObjectIdService(new Random() {
            private int seedIndex;

            @Override
            public long nextLong() {
                if (seedIndex < DEV_SEED_SUFFIXES.length) {
                    return DEV_SEED_SUFFIXES[seedIndex++];
                }
                return super.nextLong();
            }
        });
    }

    private final FileLifecycleManager fileLifecycleManager;

    public DevDataSeeder(FileLifecycleManager fileLifecycleManager) {
        this.fileLifecycleManager = fileLifecycleManager;
    }

    @Override
    public void run(ApplicationArguments args) {
        String research = register("research-summary.txt", researchSummary());
        analyze(research);

        String archived = register("archived-report.txt", archivedReport());
        analyze(archived);
        fileLifecycleManager.memorize(archived, SEED_PRINCIPAL);
        fileLifecycleManager.softDelete(archived, SEED_PRINCIPAL);
        fileLifecycleManager.purgeRawData(archived, SEED_PRINCIPAL);

        register("meeting-notes.txt", meetingNotes());

        String deployment = register("deployment-config.txt", deploymentConfig());
        analyze(deployment);

        log.info("Development seed data registered: 4 sample objects.");
    }

    private String register(String fileName, String content) {
        return fileLifecycleManager
                .importFile(new FileImportRequest(fileName, content, "text/plain"))
                .objectId();
    }

    private void analyze(String objectId) {
        fileLifecycleManager.requestAnalysis(objectId);
    }

    private static String repeat(String line, int times) {
        return line.repeat(times);
    }

    private static String researchSummary() {
        return repeat("Research summary seed content for the SFS development environment.\n", 220);
    }

    private static String archivedReport() {
        return repeat("Archived report seed content; raw bytes are released after memorization.\n", 130);
    }

    private static String meetingNotes() {
        return repeat("Meeting notes seed content; this sample has not been analyzed yet.\n", 40);
    }

    private static String deploymentConfig() {
        return repeat("Deployment configuration seed content for interface demonstration.\n", 70);
    }
}
