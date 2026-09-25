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
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Import acceptance policy")
class ImportAcceptancePolicyTest {

    private static final Instant T0 = Instant.parse("2026-03-15T10:00:00Z");

    private FileLifecycleManager manager;
    private ImportAcceptancePolicy policy;

    @BeforeEach
    void setUp() {
        policy = (fileName, contentType) -> fileName.toLowerCase(java.util.Locale.ROOT)
                .endsWith(".txt")
                ? Optional.empty()
                : Optional.of("No registered adapter supports '" + fileName + "'.");
        manager = new FileLifecycleManager(Clock.fixed(T0, ZoneOffset.UTC),
                new InMemoryRawContentStore(), new ObjectIdService(), null, policy);
    }

    @Test
    @DisplayName("a supported extension imports normally")
    void supportedImportSucceeds() {
        var result = manager.importFile(new FileImportRequest("notes.txt", "content", "text/plain"));
        assertThat(result.successful()).isTrue();
        assertThat(manager.registeredFile(result.objectId())).isPresent();
    }

    @Test
    @DisplayName("an unsupported extension is refused at import with an explicit reason")
    void unsupportedImportRefused() {
        var result = manager.importFile(new FileImportRequest("photo.png", "content", "image/png"));
        assertThat(result.successful()).isFalse();
        assertThat(result.message()).contains("No registered adapter supports");
        assertThat(manager.listFiles()).isEmpty();
        assertThat(manager.auditLog().totalEvents()).isZero();
    }

    @Test
    @DisplayName("renaming to an unsupported extension is refused and audited")
    void renameToUnsupportedRefused() {
        var imported = manager.importFile(new FileImportRequest("notes.txt", "content", "text/plain"));
        Principal principal = new Principal("operator", "Operator", Set.of(Capability.values()));

        var rename = manager.renameObject(imported.objectId(), "photo.png", principal);

        assertThat(rename.successful()).isFalse();
        assertThat(manager.findByObjectId(imported.objectId()).orElseThrow().displayName())
                .isEqualTo("notes.txt");
        assertThat(manager.auditLog().eventsFor(imported.objectId()))
                .anySatisfy(event -> assertThat(event.type())
                        .isEqualTo(com.sfs.lifecycle.model.LifecycleEventType.METADATA_UPDATE_REFUSED));
    }

    @Test
    @DisplayName("a manager without a policy accepts everything")
    void nullPolicyAcceptsAll() {
        FileLifecycleManager open = new FileLifecycleManager(Clock.fixed(T0, ZoneOffset.UTC),
                new InMemoryRawContentStore(), new ObjectIdService());
        var result = open.importFile(new FileImportRequest("blob.bin", "content", null));
        assertThat(result.successful()).isTrue();
    }
}
