package com.sfs.ui.controller;

import com.sfs.ui.view.NavigationItem;
import com.sfs.ui.view.PageViewModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(HomeController.class)
@DisplayName("Dashboard route")
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("returns HTTP 200 and renders the home view")
    void rootReturnsHomeView() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(model().attributeExists("page"));
    }

    @Test
    @DisplayName("supplies a page view model marked active on the dashboard")
    void suppliesPageViewModelActiveOnHome() throws Exception {
        var result = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andReturn();

        Object attribute = result.getModelAndView() == null
                ? null
                : result.getModelAndView().getModel().get("page");

        org.assertj.core.api.Assertions.assertThat(attribute)
                .isInstanceOf(PageViewModel.class);

        PageViewModel page = (PageViewModel) attribute;
        org.assertj.core.api.Assertions.assertThat(page.activeItem())
                .isEqualTo(NavigationItem.HOME);
        org.assertj.core.api.Assertions.assertThat(page.navigation())
                .containsExactly(NavigationItem.values());
    }

    @Test
    @DisplayName("renders every navigation label in the shell")
    void rendersAllNavigationLabels() throws Exception {
        var actions = mockMvc.perform(get("/")).andExpect(status().isOk());

        for (NavigationItem item : NavigationItem.values()) {
            actions.andExpect(content().string(containsString(item.getLabel())));
        }
    }

    @Test
    @DisplayName("renders the dashboard as a link and planned screens as non-links")
    void plannedDestinationsAreNotLinks() throws Exception {
        var actions = mockMvc.perform(get("/")).andExpect(status().isOk());

        // The implemented destination is a real anchor.
        actions.andExpect(content().string(containsString("href=\"/\"")));

        for (NavigationItem item : NavigationItem.planned()) {
            actions.andExpect(content().string(
                    not(containsString("href=\"" + item.getPath() + "\""))));
        }
    }

    @Test
    @DisplayName("states that reconstruction is approximate, not byte-for-byte")
    void disclosesApproximateReconstruction() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("semantic and approximate")));
    }

    @Test
    @DisplayName("loads no external stylesheet, script or font")
    void loadsNoExternalResources() throws Exception {
        var actions = mockMvc.perform(get("/")).andExpect(status().isOk());

       
        actions.andExpect(content().string(not(containsString("http://"))));
        actions.andExpect(content().string(not(containsString("https://cdn"))));
        actions.andExpect(content().string(not(containsString("<script"))));
    }
}
