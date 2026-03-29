package ynu.jackielinn.server.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import ynu.jackielinn.server.service.AccountService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 账户模块接口集成测试。
 * 覆盖 Controller -> Service -> Mapper -> H2 链路。
 */
@Sql(
        scripts = {
                "classpath:integration/account/schema.sql",
                "classpath:integration/account/data.sql"
        },
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
class AccountIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountService accountService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    /**
     * 成功场景：管理员创建用户成功。
     */
    @Test
    void createAccountShouldReturnSuccess() throws Exception {
        String body = """
                {
                  "username": "new_user",
                  "email": "new_user@example.com",
                  "telephone": "13900000099"
                }
                """;

        mockMvc.perform(post("/api/account/admin/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        assertThat(accountService.findAccountByUsernameOrEmail("new_user")).isNotNull();
    }

    /**
     * 失败场景：创建用户时用户名重复。
     */
    @Test
    void createAccountShouldReturnFailureWhenUsernameDuplicate() throws Exception {
        String body = """
                {
                  "username": "user1",
                  "email": "another@example.com",
                  "telephone": "13900000123"
                }
                """;

        mockMvc.perform(post("/api/account/admin/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    /**
     * 成功场景：管理员删除其他用户成功。
     */
    @Test
    void deleteAccountShouldReturnSuccess() throws Exception {
        setAdminAuthentication();

        mockMvc.perform(delete("/api/account/admin/2")
                        .requestAttr("id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        assertThat(accountService.getById(2L)).isNull();
    }

    /**
     * 失败场景：管理员删除自己失败。
     */
    @Test
    void deleteAccountShouldReturnFailureWhenDeleteSelf() throws Exception {
        setAdminAuthentication();

        mockMvc.perform(delete("/api/account/admin/1")
                        .requestAttr("id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    /**
     * 成功场景：用户更新自己的资料成功。
     */
    @Test
    void updateAccountShouldReturnSuccess() throws Exception {
        String body = """
                {
                  "username": "user1_new"
                }
                """;

        mockMvc.perform(put("/api/account/update")
                        .requestAttr("id", 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        assertThat(accountService.getById(2L).getUsername()).isEqualTo("user1_new");
    }

    /**
     * 失败场景：用户更新资料时用户名与他人重复。
     */
    @Test
    void updateAccountShouldReturnFailureWhenUsernameDuplicate() throws Exception {
        String body = """
                {
                  "username": "admin"
                }
                """;

        mockMvc.perform(put("/api/account/update")
                        .requestAttr("id", 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    /**
     * 成功场景：查询用户分页列表成功。
     */
    @Test
    void listAccountsShouldReturnSuccess() throws Exception {
        mockMvc.perform(get("/api/account/admin/list")
                        .param("current", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.total").value(3));
    }

    /**
     * 失败场景：分页参数非法时返回 4xx。
     */
    @Test
    void listAccountsShouldReturnFailureWhenPageParamInvalid() throws Exception {
        mockMvc.perform(get("/api/account/admin/list")
                        .param("current", "abc")
                        .param("size", "10"))
                .andExpect(status().is4xxClientError());
    }

    /**
     * 成功场景：查询当前用户信息成功。
     */
    @Test
    void getAccountInfoShouldReturnSuccess() throws Exception {
        mockMvc.perform(get("/api/account/info")
                        .requestAttr("id", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(2))
                .andExpect(jsonPath("$.data.username").value("user1"));
    }

    /**
     * 失败场景：未登录查询当前用户信息失败。
     */
    @Test
    void getAccountInfoShouldReturnFailureWhenNotLogin() throws Exception {
        mockMvc.perform(get("/api/account/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    /**
     * 设置管理员鉴权上下文。
     */
    private void setAdminAuthentication() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "admin",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_admin"))
                )
        );
    }
}
