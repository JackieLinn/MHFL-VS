package ynu.jackielinn.server.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import ynu.jackielinn.server.utils.Const;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * CaptchaValidateFilter 单元测试：仅 POST /auth/login 校验、参数/Redis/失败次数分支。
 */
@ExtendWith(MockitoExtension.class)
class CaptchaValidateFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private CaptchaValidateFilter filter;

    @BeforeEach
    void setUp() {
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void shouldNotValidateWhenNotLoginRequest() throws Exception {
        when(request.getRequestURI()).thenReturn("/auth/other");
        // getMethod() 不会被调用：LOGIN_URL.equals("/auth/other") 为 false，&& 短路

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(stringRedisTemplate, never()).opsForValue();
    }

    @Test
    void shouldNotValidateWhenGetLogin() throws Exception {
        when(request.getRequestURI()).thenReturn("/auth/login");
        when(request.getMethod()).thenReturn("GET");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldWriteErrorWhenCaptchaIdOrCodeEmpty() throws Exception {
        when(request.getRequestURI()).thenReturn("/auth/login");
        when(request.getMethod()).thenReturn("POST");
        when(request.getParameter("captchaId")).thenReturn("");
        when(request.getParameter("captchaCode")).thenReturn("1234");

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertTrue(sw.toString().contains("验证码ID和验证码不能为空"));
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void shouldWriteErrorWhenSavedCodeNull() throws Exception {
        when(request.getRequestURI()).thenReturn("/auth/login");
        when(request.getMethod()).thenReturn("POST");
        when(request.getParameter("captchaId")).thenReturn("id1");
        when(request.getParameter("captchaCode")).thenReturn("1234");
        when(stringRedisTemplate.opsForValue().get(Const.VERIFY_CAPTCHA_DATA + "id1")).thenReturn(null);

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertTrue(sw.toString().contains("验证码已过期或无效"));
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void shouldWriteErrorAndIncrementFailWhenCodeMismatch() throws Exception {
        when(request.getRequestURI()).thenReturn("/auth/login");
        when(request.getMethod()).thenReturn("POST");
        when(request.getParameter("captchaId")).thenReturn("id1");
        when(request.getParameter("captchaCode")).thenReturn("wrong");
        when(stringRedisTemplate.opsForValue().get(Const.VERIFY_CAPTCHA_DATA + "id1")).thenReturn("RIGHT");
        when(stringRedisTemplate.opsForValue().increment(Const.VERIFY_CAPTCHA_FAIL_COUNT + "id1")).thenReturn(1L);
        when(stringRedisTemplate.expire(anyString(), eq(3L), eq(TimeUnit.MINUTES))).thenReturn(true);

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertTrue(sw.toString().contains("验证码错误"));
        assertTrue(sw.toString().contains("还可重试"));
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void shouldDeleteCaptchaAndWriteErrorWhenFailCountReachesMax() throws Exception {
        when(request.getRequestURI()).thenReturn("/auth/login");
        when(request.getMethod()).thenReturn("POST");
        when(request.getParameter("captchaId")).thenReturn("id1");
        when(request.getParameter("captchaCode")).thenReturn("wrong");
        when(stringRedisTemplate.opsForValue().get(Const.VERIFY_CAPTCHA_DATA + "id1")).thenReturn("RIGHT");
        when(stringRedisTemplate.opsForValue().increment(Const.VERIFY_CAPTCHA_FAIL_COUNT + "id1")).thenReturn(5L);

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        filter.doFilterInternal(request, response, filterChain);

        verify(stringRedisTemplate).delete(Const.VERIFY_CAPTCHA_DATA + "id1");
        verify(stringRedisTemplate).delete(Const.VERIFY_CAPTCHA_FAIL_COUNT + "id1");
        assertTrue(sw.toString().contains("验证码错误次数过多"));
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void shouldDeleteKeysAndDoFilterWhenCodeMatch() throws Exception {
        when(request.getRequestURI()).thenReturn("/auth/login");
        when(request.getMethod()).thenReturn("POST");
        when(request.getParameter("captchaId")).thenReturn("id1");
        when(request.getParameter("captchaCode")).thenReturn("AbC");
        when(stringRedisTemplate.opsForValue().get(Const.VERIFY_CAPTCHA_DATA + "id1")).thenReturn("abc");

        filter.doFilterInternal(request, response, filterChain);

        verify(stringRedisTemplate).delete(Const.VERIFY_CAPTCHA_DATA + "id1");
        verify(stringRedisTemplate).delete(Const.VERIFY_CAPTCHA_FAIL_COUNT + "id1");
        verify(filterChain).doFilter(request, response);
    }
}
