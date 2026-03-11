package ynu.jackielinn.server.service.impl;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import ynu.jackielinn.server.dto.response.DashboardPlatformStatsVO;
import ynu.jackielinn.server.service.AccountService;
import ynu.jackielinn.server.service.AlgorithmService;
import ynu.jackielinn.server.service.DashboardService;
import ynu.jackielinn.server.service.DatasetService;
import ynu.jackielinn.server.service.TaskService;

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
}
