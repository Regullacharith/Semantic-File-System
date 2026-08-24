package com.sfs.app.service;

import com.sfs.app.api.response.SecuritySettingsResponse;
import com.sfs.contracts.security.SecuritySettingsService;

import java.util.Objects;

public class SecurityApplicationService {

    private final SecuritySettingsService securitySettingsService;

    public SecurityApplicationService(SecuritySettingsService securitySettingsService) {
        this.securitySettingsService = Objects.requireNonNull(
                securitySettingsService, "securitySettingsService must not be null");
    }

    public SecuritySettingsResponse getSettings() {
        return SecuritySettingsResponse.from(securitySettingsService.getSettings());
    }
}
