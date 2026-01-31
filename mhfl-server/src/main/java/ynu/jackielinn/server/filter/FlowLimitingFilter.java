package ynu.jackielinn.server.filter;

import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import ynu.jackielinn.server.entity.ApiResponse;
import ynu.jackielinn.server.utils.Const;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 限流控制过滤器
 * 防止用户高频请求接口，借助 Redis 进行限流
 */
@Component
@Order(Const.ORDER_LIMIT)
public class FlowLimitingFilter extends HttpFilter {

    @Resource
    StringRedisTemplate template;

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
        String address = request.getRemoteAddr();
        if (this.tryCount(address)) {
            chain.doFilter(request, response);
        } else {
            this.writeBlockMessage(response);
        }
    }

    /**
     * 尝试对指定IP地址请求计数，如果被限制则无法继续访问
     *
     * @param ip 请求IP地址
     * @return 是否操作成功
     */
    private boolean tryCount(String ip) {
        synchronized (ip.intern()) {
            if (template.hasKey(Const.FLOW_LIMIT_BLOCK + ip))
                return false;
            return this.limitPeriodCheck(ip);
        }
    }

    /**
     * 为响应编写拦截内容，提示用户操作频繁
     *
     * @param response 响应
     * @throws IOException 可能的异常
     */
    private void writeBlockMessage(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=utf-8");
        PrintWriter writer = response.getWriter();
        writer.write(ApiResponse.forbidden("操作频繁，请稍后再试").asJsonString());
    }

    /**
     * 检查指定 IP 地址的请求是否在限制周期内
     *
     * @param ip 请求的 IP 地址
     * @return 如果请求未超过限制，返回 true；否则返回 false
     */
    private boolean limitPeriodCheck(String ip) {
        if (template.hasKey(Const.FLOW_LIMIT_COUNTER + ip)) {
            long increment = Optional.ofNullable(
                    template.opsForValue().increment(Const.FLOW_LIMIT_COUNTER + ip)).orElse(0L);
            if (increment > 20) {
                template.opsForValue().set(Const.FLOW_LIMIT_BLOCK + ip, "", 10, TimeUnit.SECONDS);
                return false;
            }
        } else {
            template.opsForValue().set(Const.FLOW_LIMIT_COUNTER + ip, "1", 10, TimeUnit.SECONDS);
        }
        return true;
    }
}
