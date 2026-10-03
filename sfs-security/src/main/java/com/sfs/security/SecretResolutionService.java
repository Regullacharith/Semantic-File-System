package com.sfs.security;

import com.sfs.contracts.security.AuthorizationService;
import com.sfs.contracts.security.Capability;
import com.sfs.contracts.security.Principal;
import com.sfs.contracts.security.SecretRecord;

import java.util.Objects;

public final class SecretResolutionService {

    private final FileEncryptedSecureStore store;
    private final AuthorizationService authorizationService;
    private final SecurityPolicyEngine policyEngine;
    private final SecurityAuditLog auditLog;

    public SecretResolutionService(FileEncryptedSecureStore store,
                                   AuthorizationService authorizationService,
                                   SecurityPolicyEngine policyEngine,
                                   SecurityAuditLog auditLog) {
        this.store = Objects.requireNonNull(store, "store must not be null");
        this.authorizationService = Objects.requireNonNull(authorizationService,
                "authorizationService must not be null");
        this.policyEngine = Objects.requireNonNull(policyEngine,
                "policyEngine must not be null");
        this.auditLog = Objects.requireNonNull(auditLog, "auditLog must not be null");
    }

    public String resolve(String referenceId, Principal principal) {
        Objects.requireNonNull(principal, "principal must not be null");
        if (!authorizationService.isPermitted(principal, Capability.RESOLVE_SECRET)) {
            auditLog.record("Reference resolution denied",
                    "Attempt to resolve " + referenceId + " refused: "
                            + principal.id() + " lacks the resolve capability.",
                    false);
            throw new SecretResolutionDeniedException(
                    "Resolving a protected reference requires the resolve capability.");
        }
        SecretRecord record = store.find(referenceId)
                .orElseThrow(() -> {
                    auditLog.record("Reference resolution denied",
                            "Attempt to resolve " + referenceId + " refused: no "
                                    + "stored secret exists for the reference.",
                            false);
                    return new SecretResolutionDeniedException(
                            "No stored secret exists for that reference.");
                });
        if (!record.sensitiveType().isReversibleByDefault()) {
            auditLog.record("Reference resolution denied",
                    "Attempt to resolve " + referenceId + " refused: "
                            + record.sensitiveType().getLabel()
                            + " policy is non-reversible.",
                    false);
            throw new SecretResolutionDeniedException(
                    record.sensitiveType().getLabel()
                            + " values are never resolvable.");
        }
        try {
            String value = store.decrypt(record);
            auditLog.record("Reference resolution permitted",
                    record.sensitiveType().getLabel() + " " + referenceId
                            + " resolved for " + principal.id() + ".", true);
            return value;
        } catch (RuntimeException e) {
            auditLog.record("Reference resolution denied",
                    "Attempt to resolve " + referenceId + " refused: decryption "
                            + "was not possible under the current key.",
                    false);
            throw e;
        }
    }

    public boolean isPolicyResolvable(String referenceId) {
        return store.find(referenceId)
                .map(record -> policyEngine.policyFor(record.sensitiveType())
                        .handling().isReversible())
                .orElse(false);
    }
}
