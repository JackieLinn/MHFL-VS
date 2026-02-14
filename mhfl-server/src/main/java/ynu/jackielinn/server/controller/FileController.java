package ynu.jackielinn.server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ynu.jackielinn.server.common.ApiResponse;
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

    @Operation(summary = "上传用户头像", description = "上传头像图片，仅支持 jpg/jpeg/png/gif/webp，最大 5MB；返回完整 URL 并更新当前用户头像")
    @PostMapping("/avatar/upload")
    public ApiResponse<String> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("id");
        if (userId == null) {
            return ApiResponse.failure(401, "未登录或登录已过期");
        }
        try {
            String avatarUrl = fileService.saveAvatar(userId, file);
            String err = accountService.updateAvatar(userId, avatarUrl);
            if (err != null) {
                return ApiResponse.failure(400, err);
            }
            return ApiResponse.success(avatarUrl);
        } catch (IllegalArgumentException e) {
            return ApiResponse.failure(400, e.getMessage());
        }
    }
}
