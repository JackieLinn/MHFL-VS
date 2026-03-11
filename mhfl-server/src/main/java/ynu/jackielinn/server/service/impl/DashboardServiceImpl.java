package ynu.jackielinn.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import ynu.jackielinn.server.dto.response.DashboardPlatformStatsVO;
import ynu.jackielinn.server.entity.Algorithm;
import ynu.jackielinn.server.entity.Task;
import ynu.jackielinn.server.service.AccountService;
import ynu.jackielinn.server.service.AlgorithmService;
import ynu.jackielinn.server.service.DashboardService;
import ynu.jackielinn.server.service.DatasetService;
import ynu.jackielinn.server.service.TaskService;

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
}
