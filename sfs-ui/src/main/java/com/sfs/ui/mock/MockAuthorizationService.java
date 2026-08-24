package com.sfs.ui.mock;

import com.sfs.contracts.security.AuthorizationService;
import com.sfs.contracts.security.Capability;
import com.sfs.contracts.security.Principal;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@Profile("mock")
public class MockAuthorizationService implements AuthorizationService {

    @Override
    public boolean isPermitted(Principal principal, Capability capability) {
        Objects.requireNonNull(principal, "principal must not be null");
        Objects.requireNonNull(capability, "capability must not be null");

        return principal.has(capability);
    }
}
