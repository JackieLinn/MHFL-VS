package ynu.jackielinn.server.service;

import ynu.jackielinn.server.dto.response.DashboardPlatformStatsVO;

public interface DashboardService {

    /**
     * 获取平台概览统计（用户总数、任务总数、数据集总数、算法总数）。
     * 统计全平台未逻辑删除的数据。
     *
     * @return DashboardPlatformStatsVO
     */
    DashboardPlatformStatsVO getPlatformStats();
}
