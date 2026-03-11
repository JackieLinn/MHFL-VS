package ynu.jackielinn.server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ynu.jackielinn.server.common.RestResponse;
import ynu.jackielinn.server.common.BaseController;
import ynu.jackielinn.server.dto.response.DashboardPlatformStatsVO;
import ynu.jackielinn.server.dto.response.DashboardTaskStatusStatsVO;
import ynu.jackielinn.server.service.DashboardService;

import java.util.Map;

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
            @ApiResponse(responseCode = "401", description = "未登录或 token 过期"),
            @ApiResponse(responseCode = "403", description = "非管理员无权限")
    })
    @GetMapping("/admin/platform-stats")
    public RestResponse<DashboardPlatformStatsVO> getPlatformStats() {
        DashboardPlatformStatsVO stats = dashboardService.getPlatformStats();
        return RestResponse.success(stats);
    }

    /**
     * 获取按算法分组的任务数量（算法名 -> 任务数），用于柱状图。
     *
     * @return Map 算法名 -> 任务数
     */
    @Operation(summary = "获取按算法统计任务数", description = "返回各算法的任务数量，用于平台概览柱状图")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功"),
            @ApiResponse(responseCode = "401", description = "未登录或 token 过期"),
            @ApiResponse(responseCode = "403", description = "非管理员无权限")
    })
    @GetMapping("/admin/tasks-by-algorithm")
    public RestResponse<Map<String, Long>> getTasksByAlgorithm() {
        Map<String, Long> data = dashboardService.getTasksByAlgorithm();
        return RestResponse.success(data);
    }

    /**
     * 获取任务状态分布统计（未开始、进行中、已完成、失败）。管理员全平台，普通用户仅本人。
     *
     * @param request 用于获取当前用户 id
     * @return DashboardTaskStatusStatsVO
     */
    @Operation(summary = "获取任务状态分布统计", description = "用于饼状图；已完成=SUCCESS+RECOMMENDED；管理员全平台，普通用户本人")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功"),
            @ApiResponse(responseCode = "401", description = "未登录或 token 过期")
    })
    @GetMapping("/task-status-stats")
    public RestResponse<DashboardTaskStatusStatsVO> getTaskStatusStats(HttpServletRequest request) {
        Long uid = (Long) request.getAttribute("id");
        if (uid == null) {
            return RestResponse.failure(401, "未登录或登录已过期");
        }
        DashboardTaskStatusStatsVO stats = dashboardService.getTaskStatusStats(uid, isAdmin());
        return RestResponse.success(stats);
    }
}
