package ynu.jackielinn.server.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ynu.jackielinn.server.utils.Const;

import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = CaptchaController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
class CaptchaControllerTest extends ControllerTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    void setUp() {
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void generateCaptchaShouldReturnBadRequestWhenLimited() throws Exception {
        when(valueOperations.get(Const.VERIFY_CAPTCHA_LIMIT + "127.0.0.1")).thenReturn("3");

        mockMvc.perform(get("/captcha/generate").with(request -> {
                    request.setRemoteAddr("127.0.0.1");
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void generateCaptchaShouldReturnCaptchaDataWhenNotLimited() throws Exception {
        when(valueOperations.get(Const.VERIFY_CAPTCHA_LIMIT + "127.0.0.1")).thenReturn(null);

        mockMvc.perform(get("/captcha/generate").with(request -> {
                    request.setRemoteAddr("127.0.0.1");
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.captchaId").isString())
                .andExpect(jsonPath("$.data.captchaImage").isString());

        verify(valueOperations).set(eq(Const.VERIFY_CAPTCHA_LIMIT + "127.0.0.1"), eq("1"), eq(10L), eq(TimeUnit.SECONDS));
        verify(valueOperations).set(org.mockito.ArgumentMatchers.startsWith(Const.VERIFY_CAPTCHA_DATA), anyString(), eq(3L), eq(TimeUnit.MINUTES));
    }

    @Test
    void getCaptchaImageShouldReturnBadRequestWhenLimited() throws Exception {
        when(valueOperations.get(Const.VERIFY_CAPTCHA_LIMIT + "127.0.0.1")).thenReturn("3");

        mockMvc.perform(get("/captcha/image").with(request -> {
                    request.setRemoteAddr("127.0.0.1");
                    return request;
                }))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCaptchaImageShouldReturnImageHeadersWhenNotLimited() throws Exception {
        when(valueOperations.get(Const.VERIFY_CAPTCHA_LIMIT + "127.0.0.1")).thenReturn(null);

        mockMvc.perform(get("/captcha/image").with(request -> {
                    request.setRemoteAddr("127.0.0.1");
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-cache, no-store, must-revalidate"))
                .andExpect(header().exists("Captcha-Id"));
    }
}
