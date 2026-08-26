package com.sfs.contracts.security;

import java.util.Optional;
public interface AuthenticationService {
    Optional<Principal> authenticate(String credential);
}
