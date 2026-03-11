package ynu.jackielinn.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import ynu.jackielinn.server.dto.response.DashboardPlatformStatsVO;
import ynu.jackielinn.server.dto.response.DashboardTaskStatusStatsVO;
import ynu.jackielinn.server.dto.response.DashboardTaskTrendVO;
import ynu.jackielinn.server.common.Status;
import ynu.jackielinn.server.entity.Algorithm;
import ynu.jackielinn.server.entity.Task;
import ynu.jackielinn.server.service.AccountService;
import ynu.jackielinn.server.service.AlgorithmService;
import ynu.jackielinn.server.service.DashboardService;
import ynu.jackielinn.server.service.DatasetService;
import ynu.jackielinn.server.service.TaskService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardServiceImpl implements DashboardService {

    @Resource
    private AccountService accountService;

    @Resource
    private TaskService taskService;

    @Resource
    private DatasetService datasetService;

    @Resource
    private AlgorithmService algorithmService;

    /**
     * 获取平台概览统计。分别 count 各表，@TableLogic 自动排除已逻辑删除记录。
     */
    @Override
    public DashboardPlatformStatsVO getPlatformStats() {
        long totalUsers = accountService.count();
        long totalTasks = taskService.count();
        long totalDatasets = datasetService.count();
        long totalAlgorithms = algorithmService.count();
        return DashboardPlatformStatsVO.builder()
                .totalUsers(totalUsers)
                .totalTasks(totalTasks)
                .totalDatasets(totalDatasets)
                .totalAlgorithms(totalAlgorithms)
                .build();
    }

    /**
     * 获取按算法分组的任务数量。遍历算法表，按 aid 统计 task 数量，保持算法列表顺序。
     */
    @Override
    public Map<String, Long> getTasksByAlgorithm() {
        List<Algorithm> algorithms = algorithmService.list();
        Map<String, Long> result = new LinkedHashMap<>();
        for (Algorithm a : algorithms) {
            long cnt = taskService.count(new LambdaQueryWrapper<Task>().eq(Task::getAid, a.getId()));
            result.put(a.getAlgorithmName(), cnt);
        }
        return result;
    }

    /**
     * 获取任务状态分布统计。非管理员仅查 uid，管理员查全部。已完成 = SUCCESS + RECOMMENDED。
     *
     * @param uid     当前用户 id
     * @param isAdmin 是否为管理员
     * @return DashboardTaskStatusStatsVO
     */
    @Override
    public DashboardTaskStatusStatsVO getTaskStatusStats(Long uid, boolean isAdmin) {
        long notStarted = taskService.count(taskBaseWrapper(uid, isAdmin).eq(Task::getStatus, Status.NOT_STARTED));
        long inProgress = taskService.count(taskBaseWrapper(uid, isAdmin).eq(Task::getStatus, Status.IN_PROGRESS));
        long completed = taskService.count(taskBaseWrapper(uid, isAdmin).in(Task::getStatus, Status.SUCCESS, Status.RECOMMENDED));
        long failed = taskService.count(taskBaseWrapper(uid, isAdmin).eq(Task::getStatus, Status.FAILED));
        return DashboardTaskStatusStatsVO.builder()
                .notStarted(notStarted)
                .inProgress(inProgress)
                .completed(completed)
                .failed(failed)
                .build();
    }

    /**
     * 获取近 7 天任务趋势。遍历 7 天，每天 count create_time 在当天 00:00:00～23:59:59 的任务。
     *
     * @param uid     当前用户 id
     * @param isAdmin 是否为管理员
     * @return DashboardTaskTrendVO
     */
    @Override
    public DashboardTaskTrendVO getTaskTrend7Days(Long uid, boolean isAdmin) {
        LocalDate today = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");
        List<String> dates = new ArrayList<>(7);
        List<Long> counts = new ArrayList<>(7);
        for (int i = 6; i >= 0; i--) {
            LocalDate dayStart = today.minusDays(i);
            dates.add(dayStart.format(fmt));
            long cnt = taskService.count(
                    taskBaseWrapper(uid, isAdmin)
                            .ge(Task::getCreateTime, dayStart.atStartOfDay())
                            .le(Task::getCreateTime, dayStart.atTime(23, 59, 59))
            );
            counts.add(cnt);
        }
        return DashboardTaskTrendVO.builder().dates(dates).counts(counts).build();
    }

    private LambdaQueryWrapper<Task> taskBaseWrapper(Long uid, boolean isAdmin) {
        LambdaQueryWrapper<Task> w = new LambdaQueryWrapper<>();
        if (!isAdmin) {
            w.eq(Task::getUid, uid);
        }
        return w;
    }
}
