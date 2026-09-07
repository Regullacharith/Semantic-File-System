package com.sfs.ui.controller;

import com.sfs.contracts.file.FileOperationResult;
import com.sfs.contracts.file.FileService;
import com.sfs.contracts.security.Principal;
import com.sfs.contracts.file.FileStatus;
import com.sfs.contracts.file.FileSummary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(FileController.class)
@DisplayName("File controls")
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FileService fileService;

    private static FileSummary summary(String id, String name, FileStatus status) {
        return new FileSummary(id, name, status, 1_024, Instant.now(),
                status == FileStatus.REGISTERED ? null : Instant.now());
    }

    // ------------------------------------------------------------------ list

    @Test
    @DisplayName("renders the file list")
    void rendersFileList() throws Exception {
        given(fileService.listFiles()).willReturn(List.of(
                summary("sfs-obj-0001", "notes.txt", FileStatus.REGISTERED),
                summary("sfs-obj-0002", "gone.txt", FileStatus.MEMORIZED)));

        mockMvc.perform(get("/files"))
                .andExpect(status().isOk())
                .andExpect(view().name("files"))
                .andExpect(content().string(containsString("notes.txt")))
                .andExpect(content().string(containsString("sfs-obj-0001")))
                .andExpect(content().string(containsString("gone.txt")));
    }

    @Test
    @DisplayName("shows an empty state when nothing is registered")
    void showsEmptyState() throws Exception {
        given(fileService.listFiles()).willReturn(List.of());

        mockMvc.perform(get("/files"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No files have been imported yet")));
    }

    @Test
    @DisplayName("distinguishes memorized records from live files")
    void distinguishesMemorizedRecords() throws Exception {
        given(fileService.listFiles())
                .willReturn(List.of(summary("sfs-obj-0002", "gone.txt", FileStatus.MEMORIZED)));

        mockMvc.perform(get("/files"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("row--memorized")))
                .andExpect(content().string(containsString("Semantic Record retained")));
    }

    @Test
    @DisplayName("offers deletion only when the service permits it")
    void offersDeletionOnlyWhenPermitted() throws Exception {
        given(fileService.listFiles())
                .willReturn(List.of(summary("sfs-obj-0001", "fresh.txt", FileStatus.REGISTERED)));

        mockMvc.perform(get("/files"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("/purge"))))
                .andExpect(content().string(containsString("analyze")));
    }

    // ---------------------------------------------------------------- import

    @Test
    @DisplayName("imports a valid UTF-8 text file")
    void importsValidTextFile() throws Exception {
        given(fileService.importFile(any()))
                .willReturn(FileOperationResult.success("sfs-obj-0009", "Imported."));

        var upload = new MockMultipartFile("file", "notes.txt", "text/plain",
                "Semantic content.".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/files/import").file(upload))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/files"))
                .andExpect(flash().attribute("resultSuccess", true));

        verify(fileService).importFile(any());
    }

    @Test
    @DisplayName("rejects an empty upload without calling the service")
    void rejectsEmptyUpload() throws Exception {
        var empty = new MockMultipartFile("file", "", "text/plain", new byte[0]);

        mockMvc.perform(multipart("/files/import").file(empty))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("resultSuccess", false))
                .andExpect(flash().attribute("resultMessage",
                        containsString("No file was selected")));

        verify(fileService, never()).importFile(any());
    }

    @Test
    @DisplayName("rejects non-UTF-8 content rather than misclassifying it as text")
    void rejectsNonUtf8Content() throws Exception {
        var binary = new MockMultipartFile("file", "image.txt", "text/plain",
                new byte[]{(byte) 0xFF, (byte) 0xFE, (byte) 0xFF, (byte) 0xFE});

        mockMvc.perform(multipart("/files/import").file(binary))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("resultSuccess", false))
                .andExpect(flash().attribute("resultMessage",
                        containsString("not valid UTF-8")));

        verify(fileService, never()).importFile(any());
    }

    @Test
    @DisplayName("rejects a file name containing a path separator")
    void rejectsPathTraversalFileName() throws Exception {
        var traversal = new MockMultipartFile("file", "../../etc/passwd", "text/plain",
                "x".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/files/import").file(traversal))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("resultSuccess", false))
                .andExpect(flash().attribute("resultMessage",
                        containsString("Import rejected")));

        verify(fileService, never()).importFile(any());
    }

    // --------------------------------------------------------------- actions

    @Test
    @DisplayName("delegates an analysis request and reports the outcome")
    void delegatesAnalysisRequest() throws Exception {
        given(fileService.requestAnalysis("sfs-obj-0001"))
                .willReturn(FileOperationResult.success("sfs-obj-0001", "Analysis completed."));

        mockMvc.perform(post("/files/sfs-obj-0001/analyze"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/files"))
                .andExpect(flash().attribute("resultSuccess", true));

        verify(fileService).requestAnalysis("sfs-obj-0001");
    }

    @Test
    @DisplayName("surfaces a refused semantic deletion as an explicit failure")
    void surfacesRefusedDeletion() throws Exception {
        given(fileService.softDelete(eq("sfs-obj-0001"), any(Principal.class)))
                .willReturn(FileOperationResult.failure(
                        "Deletion is refused: the file must be analyzed first."));

        mockMvc.perform(post("/files/sfs-obj-0001/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("resultSuccess", false))
                .andExpect(flash().attribute("resultMessage", containsString("refused")));
    }

    @Test
    @DisplayName("does not expose mutating actions over GET")
    void mutatingActionsRejectGet() throws Exception {
        mockMvc.perform(get("/files/sfs-obj-0001/analyze"))
                .andExpect(status().is4xxClientError());

        mockMvc.perform(get("/files/sfs-obj-0001/delete"))
                .andExpect(status().is4xxClientError());

        mockMvc.perform(get("/files/sfs-obj-0001/undo-delete"))
                .andExpect(status().is4xxClientError());

        verify(fileService, never()).requestAnalysis(any());
        verify(fileService, never()).softDelete(any(), any());
        verify(fileService, never()).purgeRawData(any(), any());
    }

    @Test
    @DisplayName("finds a file by Object ID through the service")
    void findsByObjectId() {
        given(fileService.findByObjectId("sfs-obj-0001"))
                .willReturn(Optional.of(summary("sfs-obj-0001", "notes.txt", FileStatus.ANALYZED)));

        org.assertj.core.api.Assertions
                .assertThat(fileService.findByObjectId("sfs-obj-0001"))
                .isPresent();
    }
}
