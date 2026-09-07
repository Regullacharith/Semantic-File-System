package com.sfs.lifecycle.state;

import com.sfs.lifecycle.model.LifecycleEventType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LifecycleStateMachine")
class LifecycleStateMachineTest {

    private final LifecycleStateMachine machine = new LifecycleStateMachine();

    @Nested
    @DisplayName("legal transitions")
    class LegalTransitions {

        @Test
        @DisplayName("analysis path is legal from REGISTERED and from FAILED")
        void analysisFromRegisteredAndFailed() {
            assertThat(machine.requireTarget(FileState.REGISTERED, LifecycleEventType.ANALYSIS_STARTED))
                    .isEqualTo(FileState.ANALYZING);
            assertThat(machine.requireTarget(FileState.FAILED, LifecycleEventType.ANALYSIS_STARTED))
                    .isEqualTo(FileState.ANALYZING);
        }

        @Test
        @DisplayName("analysis outcome is legal only from ANALYZING")
        void analysisOutcomeOnlyFromAnalyzing() {
            assertThat(machine.requireTarget(FileState.ANALYZING, LifecycleEventType.ANALYSIS_SUCCEEDED))
                    .isEqualTo(FileState.ANALYZED);
            assertThat(machine.requireTarget(FileState.ANALYZING, LifecycleEventType.ANALYSIS_FAILED))
                    .isEqualTo(FileState.FAILED);
        }

        @Test
        @DisplayName("memorize chain goes ANALYZED to MEMORIZABLE to MEMORY_COMMITTED")
        void memorizeChain() {
            assertThat(machine.requireTarget(FileState.ANALYZED, LifecycleEventType.MEMORY_COMMIT_REQUESTED))
                    .isEqualTo(FileState.ANALYZED);
            assertThat(machine.requireTarget(FileState.ANALYZED, LifecycleEventType.DNA_VALIDATED))
                    .isEqualTo(FileState.MEMORIZABLE);
            assertThat(machine.requireTarget(FileState.MEMORIZABLE, LifecycleEventType.MEMORY_COMMITTED))
                    .isEqualTo(FileState.MEMORY_COMMITTED);
        }

        @Test
        @DisplayName("soft deletion is legal from ANALYZED and MEMORY_COMMITTED")
        void softDeletionFromLiveStates() {
            assertThat(machine.requireTarget(FileState.ANALYZED, LifecycleEventType.SOFT_DELETED))
                    .isEqualTo(FileState.SOFT_DELETED);
            assertThat(machine.requireTarget(FileState.MEMORY_COMMITTED, LifecycleEventType.SOFT_DELETED))
                    .isEqualTo(FileState.SOFT_DELETED);
        }

        @Test
        @DisplayName("raw release is legal only from SOFT_DELETED")
        void rawReleaseOnlyFromSoftDeleted() {
            assertThat(machine.requireTarget(FileState.SOFT_DELETED, LifecycleEventType.RAW_RELEASED))
                    .isEqualTo(FileState.MEMORIZED);
        }

        @Test
        @DisplayName("memorize interruption recovery returns MEMORIZABLE to ANALYZED")
        void interruptionRecovery() {
            assertThat(machine.requireTarget(FileState.MEMORIZABLE, LifecycleEventType.MEMORIZE_INTERRUPTED))
                    .isEqualTo(FileState.ANALYZED);
        }
    }

    @Nested
    @DisplayName("illegal transitions")
    class IllegalTransitions {

        @Test
        @DisplayName("DNA validation is refused from every state but ANALYZED")
        void memorizeRefusedElsewhere() {
            for (FileState state : FileState.values()) {
                if (state == FileState.ANALYZED) {
                    continue;
                }
                assertThatThrownBy(() -> machine.requireTarget(state, LifecycleEventType.DNA_VALIDATED))
                        .isInstanceOf(IllegalLifecycleTransitionException.class);
            }
        }

        @Test
        @DisplayName("memory commit is refused from every state but MEMORIZABLE")
        void commitRefusedOutsideMemorizable() {
            for (FileState state : FileState.values()) {
                if (state == FileState.MEMORIZABLE) {
                    continue;
                }
                assertThatThrownBy(() -> machine.requireTarget(state, LifecycleEventType.MEMORY_COMMITTED))
                        .isInstanceOf(IllegalLifecycleTransitionException.class);
            }
        }

        @Test
        @DisplayName("a terminal object allows no forward transition at all")
        void terminalAllowsNothing() {
            for (LifecycleEventType event : LifecycleEventType.values()) {
                if (event == LifecycleEventType.SOFT_DELETE_REFUSED
                        || event == LifecycleEventType.UNDO_DELETE_REFUSED
                        || event == LifecycleEventType.PURGE_REFUSED
                        || event == LifecycleEventType.MEMORY_COMMIT_REFUSED
                        || event == LifecycleEventType.ANALYSIS_REFUSED
                        || event == LifecycleEventType.METADATA_UPDATE_REFUSED
                        || event == LifecycleEventType.VERSION_ADD_REFUSED) {
                    continue;
                }
                assertThatThrownBy(() -> machine.requireTarget(FileState.MEMORIZED, event))
                        .isInstanceOf(IllegalLifecycleTransitionException.class);
            }
        }

        @Test
        @DisplayName("a registered object cannot be deleted, purged or restored")
        void registeredCannotDeleteOrPurge() {
            assertThatThrownBy(() -> machine.requireTarget(FileState.REGISTERED, LifecycleEventType.SOFT_DELETED))
                    .isInstanceOf(IllegalLifecycleTransitionException.class);
            assertThatThrownBy(() -> machine.requireTarget(FileState.REGISTERED, LifecycleEventType.RAW_RELEASED))
                    .isInstanceOf(IllegalLifecycleTransitionException.class);
            assertThatThrownBy(() -> machine.requireTarget(FileState.REGISTERED, LifecycleEventType.UNDO_DELETED))
                    .isInstanceOf(IllegalLifecycleTransitionException.class);
        }

        @Test
        @DisplayName("an analyzed object cannot skip the memory commit before raw release")
        void analyzedCannotSkipCommitForRelease() {
            assertThatThrownBy(() -> machine.requireTarget(FileState.ANALYZED, LifecycleEventType.RAW_RELEASED))
                    .isInstanceOf(IllegalLifecycleTransitionException.class);
        }

        @Test
        @DisplayName("undo accepts only a genuine pre-delete origin")
        void undoAcceptsOnlyGenuineOrigins() {
            assertThat(machine.requireUndoTarget(FileState.SOFT_DELETED, FileState.ANALYZED))
                    .isEqualTo(FileState.ANALYZED);
            assertThat(machine.requireUndoTarget(FileState.SOFT_DELETED, FileState.MEMORY_COMMITTED))
                    .isEqualTo(FileState.MEMORY_COMMITTED);
            assertThatThrownBy(() -> machine.requireUndoTarget(FileState.SOFT_DELETED, FileState.SOFT_DELETED))
                    .isInstanceOf(IllegalLifecycleTransitionException.class);
            assertThatThrownBy(() -> machine.requireUndoTarget(FileState.SOFT_DELETED, FileState.MEMORIZED))
                    .isInstanceOf(IllegalLifecycleTransitionException.class);
        }
    }

    @Nested
    @DisplayName("refusal events")
    class RefusalEvents {

        @Test
        @DisplayName("refusals are recordable from any state without changing state")
        void refusalsRecordableFromAnyState() {
            for (FileState state : FileState.values()) {
                assertThatCode(() -> machine.requireTarget(state, LifecycleEventType.SOFT_DELETE_REFUSED))
                        .doesNotThrowAnyException();
                assertThat(machine.requireTarget(state, LifecycleEventType.SOFT_DELETE_REFUSED))
                        .isEqualTo(state);
            }
        }
    }
}
