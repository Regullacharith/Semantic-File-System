package com.sfs.ui.controller;

import com.sfs.ui.view.NavigationItem;
import com.sfs.ui.view.PageViewModel;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private static final String VIEW_HOME = "home";
    private static final String MODEL_ATTRIBUTE_PAGE = "page";

    /**
     * Renders the dashboard.
     *
     * @param model Spring MVC model populated with the page view model
     * @return the logical view name resolved to {@code templates/home.html}
     */
    
    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute(
                MODEL_ATTRIBUTE_PAGE,
                PageViewModel.of("Dashboard", NavigationItem.HOME));

        return VIEW_HOME;
    }
}