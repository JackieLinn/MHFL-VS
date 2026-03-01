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
 * FlowLimitingFilter 单元测试：限流通过/拦截及 Redis 分支。
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
    }

    @Test
    void shouldPassWhenNoCounterKeyAndSetCounter() throws Exception {
        when(request.getRemoteAddr()).thenReturn(IP);
        when(template.hasKey(Const.FLOW_LIMIT_BLOCK + IP)).thenReturn(false);
        when(template.hasKey(Const.FLOW_LIMIT_COUNTER + IP)).thenReturn(false);

        filter.doFilter(request, response, chain);

        verify(valueOperations).set(eq(Const.FLOW_LIMIT_COUNTER + IP), eq("1"), eq(10L), eq(TimeUnit.SECONDS));
        verify(chain).doFilter(request, response);
    }

    @Test
    void shouldPassWhenCounterIncrementNotExceedLimit() throws Exception {
        when(request.getRemoteAddr()).thenReturn(IP);
        when(template.hasKey(Const.FLOW_LIMIT_BLOCK + IP)).thenReturn(false);
        when(template.hasKey(Const.FLOW_LIMIT_COUNTER + IP)).thenReturn(true);
        when(valueOperations.increment(Const.FLOW_LIMIT_COUNTER + IP)).thenReturn(5L);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(valueOperations, never()).set(eq(Const.FLOW_LIMIT_BLOCK + IP), anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void shouldBlockWhenCounterIncrementExceeds20() throws Exception {
        when(request.getRemoteAddr()).thenReturn(IP);
        when(template.hasKey(Const.FLOW_LIMIT_BLOCK + IP)).thenReturn(false);
        when(template.hasKey(Const.FLOW_LIMIT_COUNTER + IP)).thenReturn(true);
        when(valueOperations.increment(Const.FLOW_LIMIT_COUNTER + IP)).thenReturn(21L);

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        filter.doFilter(request, response, chain);

        verify(valueOperations).set(eq(Const.FLOW_LIMIT_BLOCK + IP), eq(""), eq(10L), eq(TimeUnit.SECONDS));
        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        assertTrue(sw.toString().contains("操作频繁"));
        verify(chain, never()).doFilter(request, response);
    }
}
