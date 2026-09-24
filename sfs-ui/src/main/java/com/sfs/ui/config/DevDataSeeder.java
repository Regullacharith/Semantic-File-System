package com.sfs.ui.config;

import com.sfs.contracts.file.FileImportRequest;
import com.sfs.contracts.file.FileOperationResult;
import com.sfs.contracts.security.Capability;
import com.sfs.contracts.security.Principal;
import com.sfs.engine.core.AnalysisJob;
import com.sfs.engine.core.SemanticEngine;
import com.sfs.lifecycle.core.FileLifecycleManager;
import com.sfs.lifecycle.identity.ObjectIdService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Random;
import java.util.Set;

@Component
@Profile("mock")
@Order(3)
public class DevDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    private static final Principal SEED_PRINCIPAL = new Principal(
            "dev-seed", "Development seed", Set.of(Capability.values()));

    private static final long[] DEV_SEED_SUFFIXES = {
            0xa1b2c3d4L, 0xe5f6a7b8L, 0xc9d0e1f2L, 0xb3c4d5e6L
    };

    private final FileLifecycleManager fileLifecycleManager;
    private final SemanticEngine semanticEngine;

    public DevDataSeeder(FileLifecycleManager fileLifecycleManager,
                         SemanticEngine semanticEngine) {
        this.fileLifecycleManager = fileLifecycleManager;
        this.semanticEngine = semanticEngine;
    }

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

    @Override
    public void run(ApplicationArguments args) throws Exception {
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

        log.info("Development seed data registered: 4 sample objects with engine-produced DNA.");
    }

    private String register(String fileName, String content) {
        return fileLifecycleManager
                .importFile(new FileImportRequest(fileName, content, "text/plain"))
                .objectId();
    }

    private void analyze(String objectId) throws Exception {
        FileOperationResult result = fileLifecycleManager.requestAnalysis(objectId);
        if (!result.successful()) {
            throw new IllegalStateException("Seed analysis was refused: " + result.message());
        }
        for (int i = 0; i < 600; i++) {
            Optional<AnalysisJob> finished = semanticEngine.jobRegistry().all().stream()
                    .filter(job -> job.objectId().equals(objectId) && job.isTerminal())
                    .findFirst();
            if (finished.isPresent()) {
                if (finished.get().status() != AnalysisJob.Status.COMPLETED
                        && finished.get().status() != AnalysisJob.Status.REUSED) {
                    throw new IllegalStateException(
                            "Seed analysis failed: " + finished.get().failureReason());
                }
                return;
            }
            Thread.sleep(10);
        }
        throw new IllegalStateException("Seed analysis did not finish in time.");
    }

    private static String researchSummary() {
        return """
                # Overview

                This document summarizes the semantic file storage research for 2026.
                Semantic DNA is stored in the Memory Database after validation.
                The Vector Index supports semantic search across stored records.
                Knowledge preservation matters more than byte compression for storage.

                # Representation Design

                Semantic DNA carries the summary, concepts, entities and facts of a file.
                The Memory Database stores the semantic record for every object.
                Reconstruction uses the stored representation instead of the raw bytes.
                The Vector Index supports retrieval for the semantic search subsystem.
                Storage addresses remain supporting metadata for every object.

                # Fidelity Targets

                Fidelity is measured across semantic, structural and factual dimensions.
                The reconstruction target of 87 to 92 percent is experimental, not guaranteed.
                Embeddings support retrieval but do not replace explicit facts.
                The evaluation reports fidelity for every reconstruction attempt.
                """;
    }

    private static String archivedReport() {
        return """
                # Summary

                This report reviews the database platform for Q3 2026.
                Query latency decreased by 40 percent after indexing changes were deployed.

                # Measurements

                PostgreSQL hosts the production workload for the analytics platform.
                Nightly batch jobs feed the reporting tables.
                Index rebuilds were scheduled outside business hours.
                Query latency improved for every dashboard in the product.

                # Recommendations

                PostgreSQL provides the primary storage for the analytics platform.
                The raw file  was released after the memory commit.
                """;
    }

    private static String meetingNotes() {
        return """
                """;
    }

    private static String deploymentConfig() {
        return """
                # Deployment Configuration

                deployment.region = asia-india
                deployment.replicas = 3

                # Credentials

                password=password123
                api_key=sk-live-9f8e7d6c5b4a
                ops.contact=charithkumar369@gmail.com
                """;
    }
}
