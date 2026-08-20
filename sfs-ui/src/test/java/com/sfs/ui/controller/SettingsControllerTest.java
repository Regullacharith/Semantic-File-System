package com.sfs.ui.controller;

import com.sfs.contracts.security.HandlingPolicy;
import com.sfs.contracts.security.SecuritySettingsService;
import com.sfs.contracts.security.SecuritySettingsView;
import com.sfs.contracts.security.SecuritySettingsView.AuditEventView;
import com.sfs.contracts.security.SensitiveTypePolicy;
import com.sfs.contracts.semantic.ProtectedReferenceView.SensitiveType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Verifies the security and policy settings view.
 */
@WebMvcTest(SettingsController.class)
@DisplayName("Security settings view")
class SettingsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SecuritySettingsService securitySettingsService;

    private static SecuritySettingsView settings() {
        return new SecuritySettingsView(
                List.of(
                        new SensitiveTypePolicy(SensitiveType.PASSWORD, HandlingPolicy.REDACT,
                                true, "Passwords are never retained in recoverable form."),
                        new SensitiveTypePolicy(SensitiveType.API_KEY, HandlingPolicy.ENCRYPT,
                                false, "Held in the encrypted secure store.")),
                true, true, true, true,
                "Keys are held separately from the ciphertext they protect.",
                List.of(new AuditEventView("2026-08-19 09:31", "Reference resolution denied",
                        "Attempt to resolve ref-2b8e4d05 refused.", false)));
    }

    @Test
    @DisplayName("renders the settings view")
    void rendersSettings() throws Exception {
        given(securitySettingsService.getSettings()).willReturn(settings());

        mockMvc.perform(get("/settings"))
                .andExpect(status().isOk())
                .andExpect(view().name("settings"));
    }

    @Test
    @DisplayName("reports the mandatory protections as enforced")
    void reportsEnforcedGuarantees() throws Exception {
        given(securitySettingsService.getSettings()).willReturn(settings());

        mockMvc.perform(get("/settings"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("never generated from plaintext secrets")))
                .andExpect(content().string(containsString("Logs never contain plaintext secrets")))
                .andExpect(content().string(containsString("always requires authorization")));
    }

    @Test
    @DisplayName("shows each sensitive type with its policy and rationale")
    void showsPoliciesWithRationale() throws Exception {
        given(securitySettingsService.getSettings()).willReturn(settings());

        mockMvc.perform(get("/settings"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Password")))
                .andExpect(content().string(containsString("API key")))
                .andExpect(content().string(containsString("Encrypt and store")))
                .andExpect(content().string(containsString("never retained in recoverable form")));
    }

    @Test
    @DisplayName("marks the password policy as locked and never recoverable")
    void marksPasswordLocked() throws Exception {
        given(securitySettingsService.getSettings()).willReturn(settings());

        mockMvc.perform(get("/settings"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("locked")))
                .andExpect(content().string(containsString(
                        "Passwords are fixed to a non-reversible policy")));
    }

    @Test
    @DisplayName("shows audit events describing actions rather than values")
    void showsAuditEvents() throws Exception {
        given(securitySettingsService.getSettings()).willReturn(settings());

        mockMvc.perform(get("/settings"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Reference resolution denied")))
                .andExpect(content().string(containsString("Refused")))
                .andExpect(content().string(containsString("never contain the value itself")));
    }

    @Test
    @DisplayName("offers no edit control while enforcement does not exist")
    void offersNoEditControl() throws Exception {
        given(securitySettingsService.getSettings()).willReturn(settings());

        mockMvc.perform(get("/settings"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("method=\"post\""))))
                .andExpect(content().string(not(containsString("<input"))))
                .andExpect(content().string(not(containsString("<select"))))
                .andExpect(content().string(containsString("This screen is read-only")));
    }

    @Test
    @DisplayName("rejects a POST to the settings route")
    void rejectsPost() throws Exception {
        mockMvc.perform(post("/settings"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("states that no security subsystem is implemented yet")
    void disclosesMockState() throws Exception {
        given(securitySettingsService.getSettings()).willReturn(settings());

        mockMvc.perform(get("/settings"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No detector, policy engine")));
    }

    @Test
    @DisplayName("displays no secret-shaped material")
    void displaysNoSecrets() throws Exception {
        given(securitySettingsService.getSettings()).willReturn(settings());

        mockMvc.perform(get("/settings"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("sk-"))))
                .andExpect(content().string(not(containsString("Bearer "))))
                .andExpect(content().string(not(containsString("AKIA"))));
    }
}
