package com.sfs.contracts.security;

/**
 * Application-facing contract for security and privacy configuration.
 */
public interface SecuritySettingsService {

    SecuritySettingsView getSettings();
}
