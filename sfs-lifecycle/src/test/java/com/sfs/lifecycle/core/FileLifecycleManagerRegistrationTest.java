package com.sfs.lifecycle.core;

import com.sfs.contracts.file.FileImportRequest;
import com.sfs.contracts.file.FileOperationResult;
import com.sfs.contracts.file.FileStatus;
import com.sfs.contracts.file.FileSummary;
import com.sfs.lifecycle.identity.ObjectIdService;
import com.sfs.lifecycle.model.ContentDigest;
import com.sfs.lifecycle.model.FileVersion;
import com.sfs.lifecycle.model.LifecycleEventType;
import com.sfs.lifecycle.store.InMemoryRawContentStore;
import com.sfs.lifecycle.store.RawContentStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FileLifecycleManager registration")
class FileLifecycleManagerRegistrationTest {

    private static final Instant T0 = Instant.parse("2026-03-15T10:00:00Z");

    private FileLifecycleManager manager;
    private RawContentStore rawContentStore;

    @BeforeEach
    void setUp() {
        rawContentStore = new InMemoryRawContentStore();
        manager = new FileLifecycleManager(Clock.fixed(T0, ZoneOffset.UTC),
                rawContentStore, new ObjectIdService(), null);
    }

    private FileOperationResult register(String fileName, String content) {
        return manager.importFile(new FileImportRequest(fileName, content, "text/plain"));
    }

    @Nested
    @DisplayName("registration")
    class Registration {

        @Test
        @DisplayName("assigns a valid, unique Object ID")
        void assignsValidUniqueObjectId() {
            FileOperationResult first = register("notes.txt", "first");
            FileOperationResult second = register("other.txt", "second");

            assertThat(first.successful()).isTrue();
            assertThat(second.successful()).isTrue();
            assertThat(first.objectId()).isNotEqualTo(second.objectId());
            assertThat(manager.registeredFile(first.objectId())).isPresent();
        }

        @Test
        @DisplayName("captures metadata with byte size and digest")
        void capturesMetadataWithByteSizeAndDigest() {
            String content = "héllo wörld";
            int expectedBytes = content.getBytes(StandardCharsets.UTF_8).length;
            FileOperationResult result = register("unicode.txt", content);

            var file = manager.registeredFile(result.objectId()).orElseThrow();
            assertThat(file.metadata().fileName()).isEqualTo("unicode.txt");
            assertThat(file.metadata().sizeBytes()).isEqualTo(expectedBytes);
            assertThat(file.metadata().sha256()).isEqualTo(ContentDigest.sha256Hex(content));
            assertThat(file.metadata().contentType()).isEqualTo("text/plain");
            assertThat(file.metadata().storageAddress()).isNull();
        }

        @Test
        @DisplayName("records version 1 at registration")
        void recordsVersionOneAtRegistration() {
            FileOperationResult result = register("notes.txt", "versioned content");
            var file = manager.registeredFile(result.objectId()).orElseThrow();
            FileVersion version = file.currentVersion();
            assertThat(version.number()).isEqualTo(1);
            assertThat(version.contentSha256()).isEqualTo(ContentDigest.sha256Hex("versioned content"));
            assertThat(version.sizeBytes()).isEqualTo("versioned content".getBytes(StandardCharsets.UTF_8).length);
            assertThat(version.capturedAt()).isEqualTo(T0);
        }

        @Test
        @DisplayName("retains raw content for the live object")
        void retainsRawContentForLiveObject() {
            FileOperationResult result = register("notes.txt", "retained bytes");
            assertThat(rawContentStore.contains(result.objectId())).isTrue();
            assertThat(new String(rawContentStore.retrieve(result.objectId()).orElseThrow(),
                    StandardCharsets.UTF_8)).isEqualTo("retained bytes");
        }

        @Test
        @DisplayName("defaults a blank content type to text/plain")
        void defaultsBlankContentType() {
            FileOperationResult result = manager.importFile(
                    new FileImportRequest("notes.txt", "content", " "));
            var file = manager.registeredFile(result.objectId()).orElseThrow();
            assertThat(file.metadata().contentType()).isEqualTo("text/plain");
        }

        @Test
        @DisplayName("refuses a null request without recording anything")
        void refusesNullRequest() {
            FileOperationResult result = manager.importFile(null);
            assertThat(result.successful()).isFalse();
            assertThat(result.message()).isNotBlank();
            assertThat(manager.listFiles()).isEmpty();
        }

        @Test
        @DisplayName("records a registration event in the audit log")
        void recordsRegistrationEvent() {
            FileOperationResult result = register("audited.txt", "content");
            var events = manager.auditLog().eventsFor(result.objectId());
            assertThat(events).hasSize(1);
            assertThat(events.getFirst().type()).isEqualTo(LifecycleEventType.REGISTRATION_RECORDED);
            assertThat(events.getFirst().to()).isEqualTo(com.sfs.lifecycle.state.FileState.REGISTERED);
            assertThat(events.getFirst().refused()).isFalse();
            assertThat(events.getFirst().at()).isEqualTo(T0);
        }
    }

    @Nested
    @DisplayName("listing and lookup")
    class ListingAndLookup {

        @Test
        @DisplayName("lists files newest first with REGISTERED status")
        void listsNewestFirstAsRegistered() {
            register("a.txt", "a");
            register("b.txt", "b");

            List<FileSummary> files = manager.listFiles();
            assertThat(files).hasSize(2);
            assertThat(files).allSatisfy(f -> assertThat(f.status()).isEqualTo(FileStatus.REGISTERED));
        }

        @Test
        @DisplayName("lookup by Object ID finds the registered file")
        void lookupByObjectIdFindsFile() {
            FileOperationResult result = register("findme.txt", "content");
            FileSummary summary = manager.findByObjectId(result.objectId()).orElseThrow();
            assertThat(summary.displayName()).isEqualTo("findme.txt");
            assertThat(summary.objectId()).isEqualTo(result.objectId());
            assertThat(manager.findByObjectId("sfs-obj-9999-00000000")).isEmpty();
            assertThat(manager.findByObjectId("")).isEmpty();
        }
    }
}
