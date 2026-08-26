package com.sfs.ui.mock;

import com.sfs.contracts.security.AuthenticationService;
import com.sfs.contracts.security.Capability;
import com.sfs.contracts.security.Principal;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@Profile("mock")
public class MockAuthenticationService implements AuthenticationService {

    private static final Map<String, Principal> IDENTITIES = Map.of(
            "reader", new Principal("dev-reader", "Development Reader",
                    Set.of(Capability.READ)),
            "operator", new Principal("dev-operator", "Development Operator",
                    Set.of(Capability.READ, Capability.WRITE,
                            Capability.DELETE_RAW, Capability.UNDO_DELETE)),
            "custodian", new Principal("dev-custodian", "Development Data Custodian",
                    Set.of(Capability.READ, Capability.WRITE, Capability.DELETE_RAW,
                            Capability.UNDO_DELETE, Capability.PURGE_RAW)));

    @Override
    public Optional<Principal> authenticate(String credential) {
        if (credential == null || credential.isBlank()) {
            return Optional.empty();
        }

        return Optional.ofNullable(
                IDENTITIES.get(credential.strip().toLowerCase(Locale.ROOT)));
    }
}
