package ynu.jackielinn.server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ynu.jackielinn.server.common.ApiResponse;
import ynu.jackielinn.server.dto.response.SystemResourcesVO;
import ynu.jackielinn.server.service.ResourceService;

/**
 * 资源管理Controller
 * 提供系统资源查询接口
 */
@RestController
@RequestMapping("/api/system")
@Tag(name = "系统资源管理", description = "系统资源查询相关接口")
public class ResourceController {

    @Resource
    private ResourceService resourceService;

    @Operation(summary = "获取系统资源信息", description = "获取CPU、内存、GPU的实时使用情况")
    @GetMapping("/resources")
    public ApiResponse<SystemResourcesVO> getSystemResources() {
        SystemResourcesVO resources = resourceService.getSystemResources();
        return ApiResponse.success(resources);
    }
}
