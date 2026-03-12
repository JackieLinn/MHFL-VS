package ynu.jackielinn.server.filter;

import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import ynu.jackielinn.server.common.RestResponse;
import ynu.jackielinn.server.utils.Const;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * 限流控制过滤器
 * 防止用户高频请求接口，借助 Redis 进行限流。
 * 使用 Lua 脚本原子执行 INCR + EXPIRE，消除 hasKey/INCR 竞态导致的 TTL 丢失（-1）。
 */
@Component
@Order(Const.ORDER_LIMIT)
public class FlowLimitingFilter extends HttpFilter {

    private static final String INCR_WITH_EXPIRE_SCRIPT =
            """
                    local current = redis.call('INCR', KEYS[1])
                    if current == 1 then
                      redis.call('EXPIRE', KEYS[1], 10)
                    end
                    return current""";

    private static final RedisScript<Long> SCRIPT =
            new DefaultRedisScript<>(INCR_WITH_EXPIRE_SCRIPT, Long.class);

    @Resource
    private StringRedisTemplate template;

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
        String address = normalizeClientAddress(request.getRemoteAddr());
        if (this.tryCount(address)) {
            chain.doFilter(request, response);
        } else {
            this.writeBlockMessage(response);
        }
    }

    /**
     * 归一化客户端地址。将 IPv6 localhost 统一为 127.0.0.1，
     * 避免同一机器因 IPv4/IPv6 切换导致限流 key 分裂。
     */
    private String normalizeClientAddress(String address) {
        if (address == null) return "127.0.0.1";
        if ("0:0:0:0:0:0:0:1".equals(address) || "::1".equals(address)) {
            return "127.0.0.1";
        }
        return address;
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
        writer.write(RestResponse.forbidden("操作频繁，请稍后再试").asJsonString());
    }

    /**
     * 使用 Lua 脚本原子执行 INCR + EXPIRE：
     * - INCR 不存在的 key 时 Redis 会创建，返回 1，脚本中设置 EXPIRE 10s
     * - INCR 已存在的 key 时保留原 TTL，不修改
     * 消除 hasKey 与 INCR 之间的竞态（key 在 hasKey 后、INCR 前过期导致 INCR 创建无 TTL 的 key）。
     */
    private boolean limitPeriodCheck(String ip) {
        String counterKey = Const.FLOW_LIMIT_COUNTER + ip;
        String blockKey = Const.FLOW_LIMIT_BLOCK + ip;

        Long current = template.execute(SCRIPT, Collections.singletonList(counterKey));
        if (current == null) current = 0L;

        if (current > 100) {
            template.opsForValue().set(blockKey, "");
            template.expire(blockKey, 10, TimeUnit.SECONDS);
            return false;
        }
        return true;
    }
}
