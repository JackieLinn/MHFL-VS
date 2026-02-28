package ynu.jackielinn.server.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;
import ynu.jackielinn.server.common.RestResponse;
import ynu.jackielinn.server.common.BaseController;
import ynu.jackielinn.server.dto.request.ListAlgorithmRO;
import ynu.jackielinn.server.dto.response.AlgorithmVO;
import ynu.jackielinn.server.service.AlgorithmService;

@RestController
@RequestMapping("/api/algorithm")
@Tag(name = "算法接口", description = "算法操作相关接口")
public class AlgorithmController extends BaseController {

    @Resource
    AlgorithmService algorithmService;

    /**
     * 管理员创建算法操作
     *
     * @param algorithmName 算法名字
     * @param request       HttpServletRequest，用于验证管理员权限
     * @return 是否操作成功
     */
    @Operation(summary = "管理员创建算法接口", description = "管理员创建算法，输入算法名字即可")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功"),
            @ApiResponse(responseCode = "400", description = "算法名已存在或参数错误"),
            @ApiResponse(responseCode = "401", description = "未登录或 token 过期"),
            @ApiResponse(responseCode = "403", description = "非管理员无权限")
    })
    @PostMapping("/admin/create")
    public RestResponse<Void> createAlgorithm(
            @Parameter(description = "算法名称", required = true) @RequestParam @NotBlank(message = "算法名字不能为空") String algorithmName,
            HttpServletRequest request) {
        // 验证管理员权限
        RestResponse<Void> adminCheck = checkAdmin(request);
        if (adminCheck != null) {
            return adminCheck;
        }
        return this.messageHandle(() -> algorithmService.createAlgorithm(algorithmName));
    }

    /**
     * 管理员逻辑删除算法
     *
     * @param id      要删除的算法 id
     * @param request HttpServletRequest，用于验证管理员权限
     * @return 是否操作成功
     */
    @Operation(summary = "管理员删除算法接口", description = "管理员逻辑删除算法，将 is_deleted 置为 1")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功"),
            @ApiResponse(responseCode = "400", description = "算法不存在或删除失败"),
            @ApiResponse(responseCode = "401", description = "未登录或 token 过期"),
            @ApiResponse(responseCode = "403", description = "非管理员无权限")
    })
    @DeleteMapping("/admin/{id}")
    public RestResponse<Void> deleteAlgorithm(
            @Parameter(description = "算法 id") @PathVariable Long id,
            HttpServletRequest request) {
        // 验证管理员权限
        RestResponse<Void> adminCheck = checkAdmin(request);
        if (adminCheck != null) {
            return adminCheck;
        }
        return this.messageHandle(() -> algorithmService.deleteAlgorithm(id));
    }

    /**
     * 管理员更新算法名字
     *
     * @param id            要更新的算法 id
     * @param algorithmName 新的算法名字
     * @param request       HttpServletRequest，用于验证管理员权限
     * @return 是否操作成功
     */
    @Operation(summary = "管理员更新算法接口", description = "管理员更新算法名字，输入算法名字即可")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功"),
            @ApiResponse(responseCode = "400", description = "算法不存在、名字已被占用或更新失败"),
            @ApiResponse(responseCode = "401", description = "未登录或 token 过期"),
            @ApiResponse(responseCode = "403", description = "非管理员无权限")
    })
    @PutMapping("/admin/{id}")
    public RestResponse<Void> updateAlgorithm(
            @Parameter(description = "算法 id") @PathVariable Long id,
            @Parameter(description = "新的算法名称", required = true) @RequestParam @NotBlank(message = "算法名字不能为空") String algorithmName,
            HttpServletRequest request) {
        // 验证管理员权限
        RestResponse<Void> adminCheck = checkAdmin(request);
        if (adminCheck != null) {
            return adminCheck;
        }
        return this.messageHandle(() -> algorithmService.updateAlgorithm(id, algorithmName));
    }

    /**
     * 管理员查询算法列表（支持关键字模糊查询和时间范围查询，分页）
     *
     * @param ro 查询条件对象（关键字、分页参数、时间范围）
     * @return 分页结果（AlgorithmVO，排除敏感信息）
     */
    @Operation(summary = "管理员查询算法列表接口", description = "管理员查询算法列表，支持关键字模糊查询（算法名字）和时间范围查询，分页查询")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功"),
            @ApiResponse(responseCode = "401", description = "未登录或 token 过期"),
            @ApiResponse(responseCode = "403", description = "非管理员无权限")
    })
    @GetMapping("/admin/list")
    public RestResponse<IPage<AlgorithmVO>> listAlgorithms(@Valid @ModelAttribute ListAlgorithmRO ro) {
        IPage<AlgorithmVO> result = algorithmService.listAlgorithms(ro);
        return RestResponse.success(result);
    }
}
