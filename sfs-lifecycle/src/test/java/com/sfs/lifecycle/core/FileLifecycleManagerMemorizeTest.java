package com.sfs.lifecycle.core;

import com.sfs.contracts.file.FileImportRequest;
import com.sfs.contracts.file.FileOperationResult;
import com.sfs.contracts.file.FileStatus;
import com.sfs.contracts.security.Capability;
import com.sfs.contracts.security.Principal;
import com.sfs.lifecycle.identity.ObjectIdService;
import com.sfs.lifecycle.model.LifecycleEventType;
import com.sfs.lifecycle.state.FileState;
import com.sfs.lifecycle.store.InMemoryRawContentStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FileLifecycleManager memorization")
class FileLifecycleManagerMemorizeTest {

    private static final Instant T0 = Instant.parse("2026-03-15T10:00:00Z");

    private FileLifecycleManager manager;
    private InMemoryRawContentStore rawContentStore;
    private Principal principal;

    @BeforeEach
    void setUp() {
        rawContentStore = new InMemoryRawContentStore();
        manager = new FileLifecycleManager(Clock.fixed(T0, ZoneOffset.UTC),
                rawContentStore, new ObjectIdService(), null);
        principal = new Principal("operator", "Operator", Set.of(Capability.MEMORIZE));
    }

    private String registerAndAnalyze(String content) {
        var result = manager.importFile(
                new FileImportRequest("notes.txt", content, "text/plain"));
        manager.requestAnalysis(result.objectId());
        manager.completeAnalysisSuccess(result.objectId(), "stub-dna/0.1");
        return result.objectId();
    }

    @Nested
    @DisplayName("successful memorization")
    class Success {

        @Test
        @DisplayName("moves an analyzed object to MEMORY_COMMITTED with raw bytes retained")
        void memorizesAnalyzedObject() {
            String objectId = registerAndAnalyze("content to memorize");

            FileOperationResult result = manager.memorize(objectId, principal);

            assertThat(result.successful()).isTrue();
            var file = manager.registeredFile(objectId).orElseThrow();
            assertThat(file.state()).isEqualTo(FileState.MEMORY_COMMITTED);
            assertThat(rawContentStore.contains(objectId)).isTrue();
            assertThat(manager.findByObjectId(objectId).orElseThrow().status())
                    .isEqualTo(FileStatus.MEMORY_COMMITTED);
        }

        @Test
        @DisplayName("records the full commit transaction in the audit log")
        void recordsTransactionEvents() {
            String objectId = registerAndAnalyze("content");

            manager.memorize(objectId, principal);

            var types = manager.auditLog().eventsFor(objectId).stream()
                    .map(com.sfs.lifecycle.model.LifecycleEvent::type)
                    .toList();
            assertThat(types).containsSubsequence(
                    LifecycleEventType.MEMORY_COMMIT_REQUESTED,
                    LifecycleEventType.DNA_VALIDATED,
                    LifecycleEventType.MEMORY_COMMITTED);
            assertThat(manager.auditLog().eventsFor(objectId))
                    .filteredOn(event -> event.type() == LifecycleEventType.MEMORY_COMMITTED)
                    .first()
                    .satisfies(event -> {
                        assertThat(event.principalId()).isEqualTo("operator");
                        assertThat(event.from()).isEqualTo(FileState.MEMORIZABLE);
                        assertThat(event.to()).isEqualTo(FileState.MEMORY_COMMITTED);
                        assertThat(event.durationMs()).isNotNull();
                    });
        }

        @Test
        @DisplayName("a memory-committed object can still be reversibly deleted and restored")
        void committedObjectDeletableAndRestorable() {
            String objectId = registerAndAnalyze("content");
            manager.memorize(objectId, principal);

            assertThat(manager.softDelete(objectId, principal).successful()).isTrue();
            assertThat(manager.registeredFile(objectId).orElseThrow().deletedFrom())
                    .isEqualTo(FileState.MEMORY_COMMITTED);

            assertThat(manager.undoDelete(objectId, principal).successful()).isTrue();
            assertThat(manager.registeredFile(objectId).orElseThrow().state())
                    .isEqualTo(FileState.MEMORY_COMMITTED);
        }
    }

    @Nested
    @DisplayName("refusals")
    class Refusals {

        @Test
        @DisplayName("memorization is refused for a registered object")
        void refusedFromRegistered() {
            var result = manager.importFile(
                    new FileImportRequest("notes.txt", "content", "text/plain"));

            assertThat(manager.memorize(result.objectId(), principal).successful()).isFalse();
            assertThat(manager.registeredFile(result.objectId()).orElseThrow().state())
                    .isEqualTo(FileState.REGISTERED);
            assertThat(manager.auditLog().eventsFor(result.objectId()))
                    .anySatisfy(event -> {
                        assertThat(event.type()).isEqualTo(LifecycleEventType.MEMORY_COMMIT_REFUSED);
                        assertThat(event.refused()).isTrue();
                    });
        }

        @Test
        @DisplayName("memorization is refused for an analyzing object")
        void refusedFromAnalyzing() {
            var result = manager.importFile(
                    new FileImportRequest("notes.txt", "content", "text/plain"));
            manager.requestAnalysis(result.objectId());

            assertThat(manager.memorize(result.objectId(), principal).successful()).isFalse();
            assertThat(manager.registeredFile(result.objectId()).orElseThrow().state())
                    .isEqualTo(FileState.ANALYZING);
        }

        @Test
        @DisplayName("repeated memorization is refused")
        void repeatedMemorizationRefused() {
            String objectId = registerAndAnalyze("content");
            assertThat(manager.memorize(objectId, principal).successful()).isTrue();

            assertThat(manager.memorize(objectId, principal).successful()).isFalse();
            assertThat(manager.auditLog().eventsFor(objectId))
                    .anySatisfy(event -> assertThat(event.type())
                            .isEqualTo(LifecycleEventType.MEMORY_COMMIT_REFUSED));
        }

        @Test
        @DisplayName("memorization is refused for a soft-deleted object")
        void refusedFromSoftDeleted() {
            String objectId = registerAndAnalyze("content");
            manager.softDelete(objectId, principal);

            assertThat(manager.memorize(objectId, principal).successful()).isFalse();
        }

        @Test
        @DisplayName("memorization is refused after purge")
        void refusedAfterPurge() {
            String objectId = registerAndAnalyze("content");
            manager.memorize(objectId, principal);
            manager.softDelete(objectId, principal);
            manager.purgeRawData(objectId, principal);

            assertThat(manager.memorize(objectId, principal).successful()).isFalse();
        }

        @Test
        @DisplayName("memorization of an unknown object fails explicitly")
        void unknownObjectFails() {
            assertThat(manager.memorize("sfs-obj-9999-00000000", principal).successful())
                    .isFalse();
        }
    }
}
