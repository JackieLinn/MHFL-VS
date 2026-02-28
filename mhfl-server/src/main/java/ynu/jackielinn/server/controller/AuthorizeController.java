package ynu.jackielinn.server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
import ynu.jackielinn.server.common.RestResponse;
import ynu.jackielinn.server.common.BaseController;
import ynu.jackielinn.server.service.AccountService;

@Validated
@RestController
@RequestMapping("/auth")
@Tag(name = "验证相关接口", description = "与验证相关的操作接口")
public class AuthorizeController extends BaseController {

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
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功"),
            @ApiResponse(responseCode = "400", description = "请求频繁或参数错误")
    })
    @GetMapping("/ask-code")
    public RestResponse<Void> askVerifyCode(
            @Parameter(description = "收件邮箱", required = true) @RequestParam @Email String email,
            @Parameter(description = "类型：register 注册 / reset 重置密码", required = true) @RequestParam @Pattern(regexp = "(register|reset)") String type,
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
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功"),
            @ApiResponse(responseCode = "400", description = "验证码错误、邮箱/用户名/电话已注册或参数错误")
    })
    @PostMapping("/register")
    public RestResponse<Void> register(@RequestBody @Valid EmailRegisterRO ro) {
        return this.messageHandle(ro, accountService::registerEmailAccount);
    }

    /**
     * 执行密码重置确认，检查验证码是否正确
     *
     * @param ro 密码重置信息
     * @return 是否操作成功
     */
    @Operation(summary = "密码确认重置接口", description = "密码确认重置接口")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功"),
            @ApiResponse(responseCode = "400", description = "请先获取验证码或验证码错误")
    })
    @PostMapping("/reset-confirm")
    public RestResponse<Void> resetConfirm(@RequestBody @Valid ConfirmResetRO ro) {
        return this.messageHandle(ro, accountService::resetConfirm);
    }

    /**
     * 执行密码重置操作
     *
     * @param ro 密码重置信息
     * @return 是否操作成功
     */
    @Operation(summary = "密码重置接口", description = "密码重置接口")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功"),
            @ApiResponse(responseCode = "400", description = "验证码错误或更新失败")
    })
    @PostMapping("/reset-password")
    public RestResponse<Void> resetPassword(@RequestBody @Valid EmailResetRO ro) {
        return this.messageHandle(ro, accountService::resetEmailAccountPassword);
    }
}
