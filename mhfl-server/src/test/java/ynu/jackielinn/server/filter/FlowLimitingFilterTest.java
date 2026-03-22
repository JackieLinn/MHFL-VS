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
import org.springframework.data.redis.core.script.RedisScript;
import ynu.jackielinn.server.utils.Const;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * FlowLimitingFilter 单元测试：限流通过/拦截及 Redis 分支。
 * 使用 Lua 脚本后，核心逻辑为 execute(SCRIPT, keys) + block 检查。
 */
@ExtendWith(MockitoExtension.class)
class FlowLimitingFilterTest {

    private static final String IP = "192.168.1.1";

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain chain;

    @Mock
    private StringRedisTemplate template;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private FlowLimitingFilter filter;

    @BeforeEach
    void setUp() {
        lenient().when(template.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void shouldBlockWhenBlockKeyExists() throws Exception {
        when(request.getRemoteAddr()).thenReturn(IP);
        when(template.hasKey(Const.FLOW_LIMIT_BLOCK + IP)).thenReturn(true);

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        filter.doFilter(request, response, chain);

        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(response).setContentType("application/json;charset=utf-8");
        assertTrue(sw.toString().contains("操作频繁"));
        verify(chain, never()).doFilter(request, response);
        verify(template, never()).execute(any(RedisScript.class), anyList());
    }

    @Test
    void shouldPassWhenLuaReturns1() throws Exception {
        when(request.getRemoteAddr()).thenReturn(IP);
        when(template.hasKey(Const.FLOW_LIMIT_BLOCK + IP)).thenReturn(false);
        when(template.execute(any(RedisScript.class), eq(Collections.singletonList(Const.FLOW_LIMIT_COUNTER + IP))))
                .thenReturn(1L);

        filter.doFilter(request, response, chain);

        verify(template).execute(any(RedisScript.class), eq(Collections.singletonList(Const.FLOW_LIMIT_COUNTER + IP)));
        verify(chain).doFilter(request, response);
        verify(template, never()).opsForValue();
    }

    @Test
    void shouldPassWhenLuaReturns5() throws Exception {
        when(request.getRemoteAddr()).thenReturn(IP);
        when(template.hasKey(Const.FLOW_LIMIT_BLOCK + IP)).thenReturn(false);
        when(template.execute(any(RedisScript.class), eq(Collections.singletonList(Const.FLOW_LIMIT_COUNTER + IP))))
                .thenReturn(5L);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(template, never()).opsForValue();
    }

    @Test
    void shouldBlockWhenLuaReturns101() throws Exception {
        when(request.getRemoteAddr()).thenReturn(IP);
        when(template.hasKey(Const.FLOW_LIMIT_BLOCK + IP)).thenReturn(false);
        when(template.execute(any(RedisScript.class), eq(Collections.singletonList(Const.FLOW_LIMIT_COUNTER + IP))))
                .thenReturn(101L);

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        filter.doFilter(request, response, chain);

        verify(template.opsForValue()).set(eq(Const.FLOW_LIMIT_BLOCK + IP), eq(""));
        verify(template).expire(eq(Const.FLOW_LIMIT_BLOCK + IP), eq(10L), eq(TimeUnit.SECONDS));
        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        assertTrue(sw.toString().contains("操作频繁"));
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void shouldNormalizeIpv6Localhost0_0_0_0_0_0_0_1To127_0_0_1() throws Exception {
        when(request.getRemoteAddr()).thenReturn("0:0:0:0:0:0:0:1");
        when(template.hasKey(Const.FLOW_LIMIT_BLOCK + "127.0.0.1")).thenReturn(false);
        when(template.execute(any(RedisScript.class), eq(Collections.singletonList(Const.FLOW_LIMIT_COUNTER + "127.0.0.1"))))
                .thenReturn(1L);

        filter.doFilter(request, response, chain);

        verify(template).execute(any(RedisScript.class), eq(Collections.singletonList(Const.FLOW_LIMIT_COUNTER + "127.0.0.1")));
        verify(chain).doFilter(request, response);
    }

    @Test
    void shouldNormalizeIpv6LocalhostColon1To127_0_0_1() throws Exception {
        when(request.getRemoteAddr()).thenReturn("::1");
        when(template.hasKey(Const.FLOW_LIMIT_BLOCK + "127.0.0.1")).thenReturn(false);
        when(template.execute(any(RedisScript.class), eq(Collections.singletonList(Const.FLOW_LIMIT_COUNTER + "127.0.0.1"))))
                .thenReturn(1L);

        filter.doFilter(request, response, chain);

        verify(template).execute(any(RedisScript.class), eq(Collections.singletonList(Const.FLOW_LIMIT_COUNTER + "127.0.0.1")));
        verify(chain).doFilter(request, response);
    }

    @Test
    void shouldNormalizeNullAddressTo127_0_0_1() throws Exception {
        when(request.getRemoteAddr()).thenReturn(null);
        when(template.hasKey(Const.FLOW_LIMIT_BLOCK + "127.0.0.1")).thenReturn(false);
        when(template.execute(any(RedisScript.class), eq(Collections.singletonList(Const.FLOW_LIMIT_COUNTER + "127.0.0.1"))))
                .thenReturn(1L);

        filter.doFilter(request, response, chain);

        verify(template).execute(any(RedisScript.class), eq(Collections.singletonList(Const.FLOW_LIMIT_COUNTER + "127.0.0.1")));
        verify(chain).doFilter(request, response);
    }

    @Test
    void shouldPassWhenLuaReturnsNull() throws Exception {
        when(request.getRemoteAddr()).thenReturn(IP);
        when(template.hasKey(Const.FLOW_LIMIT_BLOCK + IP)).thenReturn(false);
        when(template.execute(any(RedisScript.class), eq(Collections.singletonList(Const.FLOW_LIMIT_COUNTER + IP))))
                .thenReturn(null);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }
}
