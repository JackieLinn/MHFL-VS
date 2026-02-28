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
import ynu.jackielinn.server.dto.request.ListDatasetRO;
import ynu.jackielinn.server.dto.response.DatasetVO;
import ynu.jackielinn.server.service.DatasetService;

@RestController
@RequestMapping("/api/dataset")
@Tag(name = "数据集接口", description = "数据集操作相关接口")
public class DatasetController extends BaseController {

    @Resource
    DatasetService datasetService;

    /**
     * 管理员创建数据集操作
     *
     * @param dataName 数据集名字
     * @param request  HttpServletRequest，用于验证管理员权限
     * @return 是否操作成功
     */
    @Operation(summary = "管理员创建数据集接口", description = "管理员创建数据集，输入数据集名字即可")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功"),
            @ApiResponse(responseCode = "400", description = "数据集名已存在或参数错误"),
            @ApiResponse(responseCode = "401", description = "未登录或 token 过期"),
            @ApiResponse(responseCode = "403", description = "非管理员无权限")
    })
    @PostMapping("/admin/create")
    public RestResponse<Void> createDataset(
            @Parameter(description = "数据集名称", required = true) @RequestParam @NotBlank(message = "数据集名字不能为空") String dataName,
            HttpServletRequest request) {
        // 验证管理员权限
        RestResponse<Void> adminCheck = checkAdmin(request);
        if (adminCheck != null) {
            return adminCheck;
        }
        return this.messageHandle(() -> datasetService.createDataset(dataName));
    }

    /**
     * 管理员逻辑删除数据集
     *
     * @param id      要删除的数据集 id
     * @param request HttpServletRequest，用于验证管理员权限
     * @return 是否操作成功
     */
    @Operation(summary = "管理员删除数据集接口", description = "管理员逻辑删除数据集，将 is_deleted 置为 1")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功"),
            @ApiResponse(responseCode = "400", description = "数据集不存在或删除失败"),
            @ApiResponse(responseCode = "401", description = "未登录或 token 过期"),
            @ApiResponse(responseCode = "403", description = "非管理员无权限")
    })
    @DeleteMapping("/admin/{id}")
    public RestResponse<Void> deleteDataset(
            @Parameter(description = "数据集 id") @PathVariable Long id,
            HttpServletRequest request) {
        // 验证管理员权限
        RestResponse<Void> adminCheck = checkAdmin(request);
        if (adminCheck != null) {
            return adminCheck;
        }
        return this.messageHandle(() -> datasetService.deleteDataset(id));
    }

    /**
     * 管理员更新数据集名字
     *
     * @param id       要更新的数据集 id
     * @param dataName 新的数据集名字
     * @param request  HttpServletRequest，用于验证管理员权限
     * @return 是否操作成功
     */
    @Operation(summary = "管理员更新数据集接口", description = "管理员更新数据集名字，输入数据集名字即可")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功"),
            @ApiResponse(responseCode = "400", description = "数据集不存在、名字已被占用或更新失败"),
            @ApiResponse(responseCode = "401", description = "未登录或 token 过期"),
            @ApiResponse(responseCode = "403", description = "非管理员无权限")
    })
    @PutMapping("/admin/{id}")
    public RestResponse<Void> updateDataset(
            @Parameter(description = "数据集 id") @PathVariable Long id,
            @Parameter(description = "新的数据集名称", required = true) @RequestParam @NotBlank(message = "数据集名字不能为空") String dataName,
            HttpServletRequest request) {
        // 验证管理员权限
        RestResponse<Void> adminCheck = checkAdmin(request);
        if (adminCheck != null) {
            return adminCheck;
        }
        return this.messageHandle(() -> datasetService.updateDataset(id, dataName));
    }

    /**
     * 管理员查询数据集列表（支持关键字模糊查询和时间范围查询，分页）
     *
     * @param ro 查询条件对象（关键字、分页参数、时间范围）
     * @return 分页结果（DatasetVO，排除敏感信息）
     */
    @Operation(summary = "管理员查询数据集列表接口", description = "管理员查询数据集列表，支持关键字模糊查询（数据集名字）和时间范围查询，分页查询")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功"),
            @ApiResponse(responseCode = "401", description = "未登录或 token 过期"),
            @ApiResponse(responseCode = "403", description = "非管理员无权限")
    })
    @GetMapping("/admin/list")
    public RestResponse<IPage<DatasetVO>> listDatasets(@Valid @ModelAttribute ListDatasetRO ro) {
        IPage<DatasetVO> result = datasetService.listDatasets(ro);
        return RestResponse.success(result);
    }
}
