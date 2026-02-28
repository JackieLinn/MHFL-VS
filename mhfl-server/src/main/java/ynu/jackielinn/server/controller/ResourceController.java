package ynu.jackielinn.server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ynu.jackielinn.server.common.RestResponse;
import ynu.jackielinn.server.common.BaseController;
import ynu.jackielinn.server.dto.response.SystemResourcesVO;
import ynu.jackielinn.server.service.ResourceService;

/**
 * 资源管理Controller
 * 提供系统资源查询接口
 */
@RestController
@RequestMapping("/api/system")
@Tag(name = "系统资源管理", description = "系统资源查询相关接口")
public class ResourceController extends BaseController {

    @Resource
    private ResourceService resourceService;

    /**
     * 获取系统资源实时信息。通过调用 Python FastAPI 获取 CPU、内存、GPU 使用率及用量等。
     *
     * @return 封装后的 SystemResourcesVO（CPU/内存/GPU 信息）
     */
    @Operation(summary = "获取系统资源信息", description = "获取CPU、内存、GPU的实时使用情况")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功"),
            @ApiResponse(responseCode = "401", description = "未登录或 token 过期"),
            @ApiResponse(responseCode = "500", description = "调用 Python 资源接口失败")
    })
    @GetMapping("/resources")
    public RestResponse<SystemResourcesVO> getSystemResources() {
        SystemResourcesVO resources = resourceService.getSystemResources();
        return RestResponse.success(resources);
    }
}
