package com.sfs.ui.mock;

import com.sfs.contracts.security.HandlingPolicy;
import com.sfs.contracts.security.SecuritySettingsService;
import com.sfs.contracts.security.SecuritySettingsView;
import com.sfs.contracts.security.SecuritySettingsView.AuditEventView;
import com.sfs.contracts.security.SensitiveTypePolicy;
import com.sfs.contracts.semantic.ProtectedReferenceView.SensitiveType;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * In-memory stand-in for the Security and Privacy subsystem.
 */
@Service
@Profile("mock")
public class MockSecuritySettingsService implements SecuritySettingsService {

    @Override
    public SecuritySettingsView getSettings() {
        return new SecuritySettingsView(
                policies(),
                true,   // embeddings exclude secrets
                true,   // logs exclude secrets
                true,   // Semantic DNA excludes secrets
                true,   // authorization required to resolve a protected reference
                "Encryption keys are held separately from the ciphertext they protect and are "
                        + "never stored in the Memory Database.",
                auditEvents());
    }

    private static List<SensitiveTypePolicy> policies() {
        return List.of(
                new SensitiveTypePolicy(SensitiveType.PASSWORD, HandlingPolicy.REDACT, true,
                        "Passwords are never retained in recoverable form. A password's exact "
                                + "value has no legitimate reconstruction use, so storing it "
                                + "reversibly would add risk without benefit."),

                new SensitiveTypePolicy(SensitiveType.API_KEY, HandlingPolicy.ENCRYPT, false,
                        "An exact API key may need to be recovered by an authorized operator, "
                                + "so it is held in the encrypted secure store behind a "
                                + "protected reference."),

                new SensitiveTypePolicy(SensitiveType.ACCESS_TOKEN, HandlingPolicy.TOKENIZE, false,
                        "Tokens are short-lived, so recovering an expired value has little "
                                + "value and retaining it carries real risk."),

                new SensitiveTypePolicy(SensitiveType.EMAIL_ADDRESS, HandlingPolicy.TOKENIZE, false,
                        "Tokenizing keeps repeated mentions of the same person consistent "
                                + "across a document without retaining the address itself."),

                new SensitiveTypePolicy(SensitiveType.PHONE_NUMBER, HandlingPolicy.TOKENIZE, false,
                        "Preserves the semantic role of a contact number without retaining it."),

                new SensitiveTypePolicy(SensitiveType.POSTAL_ADDRESS, HandlingPolicy.REDACT, false,
                        "Replaced with a placeholder describing its role in the document."),

                new SensitiveTypePolicy(SensitiveType.ACCOUNT_IDENTIFIER, HandlingPolicy.TOKENIZE, false,
                        "Account identifiers are high-entropy and cannot be semantically "
                                + "inferred, so a consistent token preserves structure without "
                                + "retaining the value."),

                new SensitiveTypePolicy(SensitiveType.OTHER, HandlingPolicy.REDACT, true,
                        "Anything detected as sensitive but not classified is redacted. "
                                + "Unclassified data fails closed."));
    }

    private static List<AuditEventView> auditEvents() {
        return List.of(
                new AuditEventView("2026-08-19 09:14", "Sensitive value detected",
                        "API key detected in deployment-config.txt; stored as ref-7f3a9c21.", true),
                new AuditEventView("2026-08-19 09:14", "Sensitive value detected",
                        "Password detected in deployment-config.txt; redacted as ref-2b8e4d05.", true),
                new AuditEventView("2026-08-19 09:31", "Reference resolution denied",
                        "Attempt to resolve ref-2b8e4d05 refused: password policy is "
                                + "non-reversible.", false),
                new AuditEventView("2026-08-19 10:02", "Reconstruction refused",
                        "Reconstruction of sfs-obj-0004-b3c4d5e6 rejected: protected values "
                                + "cannot be reproduced.", false));
    }
}
