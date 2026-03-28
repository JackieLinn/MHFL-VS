package ynu.jackielinn.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import ynu.jackielinn.server.common.Status;
import ynu.jackielinn.server.dto.response.RecommendExperimentSettingsVO;
import ynu.jackielinn.server.dto.response.RecommendMetricsCompareItemVO;
import ynu.jackielinn.server.dto.response.RecommendMetricsCompareVO;
import ynu.jackielinn.server.entity.Algorithm;
import ynu.jackielinn.server.entity.Task;
import ynu.jackielinn.server.service.AlgorithmService;
import ynu.jackielinn.server.service.RecommendService;
import ynu.jackielinn.server.service.TaskService;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class RecommendServiceImpl implements RecommendService {

    @Resource
    private TaskService taskService;

    @Resource
    private AlgorithmService algorithmService;

    /**
     * 查询推荐展示页实验设置。
     * 从候选任务ID中按顺序选取第一条“存在 + RECOMMENDED + 属于指定数据集”的任务作为设置来源；
     * 算法名称从算法表全量读取（按 id 升序），便于前端按顺序绑定颜色。
     *
     * @param datasetId        数据集ID
     * @param candidateTaskIds 候选任务ID列表（由控制器维护）
     * @return 实验设置响应对象
     */
    @Override
    public RecommendExperimentSettingsVO getExperimentSettings(Long datasetId, List<Long> candidateTaskIds) {
        List<Long> validTaskIds = candidateTaskIds == null
                ? Collections.emptyList()
                : candidateTaskIds.stream().filter(Objects::nonNull).distinct().toList();

        List<Task> taskList = validTaskIds.isEmpty()
                ? List.of()
                : taskService.list(new LambdaQueryWrapper<Task>()
                .in(Task::getId, validTaskIds)
                .eq(Task::getDid, datasetId)
                .eq(Task::getStatus, Status.RECOMMENDED));

        Map<Long, Task> taskById = taskList.stream()
                .collect(Collectors.toMap(Task::getId, t -> t, (a, b) -> a, LinkedHashMap::new));

        Task sourceTask = null;
        for (Long id : validTaskIds) {
            Task t = taskById.get(id);
            if (t != null) {
                sourceTask = t;
                break;
            }
        }

        List<String> algorithmNames = algorithmService.list(
                        new LambdaQueryWrapper<Algorithm>().orderByAsc(Algorithm::getId))
                .stream()
                .map(Algorithm::getAlgorithmName)
                .toList();

        RecommendExperimentSettingsVO.RecommendExperimentSettingsVOBuilder builder = RecommendExperimentSettingsVO.builder()
                .datasetId(datasetId)
                .algorithmNames(algorithmNames);

        if (sourceTask != null) {
            builder.sourceTaskId(sourceTask.getId())
                    .numNodes(sourceTask.getNumNodes())
                    .fraction(sourceTask.getFraction())
                    .classesPerNode(sourceTask.getClassesPerNode())
                    .lowProb(sourceTask.getLowProb())
                    .numSteps(sourceTask.getNumSteps())
                    .epochs(sourceTask.getEpochs());
        }

        return builder.build();
    }

    /**
     * 查询推荐展示页算法效果对比数据。
     * 按候选任务ID顺序返回六列数据：命中“存在 + RECOMMENDED + did匹配”的任务则填入指标，未命中返回空占位。
     *
     * @param datasetId        数据集ID
     * @param candidateTaskIds 候选任务ID列表（由控制器维护）
     * @return 算法效果对比响应对象
     */
    @Override
    public RecommendMetricsCompareVO getMetricsCompare(Long datasetId, List<Long> candidateTaskIds) {
        List<Long> validTaskIds = candidateTaskIds == null
                ? Collections.emptyList()
                : candidateTaskIds.stream().filter(Objects::nonNull).distinct().toList();

        List<Task> taskList = validTaskIds.isEmpty()
                ? List.of()
                : taskService.list(new LambdaQueryWrapper<Task>()
                .in(Task::getId, validTaskIds)
                .eq(Task::getDid, datasetId)
                .eq(Task::getStatus, Status.RECOMMENDED));

        Map<Long, Task> taskById = taskList.stream()
                .collect(Collectors.toMap(Task::getId, t -> t, (a, b) -> a, LinkedHashMap::new));

        List<Long> aidList = taskList.stream()
                .map(Task::getAid)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, String> algorithmNameByAid = aidList.isEmpty()
                ? Collections.emptyMap()
                : algorithmService.listByIds(aidList).stream()
                .collect(Collectors.toMap(Algorithm::getId, Algorithm::getAlgorithmName));

        List<RecommendMetricsCompareItemVO> items = validTaskIds.stream()
                .map(taskId -> {
                    Task task = taskById.get(taskId);
                    if (task == null) {
                        return RecommendMetricsCompareItemVO.builder()
                                .taskId(null)
                                .algorithmName(null)
                                .loss(null)
                                .accuracy(null)
                                .precision(null)
                                .recall(null)
                                .f1Score(null)
                                .build();
                    }
                    return RecommendMetricsCompareItemVO.builder()
                            .taskId(task.getId())
                            .algorithmName(algorithmNameByAid.get(task.getAid()))
                            .loss(task.getLoss())
                            .accuracy(task.getAccuracy())
                            .precision(task.getPrecision())
                            .recall(task.getRecall())
                            .f1Score(task.getF1Score())
                            .build();
                })
                .toList();

        return RecommendMetricsCompareVO.builder()
                .datasetId(datasetId)
                .items(items)
                .build();
    }
}
