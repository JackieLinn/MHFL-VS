package ynu.jackielinn.server.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Controller 基类
 */
public abstract class BaseController {

    /**
     * 针对返回值为 String 作为错误信息的方法进行统一处理
     *
     * @param action 具体操作
     * @return 响应结果
     */
    protected ApiResponse<Void> messageHandle(Supplier<String> action) {
        String message = action.get();
        return message == null ? ApiResponse.success() : ApiResponse.failure(400, message);
    }

    /**
     * 处理函数式接口 Function<T, String> 定义的操作，并返回 ApiResponse<Void> 类型的结果
     *
     * @param ro       请求对象
     * @param function 函数式接口，接受请求对象并返回一个字符串（null=成功，非null=错误信息）
     * @return 处理结果
     */
    protected <T> ApiResponse<Void> messageHandle(T ro, Function<T, String> function) {
        return messageHandle(() -> function.apply(ro));
    }

    /**
     * 检查当前用户是否为管理员，如果不是则返回错误响应
     * 虽然路径已经通过 Spring Security 保护，但双重验证更安全
     *
     * @param request HttpServletRequest
     * @return 如果是管理员返回 null，否则返回错误响应
     */
    protected ApiResponse<Void> checkAdmin(HttpServletRequest request) {
        Long currentUserId = (Long) request.getAttribute("id");
        if (currentUserId == null) {
            return ApiResponse.failure(403, "无权限执行此操作");
        }
        // 验证当前用户是管理员
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return ApiResponse.failure(403, "无权限执行此操作");
        }
        // 检查是否有 ROLE_admin 权限
        boolean hasAdminRole = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(auth -> auth.equals("ROLE_admin"));
        if (!hasAdminRole) {
            return ApiResponse.failure(403, "无权限执行此操作");
        }
        return null;
    }
}
