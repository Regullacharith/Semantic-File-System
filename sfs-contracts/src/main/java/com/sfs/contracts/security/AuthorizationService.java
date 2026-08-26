package com.sfs.contracts.security;

public interface AuthorizationService {

    boolean isPermitted(Principal principal, Capability capability);
}
