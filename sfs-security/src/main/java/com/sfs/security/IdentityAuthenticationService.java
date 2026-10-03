package com.sfs.security;

import com.sfs.contracts.security.AuthenticationService;
import com.sfs.contracts.security.Capability;
import com.sfs.contracts.security.Principal;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class IdentityAuthenticationService implements AuthenticationService {

    private record Identity(String displayName, String credentialHash,
                            Set<Capability> capabilities) {
    }

    private final Map<String, Identity> identities = new ConcurrentHashMap<>();
    private final SecurityAuditLog auditLog;

    public IdentityAuthenticationService(SecurityAuditLog auditLog) {
        this.auditLog = auditLog;
        register("reader", "reader", "Development Reader",
                Set.of(Capability.READ));
        register("operator", "operator", "Development Operator",
                Set.of(Capability.READ, Capability.WRITE, Capability.MEMORIZE,
                        Capability.DELETE_RAW, Capability.UNDO_DELETE));
        register("custodian", "custodian", "Development Data Custodian",
                Set.of(Capability.READ, Capability.WRITE, Capability.MEMORIZE,
                        Capability.DELETE_RAW, Capability.UNDO_DELETE,
                        Capability.PURGE_RAW, Capability.RESOLVE_SECRET));
    }

    public void register(String id, String credential, String displayName,
                         Set<Capability> capabilities) {
        identities.put(id, new Identity(displayName,
                sha256(credential), Set.copyOf(capabilities)));
    }

    @Override
    public Optional<Principal> authenticate(String credential) {
        if (credential == null || credential.isBlank()) {
            return Optional.empty();
        }
        String candidate = sha256(credential.strip().toLowerCase(Locale.ROOT));
        for (Map.Entry<String, Identity> entry : identities.entrySet()) {
            if (MessageDigest.isEqual(
                    candidate.getBytes(StandardCharsets.UTF_8),
                    entry.getValue().credentialHash()
                            .getBytes(StandardCharsets.UTF_8))) {
                Identity identity = entry.getValue();
                return Optional.of(new Principal(entry.getKey(),
                        identity.displayName(), identity.capabilities()));
            }
        }
        auditLog.record("Authentication failed",
                "An unknown credential was refused.", false);
        return Optional.empty();
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
