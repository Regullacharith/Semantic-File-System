package com.sfs.lifecycle.gate;

import com.sfs.lifecycle.model.DeletionPolicy;
import com.sfs.lifecycle.state.FileState;

import java.util.Objects;

public final class RawDeletionGate {

    public record DeletionDecision(boolean allowed, String refusalReason) {

        private static final DeletionDecision ALLOWED = new DeletionDecision(true, null);

        public static DeletionDecision ok() {
            return ALLOWED;
        }

        public static DeletionDecision refused(String reason) {
            return new DeletionDecision(false,
                    Objects.requireNonNull(reason, "reason must not be null"));
        }
    }

    private final DeletionPolicy policy;

    public RawDeletionGate() {
        this(DeletionPolicy.v1());
    }

    public RawDeletionGate(DeletionPolicy policy) {
        this.policy = Objects.requireNonNull(policy, "policy must not be null");
    }

    public DeletionPolicy policy() {
        return policy;
    }

    public DeletionDecision evaluate(FileState currentState, FileState deletedFrom) {
        Objects.requireNonNull(currentState, "currentState must not be null");
        if (policy.requireSoftDeletedState() && currentState != FileState.SOFT_DELETED) {
            return DeletionDecision.refused(
                    "Raw bytes may be released only from the deleted state, so destruction "
                            + "always takes two deliberate steps. The object is "
                            + currentState + ".");
        }
        if (policy.requireMemoryCommit() && deletedFrom != FileState.MEMORY_COMMITTED) {
            return DeletionDecision.refused(
                    "Raw bytes may be released only after semantic memory is durably "
                            + "committed. Restore the object, memorize it, then delete it again.");
        }
        return DeletionDecision.ok();
    }
}
