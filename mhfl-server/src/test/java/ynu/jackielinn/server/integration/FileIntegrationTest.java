package ynu.jackielinn.server.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import ynu.jackielinn.server.service.AccountService;
import ynu.jackielinn.server.service.FileService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 文件模块接口集成测试。
 * 覆盖 FileController，业务依赖全部 mock，不触发真实中间件/文件系统。
 */
class FileIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FileService fileService;

    @MockitoBean
    private AccountService accountService;

    /**
     * 成功场景：上传头像成功并返回 URL。
     */
    @Test
    void uploadAvatarShouldReturnSuccess() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                "fake-image-content".getBytes()
        );

        String avatarUrl = "http://localhost:8088/uploads/avatar/2_123456.png";
        when(fileService.saveAvatar(eq(2L), any())).thenReturn(avatarUrl);
        when(accountService.updateAvatar(2L, avatarUrl)).thenReturn(null);

        mockMvc.perform(multipart("/api/file/avatar/upload")
                        .file(file)
                        .requestAttr("id", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(avatarUrl));
    }

    /**
     * 失败场景：未登录时上传头像失败。
     */
    @Test
    void uploadAvatarShouldReturnFailureWhenUnauthorized() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                "fake-image-content".getBytes()
        );

        mockMvc.perform(multipart("/api/file/avatar/upload")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }
}
