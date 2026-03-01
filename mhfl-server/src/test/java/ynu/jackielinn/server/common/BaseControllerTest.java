package ynu.jackielinn.server.common;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * BaseController 单元测试：messageHandle、checkAdmin、isAdmin。
 * 通过子类暴露 protected 方法进行测试。
 */
@ExtendWith(MockitoExtension.class)
class BaseControllerTest {

    private final BaseControllerImpl controller = new BaseControllerImpl();

    @BeforeEach
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void messageHandleWithSupplierShouldReturnSuccessWhenNull() {
        RestResponse<Void> res = controller.publicMessageHandle(() -> null);
        assertEquals(200, res.code());
        assertNull(res.data());
    }

    @Test
    void messageHandleWithSupplierShouldReturnFailure400WhenNonNull() {
        RestResponse<Void> res = controller.publicMessageHandle(() -> "错误信息");
        assertEquals(400, res.code());
        assertEquals("错误信息", res.message());
    }

    @Test
    void messageHandleWithFunctionShouldReturnSuccessWhenFunctionReturnsNull() {
        RestResponse<Void> res = controller.publicMessageHandle("ro", ro -> null);
        assertEquals(200, res.code());
    }

    @Test
    void messageHandleWithFunctionShouldReturnFailureWhenFunctionReturnsMessage() {
        RestResponse<Void> res = controller.publicMessageHandle("ro", ro -> "参数无效");
        assertEquals(400, res.code());
        assertEquals("参数无效", res.message());
    }

    @Test
    void checkAdminShouldReturnFailure403WhenUserIdNull() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute("id")).thenReturn(null);

        RestResponse<Void> res = controller.publicCheckAdmin(request);

        assertNotNull(res);
        assertEquals(403, res.code());
        assertTrue(res.message().contains("无权限"));
    }

    @Test
    void checkAdminShouldReturnFailure403WhenAuthenticationNull() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute("id")).thenReturn(1L);
        SecurityContextHolder.getContext().setAuthentication(null);

        RestResponse<Void> res = controller.publicCheckAdmin(request);

        assertNotNull(res);
        assertEquals(403, res.code());
    }

    @Test
    void checkAdminShouldReturnFailure403WhenNotAdmin() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute("id")).thenReturn(1L);
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "user", null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_user")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        RestResponse<Void> res = controller.publicCheckAdmin(request);

        assertNotNull(res);
        assertEquals(403, res.code());
    }

    @Test
    void checkAdminShouldReturnNullWhenAdmin() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute("id")).thenReturn(1L);
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "admin", null, List.of(new SimpleGrantedAuthority("ROLE_admin")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        RestResponse<Void> res = controller.publicCheckAdmin(request);

        assertNull(res);
    }

    @Test
    void isAdminShouldReturnFalseWhenAuthenticationNull() {
        SecurityContextHolder.clearContext();
        assertFalse(controller.publicIsAdmin());
    }

    @Test
    void isAdminShouldReturnFalseWhenNoAdminRole() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "user", null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_user")));
        SecurityContextHolder.getContext().setAuthentication(auth);
        assertFalse(controller.publicIsAdmin());
    }

    @Test
    void isAdminShouldReturnTrueWhenHasAdminRole() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "admin", null, List.of(new SimpleGrantedAuthority("ROLE_admin")));
        SecurityContextHolder.getContext().setAuthentication(auth);
        assertTrue(controller.publicIsAdmin());
    }

    /**
     * 测试用子类，暴露 protected 方法为 public。
     */
    private static class BaseControllerImpl extends BaseController {
        RestResponse<Void> publicMessageHandle(java.util.function.Supplier<String> action) {
            return messageHandle(action);
        }

        <T> RestResponse<Void> publicMessageHandle(T ro, java.util.function.Function<T, String> function) {
            return messageHandle(ro, function);
        }

        RestResponse<Void> publicCheckAdmin(HttpServletRequest request) {
            return checkAdmin(request);
        }

        boolean publicIsAdmin() {
            return isAdmin();
        }
    }
}
