package ynu.jackielinn.server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ynu.jackielinn.server.dto.request.ConfirmResetRO;
import ynu.jackielinn.server.dto.request.EmailRegisterRO;
import ynu.jackielinn.server.dto.request.EmailResetRO;
import ynu.jackielinn.server.common.ApiResponse;
import ynu.jackielinn.server.service.AccountService;

import java.util.function.Function;
import java.util.function.Supplier;

@Validated
@RestController
@RequestMapping("/auth")
@Tag(name = "验证相关接口", description = "与验证相关的操作接口")
public class AuthorizeController {

    @Resource
    AccountService accountService;

    /**
     * 请求邮件验证码
     *
     * @param email   请求邮件
     * @param type    类型
     * @param request 请求
     * @return 是否请求成功
     */
    @Operation(summary = "请求邮件验证码接口", description = "请求邮件验证码")
    @GetMapping("/ask-code")
    public ApiResponse<Void> askVerifyCode(@RequestParam @Email String email,
                                           @RequestParam @Pattern(regexp = "(register|reset)") String type,
                                           HttpServletRequest request) {
        return this.messageHandle(() ->
                accountService.registerEmailVerifyCode(type, email, request.getRemoteAddr()));
    }

    /**
     * 进行用户注册操作，需要先请求邮件验证码
     *
     * @param ro 注册信息
     * @return 是否注册成功
     */
    @Operation(summary = "注册接口", description = "注册接口")
    @PostMapping("/register")
    public ApiResponse<Void> register(@RequestBody @Valid EmailRegisterRO ro) {
        return this.messageHandle(ro, accountService::registerEmailAccount);
    }

    /**
     * 执行密码重置确认，检查验证码是否正确
     *
     * @param ro 密码重置信息
     * @return 是否操作成功
     */
    @Operation(summary = "密码确认重置接口", description = "密码确认重置接口")
    @PostMapping("/reset-confirm")
    public ApiResponse<Void> resetConfirm(@RequestBody @Valid ConfirmResetRO ro) {
        return this.messageHandle(ro, accountService::resetConfirm);
    }

    /**
     * 执行密码重置操作
     *
     * @param ro 密码重置信息
     * @return 是否操作成功
     */
    @Operation(summary = "密码重置接口", description = "密码重置接口")
    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@RequestBody @Valid EmailResetRO ro) {
        return this.messageHandle(ro, accountService::resetEmailAccountPassword);
    }

    /**
     * 针对于返回值为String作为错误信息的方法进行统一处理
     *
     * @param action 具体操作
     * @return 响应结果
     */
    private ApiResponse<Void> messageHandle(Supplier<String> action) {
        String message = action.get();
        return message == null ? ApiResponse.success() : ApiResponse.failure(400, message);
    }

    /**
     * 处理函数式接口 Function<T, String> 定义的操作，并返回一个 RestBean<Void> 类型的结果
     *
     * @param vo       输入参数
     * @param function 函数式接口，接受输入参数并返回一个字符串
     * @return 处理结果
     */
    private <T> ApiResponse<Void> messageHandle(T vo, Function<T, String> function) {
        return messageHandle(() -> function.apply(vo));
    }
}
