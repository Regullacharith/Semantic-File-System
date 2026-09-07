package com.sfs.lifecycle.audit;

import com.sfs.contracts.file.FileImportRequest;
import com.sfs.contracts.lifecycle.LifecycleAuditService;
import com.sfs.contracts.lifecycle.LifecycleStatistics;
import com.sfs.contracts.security.Capability;
import com.sfs.contracts.security.Principal;
import com.sfs.lifecycle.core.FileLifecycleManager;
import com.sfs.lifecycle.identity.ObjectIdService;
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

@DisplayName("LifecycleAuditAdapter")
class LifecycleAuditAdapterTest {

    private static final Instant T0 = Instant.parse("2026-03-15T10:00:00Z");

    private FileLifecycleManager manager;
    private LifecycleAuditService auditService;
    private Principal principal;

    @BeforeEach
    void setUp() {
        manager = new FileLifecycleManager(Clock.fixed(T0, ZoneOffset.UTC),
                new InMemoryRawContentStore(), new ObjectIdService(), null);
        auditService = new LifecycleAuditAdapter(manager.auditLog());
        principal = new Principal("operator", "Operator", Set.of(Capability.values()));
    }

    private String analyzedObject() {
        var result = manager.importFile(
                new FileImportRequest("notes.txt", "content", "text/plain"));
        manager.requestAnalysis(result.objectId());
        manager.completeAnalysisSuccess(result.objectId(), "stub-dna/0.1");
        return result.objectId();
    }

    @Nested
    @DisplayName("event mapping")
    class EventMapping {

        @Test
        @DisplayName("exposes lifecycle events as contract entries with string states")
        void exposesEntriesWithStringStates() {
            String objectId = analyzedObject();
            manager.memorize(objectId, principal);

            var events = auditService.eventsFor(objectId);

            assertThat(events).hasSize(6);
            assertThat(events.getFirst().type()).isEqualTo("REGISTRATION_RECORDED");
            assertThat(events.getFirst().fromState()).isNull();
            assertThat(events.getFirst().toState()).isEqualTo("REGISTERED");
            assertThat(events.getLast().type()).isEqualTo("MEMORY_COMMITTED");
            assertThat(events.getLast().fromState()).isEqualTo("MEMORIZABLE");
            assertThat(events.getLast().principalId()).isEqualTo("operator");
        }

        @Test
        @DisplayName("carries refusal reasons into the contract entries")
        void carriesRefusalReasons() {
            String objectId = analyzedObject();
            manager.memorize(objectId, principal);
            manager.memorize(objectId, principal);

            var events = auditService.eventsFor(objectId);

            assertThat(events).anySatisfy(entry -> {
                assertThat(entry.refused()).isTrue();
                assertThat(entry.reason()).contains("Memorization requires");
                assertThat(entry.fromState()).isEqualTo(entry.toState());
            });
        }

        @Test
        @DisplayName("returns no events for unknown objects")
        void emptyForUnknown() {
            assertThat(auditService.eventsFor("sfs-obj-9999-00000000")).isEmpty();
        }
    }

    @Nested
    @DisplayName("statistics")
    class Statistics {

        @Test
        @DisplayName("an empty log yields zeroed statistics without durations")
        void emptyLogYieldsZeroes() {
            LifecycleStatistics stats = auditService.statistics();

            assertThat(stats.totalEvents()).isZero();
            assertThat(stats.refusedEvents()).isZero();
            assertThat(stats.memoryCommitCount()).isZero();
            assertThat(stats.averageMemoryCommitMillis()).isNull();
            assertThat(stats.maxMemoryCommitMillis()).isNull();
        }

        @Test
        @DisplayName("counts commits, purges and refusals from the audit trail")
        void countsOperationsAndRefusals() {
            String objectId = analyzedObject();
            manager.memorize(objectId, principal);
            manager.softDelete(objectId, principal);
            manager.purgeRawData(objectId, principal);
            manager.memorize(objectId, principal);
            manager.purgeRawData(objectId, principal);

            LifecycleStatistics stats = auditService.statistics();

            assertThat(stats.totalEvents()).isEqualTo(11);
            assertThat(stats.memoryCommitCount()).isEqualTo(1);
            assertThat(stats.memoryCommitRefusedCount()).isEqualTo(1);
            assertThat(stats.purgeCount()).isEqualTo(1);
            assertThat(stats.purgeRefusedCount()).isEqualTo(1);
            assertThat(stats.refusedEvents()).isEqualTo(2);
            assertThat(stats.eventCounts()).containsEntry("MEMORY_COMMITTED", 1L);
        }

        @Test
        @DisplayName("captures memorize durations")
        void capturesMemorizeDurations() {
            String objectId = analyzedObject();
            manager.memorize(objectId, principal);

            LifecycleStatistics stats = auditService.statistics();

            assertThat(stats.averageMemoryCommitMillis()).isNotNull();
            assertThat(stats.averageMemoryCommitMillis()).isGreaterThanOrEqualTo(0);
            assertThat(stats.maxMemoryCommitMillis()).isGreaterThanOrEqualTo(0);
        }
    }
}
