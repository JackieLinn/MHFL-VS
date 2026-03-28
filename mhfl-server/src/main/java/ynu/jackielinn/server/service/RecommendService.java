package ynu.jackielinn.server.service;

import ynu.jackielinn.server.dto.response.RecommendClientMetricsVO;
import ynu.jackielinn.server.dto.response.RecommendExperimentSettingsVO;
import ynu.jackielinn.server.dto.response.RecommendMetricsCompareVO;
import ynu.jackielinn.server.dto.response.RecommendTestCurvesVO;

import java.util.List;

public interface RecommendService {

    /**
     * 查询推荐展示页实验设置。
     *
     * @param datasetId 数据集ID
     * @param candidateTaskIds 控制器中配置的候选任务ID列表
     * @return 实验设置响应对象
     */
    RecommendExperimentSettingsVO getExperimentSettings(Long datasetId, List<Long> candidateTaskIds);

    /**
     * 查询推荐展示页算法效果对比数据。
     *
     * @param datasetId 数据集ID
     * @param candidateTaskIds 控制器中配置的候选任务ID列表
     * @return 算法效果对比响应对象
     */
    RecommendMetricsCompareVO getMetricsCompare(Long datasetId, List<Long> candidateTaskIds);

    /**
     * 查询推荐展示页测试集曲线数据。
     *
     * @param datasetId 数据集ID
     * @param candidateTaskIds 控制器中配置的候选任务ID列表
     * @param sigma 高斯平滑 sigma
     * @return 测试集曲线响应对象
     */
    RecommendTestCurvesVO getTestCurves(Long datasetId, List<Long> candidateTaskIds, Double sigma);

    /**
     * 查询推荐展示页客户端最新指标（按单一指标）。
     *
     * @param datasetId 数据集ID
     * @param candidateTaskIds 控制器中配置的候选任务ID列表
     * @param metric 指标名称，仅支持 accuracy/precision/recall/f1
     * @return 客户端指标响应对象
     */
    RecommendClientMetricsVO getClientMetrics(Long datasetId, List<Long> candidateTaskIds, String metric);
}

