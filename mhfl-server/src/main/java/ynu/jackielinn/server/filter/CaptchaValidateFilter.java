package ynu.jackielinn.server.filter;

import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import ynu.jackielinn.server.entity.ApiResponse;
import ynu.jackielinn.server.utils.Const;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 图形验证码验证过滤器
 * 在登录请求时验证图形验证码是否正确
 */
@Component
public class CaptchaValidateFilter extends OncePerRequestFilter {

    @Resource
    StringRedisTemplate stringRedisTemplate;

    private static final String LOGIN_URL = "/auth/login";

    private static final int MAX_FAIL_COUNT = 5;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 只对登录请求进行验证码校验
        if (isLoginRequest(request)) {
            String captchaId = request.getParameter("captchaId");
            String captchaCode = request.getParameter("captchaCode");

            // 验证参数是否存在
            if (!StringUtils.hasText(captchaId) || !StringUtils.hasText(captchaCode)) {
                writeCaptchaError(response, "验证码ID和验证码不能为空");
                return;
            }

            // 从 Redis 获取验证码
            String dataKey = Const.VERIFY_CAPTCHA_DATA + captchaId;
            String savedCode = stringRedisTemplate.opsForValue().get(dataKey);

            // 验证验证码是否存在
            if (savedCode == null) {
                writeCaptchaError(response, "验证码已过期或无效，请刷新重试");
                return;
            }

            // 验证验证码是否正确（忽略大小写）
            if (!savedCode.equalsIgnoreCase(captchaCode.trim())) {
                // 验证失败，增加失败计数
                String failKey = Const.VERIFY_CAPTCHA_FAIL_COUNT + captchaId;
                long failCount = Optional.ofNullable(
                        stringRedisTemplate.opsForValue().increment(failKey)).orElse(1L);

                // 设置失败计数的过期时间（与验证码相同，3分钟）
                if (failCount == 1) {
                    stringRedisTemplate.expire(failKey, 3, TimeUnit.MINUTES);
                }

                // 如果失败次数达到上限，删除验证码
                if (failCount >= MAX_FAIL_COUNT) {
                    stringRedisTemplate.delete(dataKey);
                    stringRedisTemplate.delete(failKey);
                    writeCaptchaError(response, "验证码错误次数过多，请刷新获取新验证码");
                } else {
                    int remainingAttempts = MAX_FAIL_COUNT - (int) failCount;
                    writeCaptchaError(response, "验证码错误，还可重试" + remainingAttempts + "次");
                }
                return;
            }

            // 验证成功，删除验证码和失败计数
            stringRedisTemplate.delete(dataKey);
            stringRedisTemplate.delete(Const.VERIFY_CAPTCHA_FAIL_COUNT + captchaId);
        }

        // 继续过滤链
        filterChain.doFilter(request, response);
    }

    /**
     * 判断是否为登录请求
     *
     * @param request HttpServletRequest
     * @return 是否为登录请求
     */
    private boolean isLoginRequest(HttpServletRequest request) {
        return LOGIN_URL.equals(request.getRequestURI()) && "POST".equalsIgnoreCase(request.getMethod());
    }

    /**
     * 写入验证码错误响应
     *
     * @param response HttpServletResponse
     * @param message  错误信息
     * @throws IOException IO异常
     */
    private void writeCaptchaError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(ApiResponse.failure(400, message).asJsonString());
    }
}
