package com.sfs.reconstruction.engine;

public final class ReconstructionRefusalException extends RuntimeException {

    public ReconstructionRefusalException(String reason) {
        super(reason);
    }
}
