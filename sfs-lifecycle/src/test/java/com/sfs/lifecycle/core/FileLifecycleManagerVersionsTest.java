package com.sfs.lifecycle.core;

import com.sfs.contracts.file.FileImportRequest;
import com.sfs.contracts.security.Capability;
import com.sfs.contracts.security.Principal;
import com.sfs.lifecycle.identity.ObjectIdService;
import com.sfs.lifecycle.model.ContentDigest;
import com.sfs.lifecycle.model.FileVersion;
import com.sfs.lifecycle.model.LifecycleEventType;
import com.sfs.lifecycle.state.FileState;
import com.sfs.lifecycle.store.InMemoryRawContentStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FileLifecycleManager version tracking")
class FileLifecycleManagerVersionsTest {

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

    private String registerAnalyzed(String content) {
        var result = manager.importFile(
                new FileImportRequest("notes.txt", content, "text/plain"));
        manager.requestAnalysis(result.objectId());
        manager.completeAnalysisSuccess(result.objectId(), "stub-dna/0.1");
        return result.objectId();
    }

    @Nested
    @DisplayName("rename keeps identity stable")
    class RenameStability {

        @Test
        @DisplayName("renaming updates the display name, not the Object ID or versions")
        void renameKeepsObjectIdAndVersions() {
            String objectId = registerAnalyzed("original content");

            var rename = manager.renameObject(objectId, "renamed.txt", principal);

            assertThat(rename.successful()).isTrue();
            var summary = manager.findByObjectId(objectId).orElseThrow();
            assertThat(summary.displayName()).isEqualTo("renamed.txt");
            assertThat(manager.versionHistory(objectId)).hasSize(1);
            assertThat(manager.registeredFile(objectId).orElseThrow().state())
                    .isEqualTo(FileState.ANALYZED);
        }

        @Test
        @DisplayName("a path-like display name is refused and audited")
        void pathLikeNameRefused() {
            String objectId = registerAnalyzed("content");

            var rename = manager.renameObject(objectId, "../escape.txt", principal);

            assertThat(rename.successful()).isFalse();
            assertThat(manager.findByObjectId(objectId).orElseThrow().displayName())
                    .isEqualTo("notes.txt");
            assertThat(manager.auditLog().eventsFor(objectId))
                    .anySatisfy(event -> {
                        assertThat(event.type()).isEqualTo(LifecycleEventType.METADATA_UPDATE_REFUSED);
                        assertThat(event.refused()).isTrue();
                    });
        }

        @Test
        @DisplayName("renaming is refused for a soft-deleted object")
        void renameRefusedWhileSoftDeleted() {
            String objectId = registerAnalyzed("content");
            manager.softDelete(objectId, principal);

            assertThat(manager.renameObject(objectId, "renamed.txt", principal).successful())
                    .isFalse();
        }

        @Test
        @DisplayName("renaming an unknown object fails explicitly")
        void renameUnknownFails() {
            assertThat(manager.renameObject("sfs-obj-9999-00000000", "x.txt", principal)
                    .successful()).isFalse();
        }
    }

    @Nested
    @DisplayName("content versions")
    class ContentVersions {

        @Test
        @DisplayName("replacing content records the next version and keeps the Object ID")
        void contentChangeRecordsNextVersion() {
            String objectId = registerAnalyzed("version one");
            var updated = manager.recordContentVersion(objectId, "version two", principal);

            assertThat(updated.successful()).isTrue();
            var history = manager.versionHistory(objectId);
            assertThat(history).hasSize(2);
            assertThat(history.get(0).number()).isEqualTo(1);
            assertThat(history.get(1).number()).isEqualTo(2);
            assertThat(history.get(1).contentSha256()).isEqualTo(ContentDigest.sha256Hex("version two"));
            assertThat(new String(rawContentStore.retrieve(objectId).orElseThrow(),
                    StandardCharsets.UTF_8)).isEqualTo("version two");
            assertThat(manager.registeredFile(objectId).orElseThrow().state())
                    .isEqualTo(FileState.ANALYZED);
        }

        @Test
        @DisplayName("identical content is refused without recording a version")
        void identicalContentRefused() {
            String objectId = registerAnalyzed("same content");

            var result = manager.recordContentVersion(objectId, "same content", principal);

            assertThat(result.successful()).isFalse();
            assertThat(manager.versionHistory(objectId)).hasSize(1);
            assertThat(manager.auditLog().eventsFor(objectId))
                    .anySatisfy(event -> assertThat(event.type())
                            .isEqualTo(LifecycleEventType.VERSION_ADD_REFUSED));
        }

        @Test
        @DisplayName("content replacement is refused for a memorized object")
        void contentChangeRefusedWhenMemorized() {
            String objectId = registerAnalyzed("content");
            manager.softDelete(objectId, principal);
            manager.purgeRawData(objectId, principal);

            assertThat(manager.recordContentVersion(objectId, "new content", principal)
                    .successful()).isFalse();
        }

        @Test
        @DisplayName("version history of an unknown object is empty")
        void historyOfUnknownIsEmpty() {
            assertThat(manager.versionHistory("sfs-obj-9999-00000000")).isEmpty();
        }

        @Test
        @DisplayName("version events are auditable with the acting principal")
        void versionEventsCarryPrincipal() {
            String objectId = registerAnalyzed("content");
            manager.recordContentVersion(objectId, "changed", principal);

            var events = manager.auditLog().eventsFor(objectId);
            assertThat(events).anySatisfy(event -> {
                assertThat(event.type()).isEqualTo(LifecycleEventType.VERSION_ADDED);
                assertThat(event.principalId()).isEqualTo("operator");
                assertThat(event.reason()).contains("version 2");
            });
        }
    }

    @Nested
    @DisplayName("history query")
    class HistoryQuery {

        @Test
        @DisplayName("a fresh registration exposes exactly version 1")
        void freshRegistrationHasVersionOne() {
            String objectId = registerAnalyzed("content");
            var history = manager.versionHistory(objectId);
            assertThat(history).hasSize(1);
            FileVersion first = history.getFirst();
            assertThat(first.number()).isEqualTo(1);
            assertThat(first.capturedAt()).isEqualTo(T0);
        }
    }
}
