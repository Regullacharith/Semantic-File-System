package com.sfs.ui.controller;

import com.sfs.contracts.security.SecuritySettingsService;
import com.sfs.ui.view.NavigationItem;
import com.sfs.ui.view.PageViewModel;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Security and policy settings.
 */
@Controller
public class SettingsController {

    private static final String VIEW_SETTINGS = "settings";

    private static final String ATTR_PAGE = "page";
    private static final String ATTR_SETTINGS = "settings";

    private final SecuritySettingsService securitySettingsService;

    public SettingsController(SecuritySettingsService securitySettingsService) {
        this.securitySettingsService = securitySettingsService;
    }

    @GetMapping("/settings")
    public String showSettings(Model model) {
        model.addAttribute(ATTR_PAGE, PageViewModel.of("Settings", NavigationItem.SETTINGS));
        model.addAttribute(ATTR_SETTINGS, securitySettingsService.getSettings());

        return VIEW_SETTINGS;
    }
}
