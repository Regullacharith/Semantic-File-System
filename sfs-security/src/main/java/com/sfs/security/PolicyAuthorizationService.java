package com.sfs.security;

import com.sfs.contracts.security.AuthorizationService;
import com.sfs.contracts.security.Capability;
import com.sfs.contracts.security.Principal;

import java.util.Objects;

public final class PolicyAuthorizationService implements AuthorizationService {

    private final SecurityAuditLog auditLog;

    public PolicyAuthorizationService(SecurityAuditLog auditLog) {
        this.auditLog = Objects.requireNonNull(auditLog, "auditLog must not be null");
    }

    @Override
    public boolean isPermitted(Principal principal, Capability capability) {
        Objects.requireNonNull(principal, "principal must not be null");
        Objects.requireNonNull(capability, "capability must not be null");
        boolean permitted = principal.has(capability);
        if (!permitted && capability == Capability.RESOLVE_SECRET) {
            auditLog.record("Reference resolution denied",
                    "Attempt to resolve a protected reference refused: "
                            + principal.id() + " does not hold the capability.",
                    false);
        }
        return permitted;
    }
}
