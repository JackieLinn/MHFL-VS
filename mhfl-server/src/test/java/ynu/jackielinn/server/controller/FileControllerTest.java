package ynu.jackielinn.server.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ynu.jackielinn.server.service.AccountService;
import ynu.jackielinn.server.service.FileService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = FileController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
class FileControllerTest extends ControllerTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FileService fileService;

    @MockitoBean
    private AccountService accountService;

    @Test
    void uploadAvatarShouldReturnUnauthorizedWhenUserMissing() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[]{1});

        mockMvc.perform(multipart("/api/file/avatar/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void uploadAvatarShouldReturnSuccess() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[]{1, 2});
        when(fileService.saveAvatar(eq(1L), any())).thenReturn("/uploads/avatar/a.png");
        when(accountService.updateAvatar(1L, "/uploads/avatar/a.png")).thenReturn(null);

        mockMvc.perform(multipart("/api/file/avatar/upload")
                        .file(file)
                        .requestAttr("id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("/uploads/avatar/a.png"));

        verify(fileService).saveAvatar(eq(1L), any());
        verify(accountService).updateAvatar(1L, "/uploads/avatar/a.png");
    }

    @Test
    void uploadAvatarShouldReturnBadRequestWhenAvatarUpdateFails() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[]{1, 2});
        when(fileService.saveAvatar(eq(1L), any())).thenReturn("/uploads/avatar/a.png");
        when(accountService.updateAvatar(1L, "/uploads/avatar/a.png")).thenReturn("update failed");

        mockMvc.perform(multipart("/api/file/avatar/upload")
                        .file(file)
                        .requestAttr("id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("update failed"));
    }

    @Test
    void uploadAvatarShouldReturnBadRequestWhenSaveThrows() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[]{1, 2});
        when(fileService.saveAvatar(eq(1L), any())).thenThrow(new IllegalArgumentException("invalid file"));

        mockMvc.perform(multipart("/api/file/avatar/upload")
                        .file(file)
                        .requestAttr("id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("invalid file"));
    }
}
