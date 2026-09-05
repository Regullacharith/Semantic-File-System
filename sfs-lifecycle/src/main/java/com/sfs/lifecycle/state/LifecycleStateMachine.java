package com.sfs.lifecycle.state;

import com.sfs.lifecycle.model.LifecycleEventType;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class LifecycleStateMachine {

    private final Map<LifecycleEventType, Set<FileState>> legalSourceStates =
            new EnumMap<>(LifecycleEventType.class);

    public LifecycleStateMachine() {
        legalSourceStates.put(LifecycleEventType.REGISTRATION_RECORDED, Set.of());
        legalSourceStates.put(LifecycleEventType.ANALYSIS_STARTED,
                Set.of(FileState.REGISTERED, FileState.FAILED));
        legalSourceStates.put(LifecycleEventType.ANALYSIS_SUCCEEDED, Set.of(FileState.ANALYZING));
        legalSourceStates.put(LifecycleEventType.ANALYSIS_FAILED, Set.of(FileState.ANALYZING));
        legalSourceStates.put(LifecycleEventType.ANALYSIS_REFUSED, allStates());
        legalSourceStates.put(LifecycleEventType.MEMORY_COMMIT_REQUESTED, Set.of(FileState.ANALYZED));
        legalSourceStates.put(LifecycleEventType.DNA_VALIDATED, Set.of(FileState.ANALYZED));
        legalSourceStates.put(LifecycleEventType.MEMORY_COMMITTED, Set.of(FileState.MEMORIZABLE));
        legalSourceStates.put(LifecycleEventType.MEMORY_COMMIT_REFUSED,
                Set.of(FileState.ANALYZED, FileState.MEMORIZABLE));
        legalSourceStates.put(LifecycleEventType.SOFT_DELETED,
                Set.of(FileState.ANALYZED, FileState.MEMORY_COMMITTED));
        legalSourceStates.put(LifecycleEventType.SOFT_DELETE_REFUSED, allStates());
        legalSourceStates.put(LifecycleEventType.UNDO_DELETED, Set.of(FileState.SOFT_DELETED));
        legalSourceStates.put(LifecycleEventType.UNDO_DELETE_REFUSED, allStates());
        legalSourceStates.put(LifecycleEventType.PURGE_REQUESTED, Set.of(FileState.SOFT_DELETED));
        legalSourceStates.put(LifecycleEventType.RAW_RELEASED, Set.of(FileState.SOFT_DELETED));
        legalSourceStates.put(LifecycleEventType.PURGE_REFUSED, allStates());
        legalSourceStates.put(LifecycleEventType.METADATA_UPDATED,
                Set.of(FileState.REGISTERED, FileState.ANALYZED,
                        FileState.MEMORY_COMMITTED, FileState.FAILED));
        legalSourceStates.put(LifecycleEventType.METADATA_UPDATE_REFUSED, allStates());
        legalSourceStates.put(LifecycleEventType.VERSION_ADDED,
                Set.of(FileState.REGISTERED, FileState.ANALYZED,
                        FileState.MEMORY_COMMITTED, FileState.FAILED));
        legalSourceStates.put(LifecycleEventType.VERSION_ADD_REFUSED, allStates());
        legalSourceStates.put(LifecycleEventType.MEMORIZE_INTERRUPTED, Set.of(FileState.MEMORIZABLE));
    }

    public FileState requireTarget(FileState from, LifecycleEventType event) {
        Objects.requireNonNull(from, "from must not be null");
        Objects.requireNonNull(event, "event must not be null");
        if (!isLegal(from, event)) {
            throw new IllegalLifecycleTransitionException(event, from);
        }
        return targetOf(from, event);
    }

    public FileState requireUndoTarget(FileState from, FileState origin) {
        requireTarget(from, LifecycleEventType.UNDO_DELETED);
        if (origin != FileState.ANALYZED && origin != FileState.MEMORY_COMMITTED) {
            throw new IllegalLifecycleTransitionException(LifecycleEventType.UNDO_DELETED, from);
        }
        return origin;
    }

    public boolean isLegal(FileState from, LifecycleEventType event) {
        Objects.requireNonNull(event, "event must not be null");
        Set<FileState> sources = legalSourceStates.get(event);
        if (sources == null) {
            throw new IllegalArgumentException("unknown lifecycle event " + event);
        }
        if (from == null) {
            return event == LifecycleEventType.REGISTRATION_RECORDED;
        }
        return event != LifecycleEventType.REGISTRATION_RECORDED && sources.contains(from);
    }

    private static FileState targetOf(FileState from, LifecycleEventType event) {
        return switch (event) {
            case REGISTRATION_RECORDED -> FileState.REGISTERED;
            case ANALYSIS_STARTED -> FileState.ANALYZING;
            case ANALYSIS_SUCCEEDED -> FileState.ANALYZED;
            case ANALYSIS_FAILED -> FileState.FAILED;
            case MEMORY_COMMIT_REQUESTED -> from;
            case DNA_VALIDATED -> FileState.MEMORIZABLE;
            case MEMORY_COMMITTED -> FileState.MEMORY_COMMITTED;
            case SOFT_DELETED -> FileState.SOFT_DELETED;
            case UNDO_DELETED -> from;
            case PURGE_REQUESTED -> from;
            case RAW_RELEASED -> FileState.MEMORIZED;
            case METADATA_UPDATED -> from;
            case METADATA_UPDATE_REFUSED -> from;
            case VERSION_ADDED -> from;
            case VERSION_ADD_REFUSED -> from;
            case MEMORIZE_INTERRUPTED -> FileState.ANALYZED;
            case MEMORY_COMMIT_REFUSED, SOFT_DELETE_REFUSED, UNDO_DELETE_REFUSED, PURGE_REFUSED,
                    ANALYSIS_REFUSED ->
                    from;
        };
    }

    private static Set<FileState> allStates() {
        return Set.of(FileState.values());
    }
}
