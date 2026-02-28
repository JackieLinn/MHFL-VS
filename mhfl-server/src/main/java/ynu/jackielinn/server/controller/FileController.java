package ynu.jackielinn.server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ynu.jackielinn.server.common.RestResponse;
import ynu.jackielinn.server.common.BaseController;
import ynu.jackielinn.server.service.AccountService;
import ynu.jackielinn.server.service.FileService;

@RestController
@RequestMapping("/api/file")
@Tag(name = "文件接口", description = "文件上传等操作")
public class FileController extends BaseController {

    @Resource
    private FileService fileService;

    @Resource
    private AccountService accountService;

    /**
     * 上传当前用户头像。支持 jpg/jpeg/png/gif/webp，最大 5MB；保存至配置目录后更新 account.avatar 并返回访问 URL。
     *
     * @param file    上传的图片文件（form-data file）
     * @param request 用于获取当前用户 id
     * @return 头像访问 URL，失败时返回 400
     */
    @Operation(summary = "上传用户头像", description = "上传头像图片，仅支持 jpg/jpeg/png/gif/webp，最大 5MB；返回完整 URL 并更新当前用户头像")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功，返回头像访问 URL"),
            @ApiResponse(responseCode = "400", description = "文件为空、格式不允许、超过 5MB 或保存失败"),
            @ApiResponse(responseCode = "401", description = "未登录或 token 过期")
    })
    @PostMapping("/avatar/upload")
    public RestResponse<String> uploadAvatar(
            @Parameter(description = "图片文件，支持 jpg/jpeg/png/gif/webp，最大 5MB", required = true) @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("id");
        if (userId == null) {
            return RestResponse.failure(401, "未登录或登录已过期");
        }
        try {
            String avatarUrl = fileService.saveAvatar(userId, file);
            String err = accountService.updateAvatar(userId, avatarUrl);
            if (err != null) {
                return RestResponse.failure(400, err);
            }
            return RestResponse.success(avatarUrl);
        } catch (IllegalArgumentException e) {
            return RestResponse.failure(400, e.getMessage());
        }
    }
}
