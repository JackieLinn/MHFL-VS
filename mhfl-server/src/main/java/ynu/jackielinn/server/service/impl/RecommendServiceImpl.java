package ynu.jackielinn.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import ynu.jackielinn.server.common.Status;
import ynu.jackielinn.server.dto.response.RecommendCurveAlgorithmVO;
import ynu.jackielinn.server.dto.response.RecommendExperimentSettingsVO;
import ynu.jackielinn.server.dto.response.RecommendMetricsCompareItemVO;
import ynu.jackielinn.server.dto.response.RecommendMetricsCompareVO;
import ynu.jackielinn.server.dto.response.RecommendTestCurvesVO;
import ynu.jackielinn.server.entity.Algorithm;
import ynu.jackielinn.server.entity.Round;
import ynu.jackielinn.server.entity.Task;
import ynu.jackielinn.server.service.AlgorithmService;
import ynu.jackielinn.server.service.RecommendService;
import ynu.jackielinn.server.service.RoundService;
import ynu.jackielinn.server.service.TaskService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RecommendServiceImpl implements RecommendService {

    /**
     * 曲线平滑使用的高斯核 sigma。
     */
    private static final double CURVE_SMOOTH_SIGMA = 2.5;

    @Resource
    private TaskService taskService;

    @Resource
    private AlgorithmService algorithmService;

    @Resource
    private RoundService roundService;

    /**
     * 查询推荐展示页实验设置。
     * 从候选任务 ID 中按顺序选择第一条满足条件的任务：
     * 1) 任务存在
     * 2) 任务状态为 RECOMMENDED
     * 3) 任务 did 与 datasetId 匹配
     * 同时返回算法表全部算法名称（按 id 升序）。
     *
     * @param datasetId 数据集 ID
     * @param candidateTaskIds 候选任务 ID 列表（由控制器维护）
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
     * 按候选任务 ID 顺序返回固定列数的数据；未命中的任务位返回空占位。
     *
     * @param datasetId 数据集 ID
     * @param candidateTaskIds 候选任务 ID 列表（由控制器维护）
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

    /**
     * 查询推荐展示页测试集曲线数据。
     * 返回每个算法的 raw 与 smooth 两套序列，其中 smooth 使用 sigma=5 的高斯平滑。
     *
     * @param datasetId 数据集 ID
     * @param candidateTaskIds 候选任务 ID 列表（由控制器维护）
     * @param sigma 高斯平滑 sigma（推荐范围 0~5）
     * @return 测试集曲线响应对象
     */
    @Override
    public RecommendTestCurvesVO getTestCurves(Long datasetId, List<Long> candidateTaskIds, Double sigma) {
        double actualSigma = normalizeSigma(sigma);
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

        int roundsCount = taskList.stream()
                .map(Task::getNumSteps)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0);

        if (roundsCount <= 0) {
            roundsCount = taskList.stream()
                    .map(Task::getId)
                    .map(roundService::listByTidOrderByRoundNum)
                    .filter(list -> list != null && !list.isEmpty())
                    .map(list -> list.stream()
                            .map(Round::getRoundNum)
                            .filter(Objects::nonNull)
                            .max(Integer::compareTo)
                            .orElse(-1) + 1)
                    .max(Integer::compareTo)
                    .orElse(0);
        }

        List<Integer> rounds = new ArrayList<>(Math.max(roundsCount, 0));
        for (int i = 0; i < roundsCount; i++) {
            rounds.add(i + 1);
        }

        final int finalRoundsCount = roundsCount;
        List<RecommendCurveAlgorithmVO> algorithms = validTaskIds.stream()
                .map(taskId -> {
                    Task task = taskById.get(taskId);
                    if (task == null) {
                        return emptyCurveItem(finalRoundsCount);
                    }

                    List<Round> roundList = roundService.listByTidOrderByRoundNum(task.getId());
                    List<Double> accuracyRaw = initSeriesFromRounds(roundList, finalRoundsCount, Round::getAccuracy);
                    List<Double> precisionRaw = initSeriesFromRounds(roundList, finalRoundsCount, Round::getPrecision);
                    List<Double> recallRaw = initSeriesFromRounds(roundList, finalRoundsCount, Round::getRecall);
                    List<Double> f1Raw = initSeriesFromRounds(roundList, finalRoundsCount, Round::getF1Score);

                    return RecommendCurveAlgorithmVO.builder()
                            .taskId(task.getId())
                            .algorithmName(algorithmNameByAid.get(task.getAid()))
                            .accuracyRaw(accuracyRaw)
                            .precisionRaw(precisionRaw)
                            .recallRaw(recallRaw)
                            .f1Raw(f1Raw)
                            .accuracySmooth(gaussianSmooth(accuracyRaw, actualSigma))
                            .precisionSmooth(gaussianSmooth(precisionRaw, actualSigma))
                            .recallSmooth(gaussianSmooth(recallRaw, actualSigma))
                            .f1Smooth(gaussianSmooth(f1Raw, actualSigma))
                            .build();
                })
                .toList();

        return RecommendTestCurvesVO.builder()
                .datasetId(datasetId)
                .rounds(rounds)
                .algorithms(algorithms)
                .build();
    }

    /**
     * 规范化 sigma 值。
     * 空值使用默认值 2.5；并夹紧到 [0, 5] 区间。
     *
     * @param sigma 前端传入的 sigma
     * @return 可用的 sigma
     */
    private double normalizeSigma(Double sigma) {
        if (sigma == null || !Double.isFinite(sigma)) {
            return CURVE_SMOOTH_SIGMA;
        }
        return Math.max(0.0, Math.min(5.0, sigma));
    }

    /**
     * 构造“任务未命中”时的空曲线占位对象。
     *
     * @param roundsCount 轮次数量
     * @return 空算法曲线对象
     */
    private RecommendCurveAlgorithmVO emptyCurveItem(int roundsCount) {
        List<Double> accuracy = createNullSeries(roundsCount);
        List<Double> precision = createNullSeries(roundsCount);
        List<Double> recall = createNullSeries(roundsCount);
        List<Double> f1 = createNullSeries(roundsCount);
        return RecommendCurveAlgorithmVO.builder()
                .taskId(null)
                .algorithmName(null)
                .accuracyRaw(accuracy)
                .precisionRaw(precision)
                .recallRaw(recall)
                .f1Raw(f1)
                .accuracySmooth(createNullSeries(roundsCount))
                .precisionSmooth(createNullSeries(roundsCount))
                .recallSmooth(createNullSeries(roundsCount))
                .f1Smooth(createNullSeries(roundsCount))
                .build();
    }

    /**
     * 创建固定长度的 null 序列。
     *
     * @param size 序列长度
     * @return 元素全为 null 的序列
     */
    private List<Double> createNullSeries(int size) {
        List<Double> series = new ArrayList<>(Math.max(size, 0));
        for (int i = 0; i < size; i++) {
            series.add(null);
        }
        return series;
    }

    /**
     * 将 Round 列表按 roundNum 映射为固定长度序列。
     * 缺失轮次保留 null。
     *
     * @param roundList Round 列表
     * @param roundsCount 目标序列长度
     * @param getter 指标提取函数
     * @return 指标序列
     */
    private List<Double> initSeriesFromRounds(List<Round> roundList, int roundsCount, Function<Round, Double> getter) {
        List<Double> series = new ArrayList<>(Math.max(roundsCount, 0));
        for (int i = 0; i < roundsCount; i++) {
            series.add(null);
        }
        if (roundList == null || roundList.isEmpty()) {
            return series;
        }
        for (Round round : roundList) {
            Integer r = round.getRoundNum();
            if (r == null || r < 0 || r >= roundsCount) {
                continue;
            }
            series.set(r, getter.apply(round));
        }
        return series;
    }

    /**
     * 高斯平滑（sigma 可配），忽略 null 点。
     * 当窗口内无有效值时返回 null。
     *
     * @param input 输入序列
     * @param sigma 高斯核 sigma
     * @return 平滑后的序列
     */
    private List<Double> gaussianSmooth(List<Double> input, double sigma) {
        if (input == null || input.isEmpty()) {
            return List.of();
        }
        if (sigma <= 0.0) {
            return new ArrayList<>(input);
        }
        int radius = Math.max(1, (int) Math.ceil(3 * sigma));
        List<Double> output = new ArrayList<>(input.size());

        for (int i = 0; i < input.size(); i++) {
            double numerator = 0.0;
            double denominator = 0.0;
            int left = Math.max(0, i - radius);
            int right = Math.min(input.size() - 1, i + radius);
            for (int j = left; j <= right; j++) {
                Double v = input.get(j);
                if (v == null) {
                    continue;
                }
                double dist = j - i;
                double weight = Math.exp(-(dist * dist) / (2.0 * sigma * sigma));
                numerator += weight * v;
                denominator += weight;
            }
            output.add(denominator > 0 ? numerator / denominator : null);
        }

        return output;
    }
}
