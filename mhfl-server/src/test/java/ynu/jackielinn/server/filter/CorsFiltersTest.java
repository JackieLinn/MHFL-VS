package ynu.jackielinn.server.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

/**
 * CorsFilters 单元测试：CORS 头设置与链放行。
 */
@ExtendWith(MockitoExtension.class)
class CorsFiltersTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain chain;

    private CorsFilters filter;

    @BeforeEach
    void setUp() {
        filter = new CorsFilters();
    }

    @Test
    void shouldAddCorsHeadersAndCallChain() throws Exception {
        when(request.getHeader("Origin")).thenReturn("http://localhost:3000");

        filter.doFilter(request, response, chain);

        verify(response).addHeader("Access-Control-Allow-Origin", "http://localhost:3000");
        verify(response).addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS, DELETE, PUT");
        verify(response).addHeader("Access-Control-Allow-Headers", "Authorization, Content-Type");
        verify(chain).doFilter(request, response);
    }

    @Test
    void shouldPassOriginToAllowOriginHeader() throws Exception {
        when(request.getHeader("Origin")).thenReturn("https://example.com");

        filter.doFilter(request, response, chain);

        verify(response).addHeader("Access-Control-Allow-Origin", "https://example.com");
    }

    @Test
    void shouldCallChainExactlyOnce() throws Exception {
        when(request.getHeader("Origin")).thenReturn("http://localhost");

        filter.doFilter(request, response, chain);

        verify(chain, times(1)).doFilter(request, response);
    }

    @Test
    void shouldSetSseHeadersWhenStreamEndpoint() throws Exception {
        when(request.getHeader("Origin")).thenReturn("http://localhost");
        when(request.getRequestURI()).thenReturn("/api/assistant/chat/stream");

        filter.doFilter(request, response, chain);

        verify(response).setHeader("Cache-Control", "no-cache");
        verify(response).setHeader("Connection", "keep-alive");
        verify(response).setHeader("X-Accel-Buffering", "no");
        verify(chain).doFilter(request, response);
    }

    @Test
    void shouldNotSetSseHeadersWhenUriIsNull() throws Exception {
        when(request.getHeader("Origin")).thenReturn("http://localhost");
        when(request.getRequestURI()).thenReturn(null);

        filter.doFilter(request, response, chain);

        verify(response, never()).setHeader(eq("Cache-Control"), anyString());
        verify(response, never()).setHeader(eq("Connection"), anyString());
        verify(response, never()).setHeader(eq("X-Accel-Buffering"), anyString());
        verify(chain).doFilter(request, response);
    }
}
