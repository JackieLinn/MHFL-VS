package ynu.jackielinn.server.service;

import ynu.jackielinn.server.dto.response.DashboardPlatformStatsVO;
import ynu.jackielinn.server.dto.response.DashboardStatCardsVO;
import ynu.jackielinn.server.dto.response.DashboardTaskStatusStatsVO;
import ynu.jackielinn.server.dto.response.DashboardTaskTrendVO;
import ynu.jackielinn.server.dto.response.TaskVO;

import java.util.List;
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

    /**
     * 获取任务状态分布统计（未开始、进行中、已完成、失败）。
     * 管理员统计全平台，普通用户仅统计本人任务。已完成 = SUCCESS + RECOMMENDED。
     *
     * @param uid     当前用户 id
     * @param isAdmin 是否为管理员
     * @return DashboardTaskStatusStatsVO
     */
    DashboardTaskStatusStatsVO getTaskStatusStats(Long uid, boolean isAdmin);

    /**
     * 获取近 7 天任务趋势（含今天）。按 task 表 create_time 统计每日创建数。
     * 管理员统计全平台，普通用户仅统计本人任务。
     *
     * @param uid     当前用户 id
     * @param isAdmin 是否为管理员
     * @return DashboardTaskTrendVO（dates + counts）
     */
    DashboardTaskTrendVO getTaskTrend7Days(Long uid, boolean isAdmin);

    /**
     * 获取统计卡片数据（总数、进行中、已完成、今日创建）。
     * 管理员统计全平台，普通用户仅统计本人任务。已完成 = SUCCESS + RECOMMENDED。
     *
     * @param uid     当前用户 id
     * @param isAdmin 是否为管理员
     * @return DashboardStatCardsVO
     */
    DashboardStatCardsVO getStatCards(Long uid, boolean isAdmin);

    /**
     * 最近任务列表，按 create_time 降序取前 8 条。管理员全平台，普通用户仅本人。
     *
     * @param uid     当前用户 id
     * @param isAdmin 是否为管理员
     * @return TaskVO 列表
     */
    List<TaskVO> getRecentTasks(Long uid, boolean isAdmin);
}
