package com.sfs.lifecycle.state;

import com.sfs.lifecycle.model.LifecycleEventType;

public final class IllegalLifecycleTransitionException extends RuntimeException {

    private final LifecycleEventType attemptedEvent;
    private final FileState currentState;

    public IllegalLifecycleTransitionException(LifecycleEventType attemptedEvent,
                                               FileState currentState) {
        super("Lifecycle event " + attemptedEvent + " is not legal from state " + currentState);
        this.attemptedEvent = attemptedEvent;
        this.currentState = currentState;
    }

    public LifecycleEventType attemptedEvent() {
        return attemptedEvent;
    }

    public FileState currentState() {
        return currentState;
    }
}
