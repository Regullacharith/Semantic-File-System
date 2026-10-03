package com.sfs.security;

import com.sfs.contracts.security.SecuritySettingsService;
import com.sfs.contracts.security.SecuritySettingsView;

import java.util.Objects;

public final class PolicySecuritySettingsService implements SecuritySettingsService {

    private final SecurityPolicyEngine policyEngine;
    private final SecurityAuditLog auditLog;
    private final String keyStorageDescription;

    public PolicySecuritySettingsService(SecurityPolicyEngine policyEngine,
                                         SecurityAuditLog auditLog,
                                         String keyStorageDescription) {
        this.policyEngine = Objects.requireNonNull(policyEngine,
                "policyEngine must not be null");
        this.auditLog = Objects.requireNonNull(auditLog, "auditLog must not be null");
        this.keyStorageDescription = Objects.requireNonNull(keyStorageDescription,
                "keyStorageDescription must not be null");
    }

    @Override
    public SecuritySettingsView getSettings() {
        return new SecuritySettingsView(
                policyEngine.policies(),
                true,
                true,
                true,
                true,
                keyStorageDescription,
                auditLog.events());
    }
}
