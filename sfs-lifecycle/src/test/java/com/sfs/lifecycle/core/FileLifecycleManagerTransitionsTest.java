package com.sfs.lifecycle.core;

import com.sfs.contracts.file.FileImportRequest;
import com.sfs.contracts.file.FileOperationResult;
import com.sfs.contracts.file.FileStatus;
import com.sfs.contracts.security.Capability;
import com.sfs.contracts.security.Principal;
import com.sfs.lifecycle.identity.ObjectIdService;
import com.sfs.lifecycle.model.LifecycleEvent;
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

@DisplayName("FileLifecycleManager state transitions")
class FileLifecycleManagerTransitionsTest {

    private static final Instant T0 = Instant.parse("2026-03-15T10:00:00Z");

    private FileLifecycleManager manager;
    private InMemoryRawContentStore rawContentStore;
    private Principal principal;

    @BeforeEach
    void setUp() {
        rawContentStore = new InMemoryRawContentStore();
        manager = new FileLifecycleManager(Clock.fixed(T0, ZoneOffset.UTC),
                rawContentStore, new ObjectIdService(), null);
        principal = new Principal("operator", "Operator", Set.of(Capability.values()));
    }

    private String registerAnalyzed() {
        FileOperationResult result = manager.importFile(
                new FileImportRequest("notes.txt", "analyzed content", "text/plain"));
        manager.requestAnalysis(result.objectId());
        manager.completeAnalysisSuccess(result.objectId(), "stub-dna/0.1");
        return result.objectId();
    }

    private String registerMemorizedAndDeleted() {
        String objectId = registerAnalyzed();
        manager.memorize(objectId, principal);
        manager.softDelete(objectId, principal);
        return objectId;
    }

    @Nested
    @DisplayName("analysis")
    class Analysis {

        @Test
        @DisplayName("success moves a registered object to ANALYZED and certifies DNA")
        void successProducesAnalyzedWithDna() {
            FileOperationResult result = manager.importFile(
                    new FileImportRequest("notes.txt", "content", "text/plain"));
            manager.requestAnalysis(result.objectId());
            manager.completeAnalysisSuccess(result.objectId(), "stub-dna/0.1");

            var file = manager.registeredFile(result.objectId()).orElseThrow();
            assertThat(file.state()).isEqualTo(FileState.ANALYZED);
            assertThat(file.certifiedDnaVersion()).isEqualTo("stub-dna/0.1");
            assertThat(manager.findByObjectId(result.objectId()).orElseThrow().status())
                    .isEqualTo(FileStatus.ANALYZED);
        }

        @Test
        @DisplayName("failure moves a registered object to FAILED with an explicit reason")
        void failureProducesFailedWithReason() {
            FileOperationResult result = manager.importFile(
                    new FileImportRequest("notes.txt", "content", "text/plain"));
            manager.requestAnalysis(result.objectId());
            manager.completeAnalysisFailure(result.objectId(), "text adapter refused the payload");

            var file = manager.registeredFile(result.objectId()).orElseThrow();
            assertThat(file.state()).isEqualTo(FileState.FAILED);
            assertThat(file.certifiedDnaVersion()).isNull();

            assertThat(manager.auditLog().eventsFor(result.objectId())).anySatisfy(event -> {
                assertThat(event.type()).isEqualTo(LifecycleEventType.ANALYSIS_FAILED);
                assertThat(event.reason()).isEqualTo("text adapter refused the payload");
            });
        }

        @Test
        @DisplayName("a failed analysis can be retried and then succeed")
        void failedAnalysisCanBeRetried() {
            FileOperationResult result = manager.importFile(
                    new FileImportRequest("notes.txt", "content", "text/plain"));
            manager.requestAnalysis(result.objectId());
            manager.completeAnalysisFailure(result.objectId(), "transient engine failure");
            FileOperationResult retry = manager.requestAnalysis(result.objectId());
            assertThat(retry.successful()).isTrue();
            manager.completeAnalysisSuccess(result.objectId(), "stub-dna/0.1");

            assertThat(manager.registeredFile(result.objectId()).orElseThrow().state())
                    .isEqualTo(FileState.ANALYZED);
        }

        @Test
        @DisplayName("analysis is refused for an already analyzed object")
        void analysisRefusedWhenAlreadyAnalyzed() {
            String objectId = registerAnalyzed();
            FileOperationResult second = manager.requestAnalysis(objectId);
            assertThat(second.successful()).isFalse();

            assertThat(manager.auditLog().eventsFor(objectId)).anySatisfy(event -> {
                assertThat(event.type()).isEqualTo(LifecycleEventType.ANALYSIS_REFUSED);
                assertThat(event.refused()).isTrue();
                assertThat(event.reason()).isNotBlank();
            });
        }

        @Test
        @DisplayName("analysis of an unknown object reports failure without recording an event")
        void analysisOfUnknownObjectFails() {
            assertThat(manager.requestAnalysis("sfs-obj-9999-00000000").successful()).isFalse();
        }
    }

    @Nested
    @DisplayName("deletion and restoration")
    class DeletionAndRestoration {

        @Test
        @DisplayName("delete then undo restores an analyzed object to ANALYZED")
        void undoRestoresToAnalyzed() {
            String objectId = registerAnalyzed();
            assertThat(manager.softDelete(objectId, principal).successful()).isTrue();
            assertThat(manager.registeredFile(objectId).orElseThrow().state())
                    .isEqualTo(FileState.SOFT_DELETED);
            assertThat(rawContentStore.contains(objectId)).isTrue();

            assertThat(manager.undoDelete(objectId, principal).successful()).isTrue();
            assertThat(manager.registeredFile(objectId).orElseThrow().state())
                    .isEqualTo(FileState.ANALYZED);
            assertThat(rawContentStore.contains(objectId)).isTrue();
        }

        @Test
        @DisplayName("deletion is refused from a registered object")
        void deletionRefusedFromRegistered() {
            FileOperationResult result = manager.importFile(
                    new FileImportRequest("notes.txt", "content", "text/plain"));
            FileOperationResult deletion = manager.softDelete(result.objectId(), principal);
            assertThat(deletion.successful()).isFalse();
            assertThat(manager.registeredFile(result.objectId()).orElseThrow().state())
                    .isEqualTo(FileState.REGISTERED);
            assertThat(manager.auditLog().eventsFor(result.objectId()))
                    .anySatisfy(event -> assertThat(event.type())
                            .isEqualTo(LifecycleEventType.SOFT_DELETE_REFUSED));
        }

        @Test
        @DisplayName("repeated deletion is refused with an explicit outcome")
        void repeatedDeletionRefused() {
            String objectId = registerAnalyzed();
            assertThat(manager.softDelete(objectId, principal).successful()).isTrue();
            FileOperationResult again = manager.softDelete(objectId, principal);
            assertThat(again.successful()).isFalse();
            assertThat(manager.auditLog().eventsFor(objectId))
                    .anySatisfy(event -> assertThat(event.type())
                            .isEqualTo(LifecycleEventType.SOFT_DELETE_REFUSED));
        }

        @Test
        @DisplayName("repeated undo is refused with an explicit outcome")
        void repeatedUndoRefused() {
            String objectId = registerAnalyzed();
            manager.softDelete(objectId, principal);
            assertThat(manager.undoDelete(objectId, principal).successful()).isTrue();
            assertThat(manager.undoDelete(objectId, principal).successful()).isFalse();
        }

        @Test
        @DisplayName("operations on unknown objects fail explicitly")
        void unknownObjectsFailExplicitly() {
            assertThat(manager.softDelete("sfs-obj-9999-00000000", principal).successful()).isFalse();
            assertThat(manager.undoDelete("sfs-obj-9999-00000000", principal).successful()).isFalse();
            assertThat(manager.purgeRawData("sfs-obj-9999-00000000", principal).successful()).isFalse();
        }
    }

    @Nested
    @DisplayName("purge")
    class Purge {

        @Test
        @DisplayName("purge releases raw bytes and leaves the record listed as MEMORIZED")
        void purgeReleasesRawBytes() {
            String objectId = registerMemorizedAndDeleted();

            FileOperationResult purge = manager.purgeRawData(objectId, principal);
            assertThat(purge.successful()).isTrue();
            assertThat(manager.registeredFile(objectId).orElseThrow().state())
                    .isEqualTo(FileState.MEMORIZED);
            assertThat(rawContentStore.contains(objectId)).isFalse();
            assertThat(manager.findByObjectId(objectId)).isPresent();
        }

        @Test
        @DisplayName("repeated purge is refused after the bytes are gone")
        void repeatedPurgeRefused() {
            String objectId = registerMemorizedAndDeleted();
            assertThat(manager.purgeRawData(objectId, principal).successful()).isTrue();
            assertThat(manager.purgeRawData(objectId, principal).successful()).isFalse();
        }

        @Test
        @DisplayName("purge is refused from a live object")
        void purgeRefusedFromLiveObject() {
            String objectId = registerAnalyzed();
            FileOperationResult purge = manager.purgeRawData(objectId, principal);
            assertThat(purge.successful()).isFalse();
            assertThat(rawContentStore.contains(objectId)).isTrue();
        }

        @Test
        @DisplayName("raw bytes are never released before the memory commit")
        void gateRefusesReleaseBeforeMemoryCommit() {
            String objectId = registerAnalyzed();
            manager.softDelete(objectId, principal);

            FileOperationResult purge = manager.purgeRawData(objectId, principal);

            assertThat(purge.successful()).isFalse();
            assertThat(purge.message()).contains("durably committed");
            assertThat(rawContentStore.contains(objectId)).isTrue();
            assertThat(manager.auditLog().eventsFor(objectId))
                    .anySatisfy(event -> {
                        assertThat(event.type()).isEqualTo(LifecycleEventType.PURGE_REFUSED);
                        assertThat(event.refused()).isTrue();
                        assertThat(event.reason()).contains("durably committed");
                    });
        }

        @Test
        @DisplayName("the gated path to release is undo, memorize, delete, purge")
        void gatedPathReachesRelease() {
            String objectId = registerAnalyzed();
            manager.softDelete(objectId, principal);
            assertThat(manager.purgeRawData(objectId, principal).successful()).isFalse();

            manager.undoDelete(objectId, principal);
            manager.memorize(objectId, principal);
            manager.softDelete(objectId, principal);

            assertThat(manager.purgeRawData(objectId, principal).successful()).isTrue();
            assertThat(rawContentStore.contains(objectId)).isFalse();
        }

        @Test
        @DisplayName("undo after purge is refused")
        void undoAfterPurgeRefused() {
            String objectId = registerMemorizedAndDeleted();
            manager.purgeRawData(objectId, principal);
            assertThat(manager.undoDelete(objectId, principal).successful()).isFalse();
        }
    }

    @Nested
    @DisplayName("audit trail")
    class AuditTrail {

        @Test
        @DisplayName("a full deletion walk is auditable end to end")
        void fullWalkIsAuditable() {
            String objectId = registerAnalyzed();
            manager.softDelete(objectId, principal);
            manager.undoDelete(objectId, principal);
            manager.memorize(objectId, principal);
            manager.softDelete(objectId, principal);
            manager.purgeRawData(objectId, principal);

            var types = manager.auditLog().eventsFor(objectId).stream()
                    .map(LifecycleEvent::type)
                    .toList();
            assertThat(types).containsExactly(
                    LifecycleEventType.REGISTRATION_RECORDED,
                    LifecycleEventType.ANALYSIS_STARTED,
                    LifecycleEventType.ANALYSIS_SUCCEEDED,
                    LifecycleEventType.SOFT_DELETED,
                    LifecycleEventType.UNDO_DELETED,
                    LifecycleEventType.MEMORY_COMMIT_REQUESTED,
                    LifecycleEventType.DNA_VALIDATED,
                    LifecycleEventType.MEMORY_COMMITTED,
                    LifecycleEventType.SOFT_DELETED,
                    LifecycleEventType.PURGE_REQUESTED,
                    LifecycleEventType.RAW_RELEASED);
        }

        @Test
        @DisplayName("deletion events carry the acting principal")
        void deletionEventsCarryPrincipal() {
            String objectId = registerAnalyzed();
            manager.softDelete(objectId, principal);

            var events = manager.auditLog().eventsFor(objectId);
            assertThat(events.getLast().principalId()).isEqualTo("operator");
            assertThat(events.getFirst().principalId()).isEqualTo(FileLifecycleManager.SYSTEM_PRINCIPAL);
        }
    }
}
