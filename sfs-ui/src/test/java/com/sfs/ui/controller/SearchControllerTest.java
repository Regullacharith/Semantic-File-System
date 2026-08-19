package com.sfs.ui.controller;

import com.sfs.contracts.file.FileStatus;
import com.sfs.contracts.search.SearchEvidence;
import com.sfs.contracts.search.SearchQuery;
import com.sfs.contracts.search.SearchResponse;
import com.sfs.contracts.search.SearchResult;
import com.sfs.contracts.search.SearchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Verifies the search view
*/
@WebMvcTest(SearchController.class)
@DisplayName("Semantic search view")
class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SearchService searchService;

    private static SearchResponse responseWith(SearchResult... results) {
        return new SearchResponse("test query", List.of(results),
                SearchResponse.RetrievalMode.SEMANTIC, 12);
    }

    private static SearchResult result(String id, String name, FileStatus status) {
        return new SearchResult(id, name, status, 0.85, "A summary of the record.",
                List.of(new SearchEvidence(SearchEvidence.EvidenceType.CONCEPT,
                        "Concept: database performance")));
    }

    @Test
    @DisplayName("renders the search form without running a search on first visit")
    void rendersFormWithoutSearching() throws Exception {
        mockMvc.perform(get("/search"))
                .andExpect(status().isOk())
                .andExpect(view().name("search"))
                .andExpect(model().attribute("searched", false));

        verify(searchService, never()).search(any());
    }

    @Test
    @DisplayName("ignores a blank query rather than searching for nothing")
    void ignoresBlankQuery() throws Exception {
        mockMvc.perform(get("/search").param("q", "   "))
                .andExpect(status().isOk())
                .andExpect(model().attribute("searched", false));

        verify(searchService, never()).search(any());
    }

    @Test
    @DisplayName("executes a supplied query and renders the results")
    void executesQueryAndRendersResults() throws Exception {
        given(searchService.search(any()))
                .willReturn(responseWith(result("sfs-obj-0001-aaaa", "notes.txt", FileStatus.ANALYZED)));

        mockMvc.perform(get("/search").param("q", "database performance"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("searched", true))
                .andExpect(content().string(containsString("notes.txt")))
                .andExpect(content().string(containsString("sfs-obj-0001-aaaa")));

        verify(searchService).search(any(SearchQuery.class));
    }

    @Test
    @DisplayName("renders match evidence, not just a score")
    void rendersEvidence() throws Exception {
        given(searchService.search(any()))
                .willReturn(responseWith(result("sfs-obj-0001-aaaa", "notes.txt", FileStatus.ANALYZED)));

        mockMvc.perform(get("/search").param("q", "database"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Why this matched")))
                .andExpect(content().string(containsString("Concept: database performance")));
    }

    @Test
    @DisplayName("reports the retrieval mode and result count")
    void reportsRetrievalMode() throws Exception {
        given(searchService.search(any())).willReturn(new SearchResponse(
                "sfs-obj-0001-aaaa",
                List.of(result("sfs-obj-0001-aaaa", "notes.txt", FileStatus.ANALYZED)),
                SearchResponse.RetrievalMode.OBJECT_ID_LOOKUP, 3));

        mockMvc.perform(get("/search").param("q", "sfs-obj-0001-aaaa"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Direct Object ID lookup")));
    }

    @Test
    @DisplayName("distinguishes a memorized record from a live file")
    void distinguishesMemorizedRecord() throws Exception {
        given(searchService.search(any()))
                .willReturn(responseWith(result("sfs-obj-0002-bbbb", "gone.txt", FileStatus.MEMORIZED)));

        mockMvc.perform(get("/search").param("q", "archived"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("result--memorized")))
                .andExpect(content().string(containsString("Raw file deleted")));
    }

    @Test
    @DisplayName("shows an explicit empty state when nothing matches")
    void showsEmptyState() throws Exception {
        given(searchService.search(any())).willReturn(
                new SearchResponse("nothing", List.of(), SearchResponse.RetrievalMode.SEMANTIC, 1));

        mockMvc.perform(get("/search").param("q", "nothing"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No semantic records matched")));
    }

    @Test
    @DisplayName("reports an over-long query as an explicit error")
    void reportsRejectedQuery() throws Exception {
        String tooLong = "x".repeat(SearchQuery.MAX_QUERY_LENGTH + 1);

        mockMvc.perform(get("/search").param("q", tooLong))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("error"))
                .andExpect(content().string(containsString("Query rejected")));

        verify(searchService, never()).search(any());
    }

    @Test
    @DisplayName("offers no reconstruction control on the search view")
    void offersNoReconstructionControl() throws Exception {
        given(searchService.search(any()))
                .willReturn(responseWith(result("sfs-obj-0001-aaaa", "notes.txt", FileStatus.ANALYZED)));
               mockMvc.perform(get("/search").param("q", "database"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("method=\"post\""))))
                .andExpect(content().string(not(containsString(
                        "action=\"/reconstruction/sfs-obj-0001-aaaa\""))));
    }

    @Test
    @DisplayName("escapes HTML in a query so it cannot inject markup")
    void escapesQueryInOutput() throws Exception {
        given(searchService.search(any())).willReturn(
                new SearchResponse("x", List.of(), SearchResponse.RetrievalMode.SEMANTIC, 1));

        mockMvc.perform(get("/search").param("q", "<script>alert(1)</script>"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("<script>alert(1)</script>"))));
    }
}
