package ynu.jackielinn.server.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import ynu.jackielinn.server.utils.Const;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 认证模块接口集成测试。
 * 覆盖 Controller -> Service -> Mapper -> H2 链路。
 */
@Sql(
        scripts = {
                "classpath:integration/authorize/schema.sql",
                "classpath:integration/authorize/data.sql"
        },
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
class AuthorizeIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUpRedisMock() {
        ValueOperations<String, String> valueOps = org.mockito.Mockito.mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(false);
    }

    /**
     * 成功场景：请求邮箱验证码成功。
     */
    @Test
    void askVerifyCodeShouldReturnSuccess() throws Exception {
        mockMvc.perform(get("/auth/ask-code")
                        .param("email", "new_user@example.com")
                        .param("type", "register")
                        .with(req -> {
                            req.setRemoteAddr("127.0.0.1");
                            return req;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * 失败场景：请求邮箱验证码过于频繁。
     */
    @Test
    void askVerifyCodeShouldReturnFailureWhenTooFrequent() throws Exception {
        when(stringRedisTemplate.hasKey(eq(Const.VERIFY_EMAIL_LIMIT + "127.0.0.1"))).thenReturn(true);

        mockMvc.perform(get("/auth/ask-code")
                        .param("email", "new_user@example.com")
                        .param("type", "register")
                        .with(req -> {
                            req.setRemoteAddr("127.0.0.1");
                            return req;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    /**
     * 成功场景：注册成功。
     */
    @Test
    void registerShouldReturnSuccess() throws Exception {
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOps = (ValueOperations<String, String>) stringRedisTemplate.opsForValue();
        when(valueOps.get(eq(Const.VERIFY_EMAIL_DATA + "new_user@example.com"))).thenReturn("123456");

        String body = """
                {
                  "telephone": "13900000999",
                  "email": "new_user@example.com",
                  "code": "123456",
                  "username": "newuser",
                  "password": "123456"
                }
                """;
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * 失败场景：注册时用户名重复。
     */
    @Test
    void registerShouldReturnFailureWhenUsernameDuplicate() throws Exception {
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOps = (ValueOperations<String, String>) stringRedisTemplate.opsForValue();
        when(valueOps.get(eq(Const.VERIFY_EMAIL_DATA + "new_user@example.com"))).thenReturn("123456");

        String body = """
                {
                  "telephone": "13900000999",
                  "email": "new_user@example.com",
                  "code": "123456",
                  "username": "user1",
                  "password": "123456"
                }
                """;
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    /**
     * 成功场景：重置确认成功。
     */
    @Test
    void resetConfirmShouldReturnSuccess() throws Exception {
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOps = (ValueOperations<String, String>) stringRedisTemplate.opsForValue();
        when(valueOps.get(eq(Const.VERIFY_EMAIL_DATA + "user1@example.com"))).thenReturn("123456");

        String body = """
                {
                  "email": "user1@example.com",
                  "code": "123456"
                }
                """;
        mockMvc.perform(post("/auth/reset-confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * 失败场景：重置确认验证码错误。
     */
    @Test
    void resetConfirmShouldReturnFailureWhenCodeInvalid() throws Exception {
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOps = (ValueOperations<String, String>) stringRedisTemplate.opsForValue();
        when(valueOps.get(eq(Const.VERIFY_EMAIL_DATA + "user1@example.com"))).thenReturn("999999");

        String body = """
                {
                  "email": "user1@example.com",
                  "code": "123456"
                }
                """;
        mockMvc.perform(post("/auth/reset-confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    /**
     * 成功场景：重置密码成功。
     */
    @Test
    void resetPasswordShouldReturnSuccess() throws Exception {
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOps = (ValueOperations<String, String>) stringRedisTemplate.opsForValue();
        when(valueOps.get(eq(Const.VERIFY_EMAIL_DATA + "user1@example.com"))).thenReturn("123456");

        String body = """
                {
                  "email": "user1@example.com",
                  "code": "123456",
                  "password": "654321"
                }
                """;
        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * 失败场景：重置密码验证码错误。
     */
    @Test
    void resetPasswordShouldReturnFailureWhenCodeInvalid() throws Exception {
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOps = (ValueOperations<String, String>) stringRedisTemplate.opsForValue();
        when(valueOps.get(eq(Const.VERIFY_EMAIL_DATA + "user1@example.com"))).thenReturn("999999");

        String body = """
                {
                  "email": "user1@example.com",
                  "code": "123456",
                  "password": "654321"
                }
                """;
        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }
}
