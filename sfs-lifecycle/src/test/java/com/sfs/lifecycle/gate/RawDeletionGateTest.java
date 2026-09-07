package com.sfs.lifecycle.gate;

import com.sfs.lifecycle.model.DeletionPolicy;
import com.sfs.lifecycle.state.FileState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RawDeletionGate")
class RawDeletionGateTest {

    private final RawDeletionGate gate = new RawDeletionGate();

    @Nested
    @DisplayName("v1 policy")
    class V1Policy {

        @Test
        @DisplayName("allows release for a soft-deleted object whose memory was committed")
        void allowsCommittedDeletion() {
            var decision = gate.evaluate(FileState.SOFT_DELETED, FileState.MEMORY_COMMITTED);
            assertThat(decision.allowed()).isTrue();
        }

        @Test
        @DisplayName("refuses release for a soft-deleted object without committed memory")
        void refusesUncommittedDeletion() {
            var decision = gate.evaluate(FileState.SOFT_DELETED, FileState.ANALYZED);
            assertThat(decision.allowed()).isFalse();
            assertThat(decision.refusalReason()).contains("durably");
        }

        @Test
        @DisplayName("refuses release for any live or terminal state")
        void refusesNonDeletedStates() {
            for (FileState state : FileState.values()) {
                if (state == FileState.SOFT_DELETED) {
                    continue;
                }
                var decision = gate.evaluate(state, FileState.MEMORY_COMMITTED);
                assertThat(decision.allowed()).isFalse();
            }
        }
    }

    @Nested
    @DisplayName("policy validation")
    class PolicyValidation {

        @Test
        @DisplayName("the v1 policy requires the deleted state and the memory commit")
        void v1RequiresBothControls() {
            DeletionPolicy policy = DeletionPolicy.v1();
            assertThat(policy.requireSoftDeletedState()).isTrue();
            assertThat(policy.requireMemoryCommit()).isTrue();
        }

        @Test
        @DisplayName("memory-commit enforcement without the deleted state is contradictory")
        void contradictoryPolicyRejected() {
            assertThatThrownBy(() -> new DeletionPolicy(false, true))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("a weaker policy follows its own terms")
        void weakerPolicyFollowsItsTerms() {
            RawDeletionGate lenient = new RawDeletionGate(new DeletionPolicy(true, false));
            assertThat(lenient.evaluate(FileState.SOFT_DELETED, FileState.ANALYZED).allowed())
                    .isTrue();
        }

        @Test
        @DisplayName("the gate exposes the policy it enforces")
        void exposesPolicy() {
            assertThat(gate.policy()).isEqualTo(DeletionPolicy.v1());
        }
    }
}
