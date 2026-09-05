package com.sfs.lifecycle.core;

import com.sfs.contracts.file.FileImportRequest;
import com.sfs.contracts.security.Capability;
import com.sfs.contracts.security.Principal;
import com.sfs.lifecycle.identity.ObjectIdService;
import com.sfs.lifecycle.model.LifecycleEventType;
import com.sfs.lifecycle.model.SemanticFile;
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

@DisplayName("Restart and retry behavior")
class LifecycleRecoveryTest {

    private static final Instant T0 = Instant.parse("2026-03-15T10:00:00Z");
    private static final Instant T1 = Instant.parse("2026-03-15T10:01:00Z");

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

    private SemanticFile analyzedAggregate(String marker) {
        var result = manager.importFile(
                new FileImportRequest("restart-" + marker + ".txt", "content", "text/plain"));
        manager.requestAnalysis(result.objectId());
        manager.completeAnalysisSuccess(result.objectId(), "stub-dna/0.1");
        return manager.registeredFile(result.objectId()).orElseThrow();
    }

    @Nested
    @DisplayName("interrupted memorization")
    class InterruptedMemorization {

        @Test
        @DisplayName("an object stranded in MEMORIZABLE is rolled back to ANALYZED at startup")
        void strandedMemorizableRollsBack() {
            SemanticFile analyzed = analyzedAggregate("x");
            manager.adopt(analyzed.withState(FileState.MEMORIZABLE, T1));

            int recovered = manager.recoverInterruptedMemorizations();

            assertThat(recovered).isEqualTo(1);
            assertThat(manager.registeredFile(analyzed.objectId().value()).orElseThrow().state())
                    .isEqualTo(FileState.ANALYZED);
            assertThat(rawContentStore.contains(analyzed.objectId().value())).isTrue();
        }

        @Test
        @DisplayName("rollback is auditable with an explicit reason")
        void rollbackIsAuditable() {
            SemanticFile analyzed = analyzedAggregate("y");
            manager.adopt(analyzed.withState(FileState.MEMORIZABLE, T1));
            manager.recoverInterruptedMemorizations();

            var events = manager.auditLog().eventsFor(analyzed.objectId().value());
            assertThat(events).anySatisfy(event -> {
                assertThat(event.type()).isEqualTo(LifecycleEventType.MEMORIZE_INTERRUPTED);
                assertThat(event.from()).isEqualTo(FileState.MEMORIZABLE);
                assertThat(event.to()).isEqualTo(FileState.ANALYZED);
                assertThat(event.reason()).contains("interrupted");
            });
        }

        @Test
        @DisplayName("a rolled-back object can be memorized and purged normally afterwards")
        void rolledBackObjectRemainsUsable() {
            SemanticFile analyzed = analyzedAggregate("z");
            String objectId = analyzed.objectId().value();
            manager.adopt(analyzed.withState(FileState.MEMORIZABLE, T1));
            manager.recoverInterruptedMemorizations();

            assertThat(manager.memorize(objectId, principal).successful()).isTrue();
            manager.softDelete(objectId, principal);
            assertThat(manager.purgeRawData(objectId, principal).successful()).isTrue();
            assertThat(manager.registeredFile(objectId).orElseThrow().state())
                    .isEqualTo(FileState.MEMORIZED);
        }
    }

    @Nested
    @DisplayName("healthy state survives recovery")
    class HealthyStateUnaffected {

        @Test
        @DisplayName("recovery touches only MEMORIZABLE objects and reports zero otherwise")
        void recoveryIsSelective() {
            String live = analyzedAggregate("a").objectId().value();
            manager.memorize(live, principal);
            manager.softDelete(live, principal);

            assertThat(manager.recoverInterruptedMemorizations()).isZero();
            assertThat(manager.registeredFile(live).orElseThrow().state())
                    .isEqualTo(FileState.SOFT_DELETED);
        }

        @Test
        @DisplayName("a fresh manager recovers nothing")
        void freshManagerRecoversNothing() {
            assertThat(manager.recoverInterruptedMemorizations()).isZero();
        }
    }
}
