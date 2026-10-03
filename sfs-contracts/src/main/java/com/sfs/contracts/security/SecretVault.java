package com.sfs.contracts.security;

import java.util.Optional;

public interface SecretVault {

    Optional<SecretRecord> store(SecretSubmission submission);

    Optional<SecretRecord> find(String referenceId);

    boolean contains(String referenceId);

    int size();
}
