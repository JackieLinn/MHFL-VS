package ynu.jackielinn.server.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import ynu.jackielinn.server.service.AccountService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 文件模块接口集成测试。
 * 覆盖 Controller -> Service -> Mapper -> H2 链路。
 */
@Sql(
        scripts = {
                "classpath:integration/file/schema.sql",
                "classpath:integration/file/data.sql"
        },
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@TestPropertySource(properties = "web.upload.base-dir=target/test-uploads")
class FileIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountService accountService;

    /**
     * 成功场景：上传头像成功并更新账号头像字段。
     */
    @Test
    void uploadAvatarShouldReturnSuccess() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                "fake-image-content".getBytes()
        );

        mockMvc.perform(multipart("/api/file/avatar/upload")
                        .file(file)
                        .requestAttr("id", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.containsString("/uploads/avatar/")));

        String avatar = accountService.getById(2L).getAvatar();
        assertThat(avatar).isNotBlank();
        assertThat(avatar).contains("/uploads/avatar/");
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
