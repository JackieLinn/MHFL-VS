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
import ynu.jackielinn.server.dto.response.DashboardPlatformStatsVO;
import ynu.jackielinn.server.service.DashboardService;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "仪表盘 Dashboard 接口", description = "仪表盘 Dashboard 操作相关接口")
public class DashboardController extends BaseController {

    @Resource
    private DashboardService dashboardService;

    /**
     * 获取平台概览统计（用户总数、任务总数、数据集总数、算法总数）。
     *
     * @return DashboardPlatformStatsVO
     */
    @Operation(summary = "获取平台概览统计", description = "返回全平台用户、任务、数据集、算法总数")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功"),
            @ApiResponse(responseCode = "401", description = "未登录或 token 过期")
    })
    @GetMapping("/platform-stats")
    public RestResponse<DashboardPlatformStatsVO> getPlatformStats() {
        DashboardPlatformStatsVO stats = dashboardService.getPlatformStats();
        return RestResponse.success(stats);
    }
}
