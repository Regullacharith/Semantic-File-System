package com.sfs.security;

public final class SecretResolutionDeniedException extends RuntimeException {

    public SecretResolutionDeniedException(String reason) {
        super(reason);
    }
}
