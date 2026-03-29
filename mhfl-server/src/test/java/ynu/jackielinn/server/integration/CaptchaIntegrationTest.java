package ynu.jackielinn.server.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import ynu.jackielinn.server.utils.Const;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 图形验证码模块接口集成测试。
 * 覆盖 Controller -> Service/Utils -> Redis(Mock) 链路。
 */
@Sql(
        scripts = {
                "classpath:integration/captcha/schema.sql",
                "classpath:integration/captcha/data.sql"
        },
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
class CaptchaIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private ValueOperations<String, String> valueOperations;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        valueOperations = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(2L);
    }

    /**
     * 成功场景：生成 Base64 验证码成功。
     */
    @Test
    void generateCaptchaShouldReturnSuccess() throws Exception {
        when(valueOperations.get(eq(Const.VERIFY_CAPTCHA_LIMIT + "127.0.0.1"))).thenReturn(null);

        mockMvc.perform(get("/captcha/generate").with(request -> {
                    request.setRemoteAddr("127.0.0.1");
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.captchaId").isString())
                .andExpect(jsonPath("$.data.captchaImage").isString());
    }

    /**
     * 失败场景：请求过于频繁时返回 400 业务码。
     */
    @Test
    void generateCaptchaShouldReturnFailureWhenLimited() throws Exception {
        when(valueOperations.get(eq(Const.VERIFY_CAPTCHA_LIMIT + "127.0.0.1"))).thenReturn("3");

        mockMvc.perform(get("/captcha/generate").with(request -> {
                    request.setRemoteAddr("127.0.0.1");
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    /**
     * 成功场景：直接输出图片验证码成功。
     */
    @Test
    void getCaptchaImageShouldReturnSuccess() throws Exception {
        when(valueOperations.get(eq(Const.VERIFY_CAPTCHA_LIMIT + "127.0.0.1"))).thenReturn(null);

        mockMvc.perform(get("/captcha/image").with(request -> {
                    request.setRemoteAddr("127.0.0.1");
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(header().exists("Captcha-Id"))
                .andExpect(header().string("Cache-Control", "no-cache, no-store, must-revalidate"));
    }

    /**
     * 失败场景：图片验证码请求过于频繁返回 400。
     */
    @Test
    void getCaptchaImageShouldReturnFailureWhenLimited() throws Exception {
        when(valueOperations.get(eq(Const.VERIFY_CAPTCHA_LIMIT + "127.0.0.1"))).thenReturn("3");

        mockMvc.perform(get("/captcha/image").with(request -> {
                    request.setRemoteAddr("127.0.0.1");
                    return request;
                }))
                .andExpect(status().isBadRequest());
    }
}
