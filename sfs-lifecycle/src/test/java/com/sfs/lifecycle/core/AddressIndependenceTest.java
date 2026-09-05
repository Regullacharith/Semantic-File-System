package com.sfs.lifecycle.core;

import com.sfs.contracts.file.FileImportRequest;
import com.sfs.contracts.security.Capability;
import com.sfs.contracts.security.Principal;
import com.sfs.lifecycle.identity.ObjectIdService;
import com.sfs.lifecycle.store.InMemoryRawContentStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Address independence of deleted records")
class AddressIndependenceTest {

    private FileLifecycleManager manager;
    private InMemoryRawContentStore rawContentStore;
    private Principal principal;

    @BeforeEach
    void setUp() {
        rawContentStore = new InMemoryRawContentStore();
        manager = new FileLifecycleManager(Clock.systemUTC(),
                rawContentStore, new ObjectIdService(), null);
        principal = new Principal("custodian", "Custodian", Set.of(Capability.values()));
    }

    private String memorizedObject() {
        var result = manager.importFile(
                new FileImportRequest("notes.txt", "purgeable content", "text/plain"));
        manager.requestAnalysis(result.objectId());
        manager.completeAnalysisSuccess(result.objectId(), "stub-dna/0.1");
        manager.memorize(result.objectId(), principal);
        manager.softDelete(result.objectId(), principal);
        manager.purgeRawData(result.objectId(), principal);
        return result.objectId();
    }

    @Test
    @DisplayName("a purged record remains addressable by its Object ID alone")
    void purgedRecordAddressableByObjectId() {
        String objectId = memorizedObject();

        var summary = manager.findByObjectId(objectId).orElseThrow();
        assertThat(summary.objectId()).isEqualTo(objectId);
        assertThat(manager.registeredFile(objectId)).isPresent();
    }

    @Test
    @DisplayName("a purged record carries no storage address")
    void purgedRecordHasNoStorageAddress() {
        String objectId = memorizedObject();

        var file = manager.registeredFile(objectId).orElseThrow();
        assertThat(file.metadata().storageAddress()).isNull();
    }

    @Test
    @DisplayName("a purged record keeps its identity, name and version history")
    void purgedRecordKeepsIdentityAndHistory() {
        String objectId = memorizedObject();

        var file = manager.registeredFile(objectId).orElseThrow();
        assertThat(file.metadata().fileName()).isEqualTo("notes.txt");
        assertThat(file.versions()).hasSize(1);
        assertThat(file.objectId().value()).isEqualTo(objectId);
    }

    @Test
    @DisplayName("raw bytes are gone after purge and cannot be retrieved")
    void rawBytesGoneAfterPurge() {
        String objectId = memorizedObject();

        assertThat(rawContentStore.contains(objectId)).isFalse();
        assertThat(rawContentStore.retrieve(objectId)).isEmpty();
    }
}
