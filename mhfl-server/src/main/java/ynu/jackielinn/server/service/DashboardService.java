package ynu.jackielinn.server.service;

import ynu.jackielinn.server.dto.response.DashboardPlatformStatsVO;

import java.util.Map;

public interface DashboardService {

    /**
     * 获取平台概览统计（用户总数、任务总数、数据集总数、算法总数）。
     * 统计全平台未逻辑删除的数据。
     *
     * @return DashboardPlatformStatsVO
     */
    DashboardPlatformStatsVO getPlatformStats();

    /**
     * 获取按算法分组的任务数量。键为算法名，值为该算法的任务数。
     *
     * @return Map 算法名 -> 任务数
     */
    Map<String, Long> getTasksByAlgorithm();
}
