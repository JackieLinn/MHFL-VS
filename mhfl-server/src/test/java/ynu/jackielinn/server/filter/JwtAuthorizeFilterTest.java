package ynu.jackielinn.server.filter;

import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import ynu.jackielinn.server.utils.JwtUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * JwtAuthorizeFilter 单元测试：无 JWT 放行、有 JWT 时设置认证与 request 属性并放行。
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthorizeFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private JwtUtils utils;

    @Mock
    private DecodedJWT decodedJwt;

    @InjectMocks
    private JwtAuthorizeFilter filter;

    @BeforeEach
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldDoFilterWhenNoAuthorizationHeader() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);
        when(utils.resolveJwt(null)).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(utils).resolveJwt(null);
        verify(utils, never()).toUser(any());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldDoFilterWhenJwtInvalid() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid");
        when(utils.resolveJwt("Bearer invalid")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(utils).resolveJwt("Bearer invalid");
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldSetAuthenticationAndAttributeWhenJwtValid() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(utils.resolveJwt("Bearer valid-token")).thenReturn(decodedJwt);
        UserDetails user = User.builder()
                .username("testuser")
                .password("")
                .authorities(Collections.emptyList())
                .build();
        when(utils.toUser(decodedJwt)).thenReturn(user);
        when(utils.toId(decodedJwt)).thenReturn(100L);

        filter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("testuser", SecurityContextHolder.getContext().getAuthentication().getName());
        verify(request).setAttribute("id", 100L);
        verify(filterChain).doFilter(request, response);
    }
}
