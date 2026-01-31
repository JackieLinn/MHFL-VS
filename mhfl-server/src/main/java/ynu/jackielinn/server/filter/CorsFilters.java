package ynu.jackielinn.server.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import ynu.jackielinn.server.utils.Const;

import java.io.IOException;

/**
 * 实现跨域的Filter
 * 由于CorsFilter与SpringSecurity中的CorsFilter重名
 * 所以改成CorsFilters
 */
@Component
@Order(Const.ORDER_CORS)
public class CorsFilters extends HttpFilter {

    /**
     * 过滤器的核心方法，用于处理请求
     *
     * @param request  HttpServletRequest 对象，包含了客户端的请求信息
     * @param response HttpServletResponse 对象，用于向客户端发送响应
     * @param chain    FilterChain 对象，用于将请求传递给下一个过滤器或目标资源
     * @throws IOException      如果在处理请求或响应时发生 I/O 错误
     * @throws ServletException 如果在处理请求时发生 Servlet 异常
     */
    @Override
    protected void doFilter(HttpServletRequest request,
                            HttpServletResponse response,
                            FilterChain chain) throws IOException, ServletException {
        this.addCorsHeader(request, response);
        chain.doFilter(request, response);
    }

    /**
     * 向响应中添加跨域请求头
     *
     * @param request  HttpServletRequest 对象，包含了客户端的请求信息
     * @param response HttpServletResponse 对象，用于向客户端发送响应
     */
    private void addCorsHeader(HttpServletRequest request,
                               HttpServletResponse response) {
        response.addHeader("Access-Control-Allow-Origin", request.getHeader("Origin"));
        response.addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS, DELETE, PUT");
        response.addHeader("Access-Control-Allow-Headers", "Authorization, Content-Type");
    }
}
