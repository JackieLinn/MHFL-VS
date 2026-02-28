package ynu.jackielinn.server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.web.bind.annotation.*;
import ynu.jackielinn.server.common.RestResponse;
import ynu.jackielinn.server.common.BaseController;
import ynu.jackielinn.server.dto.request.CreateAccountRO;
import ynu.jackielinn.server.dto.request.ListAccountRO;
import ynu.jackielinn.server.dto.request.UpdateAccountRO;
import ynu.jackielinn.server.dto.response.AccountVO;
import ynu.jackielinn.server.service.AccountService;

@RestController
@RequestMapping("/api/account")
@Tag(name = "用户接口", description = "用户操作相关接口")
public class AccountController extends BaseController {

    @Resource
    AccountService accountService;

    /**
     * 管理员导入用户操作
     *
     * @param ro 用户基本信息
     * @return 是否操作成功
     */
    @Operation(summary = "管理员导入用户接口", description = "管理员导入用户接口")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功"),
            @ApiResponse(responseCode = "400", description = "用户名/邮箱/电话已存在或参数错误"),
            @ApiResponse(responseCode = "401", description = "未登录或 token 过期"),
            @ApiResponse(responseCode = "403", description = "非管理员无权限")
    })
    @PostMapping("/admin/create")
    public RestResponse<Void> createAccount(@RequestBody @Valid CreateAccountRO ro) {
        return this.messageHandle(ro, accountService::createAccount);
    }

    /**
     * 管理员逻辑删除用户（从 token 中获取当前管理员信息，验证权限后删除指定用户）
     *
     * @param id      要删除的用户 id
     * @param request HttpServletRequest，用于获取当前登录管理员 id（从 JWT token 中解析）
     * @return 是否操作成功
     */
    @Operation(summary = "管理员删除用户接口", description = "管理员逻辑删除用户，将 is_deleted 置为 1")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功"),
            @ApiResponse(responseCode = "400", description = "用户不存在或不能删除自己"),
            @ApiResponse(responseCode = "401", description = "未登录或 token 过期"),
            @ApiResponse(responseCode = "403", description = "非管理员无权限")
    })
    @DeleteMapping("/admin/{id}")
    public RestResponse<Void> deleteAccount(
            @Parameter(description = "要删除的用户 id") @PathVariable Long id,
            HttpServletRequest request) {
        Long currentAdminId = (Long) request.getAttribute("id");
        if (currentAdminId == null) {
            return RestResponse.failure(401, "未登录或登录已过期");
        }
        // 验证管理员权限
        RestResponse<Void> adminCheck = checkAdmin(request);
        if (adminCheck != null) {
            return adminCheck;
        }
        return this.messageHandle(() -> accountService.deleteAccount(id, currentAdminId));
    }

    /**
     * 更新用户信息（只能更新当前登录用户自己的信息）
     *
     * @param ro      更新信息对象（username、gender、telephone、birthday 可选）
     * @param request HttpServletRequest，用于获取当前登录用户 id（从 JWT token 中解析）
     * @return 是否操作成功
     */
    @Operation(summary = "更新用户信息接口", description = "更新当前登录用户的信息")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功"),
            @ApiResponse(responseCode = "400", description = "用户名/电话已被占用或更新失败"),
            @ApiResponse(responseCode = "401", description = "未登录或 token 过期")
    })
    @PutMapping("/update")
    public RestResponse<Void> updateAccount(@RequestBody @Valid UpdateAccountRO ro, HttpServletRequest request) {
        Long currentUserId = (Long) request.getAttribute("id");
        if (currentUserId == null) {
            return RestResponse.failure(401, "未登录或登录已过期");
        }
        return this.messageHandle(() -> accountService.updateAccount(ro, currentUserId));
    }

    /**
     * 管理员查询用户列表（支持关键字模糊查询和时间范围查询，分页）
     *
     * @param ro 查询条件对象（关键字、分页参数、时间范围）
     * @return 分页结果（AccountVO，排除敏感信息）
     */
    @Operation(summary = "管理员查询用户列表接口", description = "管理员查询用户列表，支持关键字模糊查询（用户名、邮箱、电话号码）和时间范围查询，分页查询")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功"),
            @ApiResponse(responseCode = "401", description = "未登录或 token 过期"),
            @ApiResponse(responseCode = "403", description = "非管理员无权限")
    })
    @GetMapping("/admin/list")
    public RestResponse<IPage<AccountVO>> listAccounts(@Valid @ModelAttribute ListAccountRO ro) {
        IPage<AccountVO> result = accountService.listAccounts(ro);
        return RestResponse.success(result);
    }

    /**
     * 用户查询自己的信息（只能查询自己的信息）
     *
     * @param request HttpServletRequest，用于获取当前登录用户 id（从 JWT token 中解析）
     * @return 用户信息（AccountVO，排除敏感信息）
     */
    @Operation(summary = "查询当前用户信息接口", description = "查询当前登录用户的信息，只能查询自己的信息")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功"),
            @ApiResponse(responseCode = "401", description = "未登录或 token 过期"),
            @ApiResponse(responseCode = "404", description = "用户不存在")
    })
    @GetMapping("/info")
    public RestResponse<AccountVO> getAccountInfo(HttpServletRequest request) {
        Long currentUserId = (Long) request.getAttribute("id");
        if (currentUserId == null) {
            return RestResponse.failure(401, "未登录或登录已过期");
        }
        AccountVO accountVO = accountService.getAccountInfo(currentUserId);
        if (accountVO == null) {
            return RestResponse.failure(404, "用户不存在");
        }
        return RestResponse.success(accountVO);
    }
}
