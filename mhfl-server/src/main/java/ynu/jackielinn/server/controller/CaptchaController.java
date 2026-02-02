package ynu.jackielinn.server.controller;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.util.IdUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ynu.jackielinn.server.entity.ApiResponse;
import ynu.jackielinn.server.utils.Const;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 图形验证码 Controller
 * 用于生成和刷新登录时使用的图形验证码
 */
@RestController
@RequestMapping("/captcha")
@Tag(name = "Captcha", description = "图形验证码相关接口")
public class CaptchaController {

    @Resource
    StringRedisTemplate stringRedisTemplate;

    private static final int LIMIT_PERIOD = 10;
    private static final int LIMIT_COUNT = 3;

    /**
     * 生成图形验证码
     * 返回验证码图片的 Base64 编码和验证码 ID
     *
     * @param request HttpServletRequest 用于获取客户端 IP
     * @return 包含 captchaId 和 captchaImage 的响应
     */
    @GetMapping("/generate")
    @Operation(summary = "生成图形验证码", description = "生成图形验证码，返回验证码ID和Base64编码的图片")
    public ApiResponse<Map<String, String>> generateCaptcha(HttpServletRequest request) {
        String ip = request.getRemoteAddr();

        // 限流检查：10 秒内最多请求 3 次
        if (this.isLimited(ip)) {
            return ApiResponse.failure(400, "请求频繁，请稍后再试");
        }

        // 生成验证码，宽度 130，高度 48，验证码长度 4，干扰线数量 20
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(130, 48, 4, 20);

        // 生成唯一的验证码 ID
        String captchaId = IdUtil.fastSimpleUUID();

        // 获取验证码文本（小写存储，验证时也转小写，忽略大小写）
        String captchaCode = captcha.getCode().toLowerCase();

        // 将验证码存入 Redis，有效期 3 分钟
        stringRedisTemplate.opsForValue().set(
                Const.VERIFY_CAPTCHA_DATA + captchaId,
                captchaCode,
                3,
                TimeUnit.MINUTES
        );

        // 返回验证码 ID 和 Base64 编码的图片
        Map<String, String> result = Map.of(
                "captchaId", captchaId,
                "captchaImage", captcha.getImageBase64Data()
        );

        return ApiResponse.success(result);
    }

    /**
     * 直接输出图形验证码图片（可选接口，用于直接在 img 标签中显示）
     *
     * @param request  HttpServletRequest 用于获取客户端 IP
     * @param response HttpServletResponse
     * @throws IOException IO异常
     */
    @GetMapping("/image")
    @Operation(summary = "直接输出验证码图片", description = "直接输出验证码图片流，需要通过响应头获取captchaId")
    public void getCaptchaImage(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String ip = request.getRemoteAddr();

        // 限流检查：10 秒内最多请求 3 次
        if (this.isLimited(ip)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(ApiResponse.failure(400, "请求频繁，请稍后再试").asJsonString());
            return;
        }

        // 生成验证码
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(130, 48, 4, 20);

        // 生成唯一的验证码 ID
        String captchaId = IdUtil.fastSimpleUUID();

        // 获取验证码文本
        String captchaCode = captcha.getCode().toLowerCase();

        // 将验证码存入 Redis，有效期 3 分钟
        stringRedisTemplate.opsForValue().set(
                Const.VERIFY_CAPTCHA_DATA + captchaId,
                captchaCode,
                3,
                TimeUnit.MINUTES
        );

        // 设置响应头
        response.setContentType("image/png");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
        response.setHeader("Captcha-Id", captchaId);

        // 输出图片流
        captcha.write(response.getOutputStream());
    }

    /**
     * 检查指定 IP 是否被限流（10 秒内最多 3 次请求）
     *
     * @param ip 要检查的 IP 地址
     * @return 如果被限流返回 true，否则返回 false
     */
    private boolean isLimited(String ip) {
        String key = Const.VERIFY_CAPTCHA_LIMIT + ip;

        // 检查当前计数
        String countStr = stringRedisTemplate.opsForValue().get(key);
        if (countStr != null) {
            long count = Long.parseLong(countStr);
            if (count >= LIMIT_COUNT) {
                return true;
            }
            // 增加计数
            stringRedisTemplate.opsForValue().increment(key);
        } else {
            // 首次请求，设置计数为 1，过期时间为 10 秒
            stringRedisTemplate.opsForValue().set(key, "1", LIMIT_PERIOD, TimeUnit.SECONDS);
        }
        return false;
    }
}
