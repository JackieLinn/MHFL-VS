package ynu.jackielinn.server.service;

import ynu.jackielinn.server.dto.response.RecommendExperimentSettingsVO;
import ynu.jackielinn.server.dto.response.RecommendMetricsCompareVO;

import java.util.List;

public interface RecommendService {

    /**
     * 查询推荐展示页的实验设置。
     * 根据传入的候选任务ID列表，筛选“存在且状态为 RECOMMENDED 且属于该数据集”的任务，取第一条作为设置来源。
     * 同时返回算法表全部算法名称（按 id 升序）。
     *
     * @param datasetId       数据集ID（通过 query 参数传入）
     * @param candidateTaskIds 控制器中配置的候选任务ID列表
     * @return 推荐页实验设置响应对象
     */
    RecommendExperimentSettingsVO getExperimentSettings(Long datasetId, List<Long> candidateTaskIds);

    /**
     * 查询推荐展示页算法效果对比数据。
     * 仅使用“存在且状态为 RECOMMENDED 且属于该数据集”的任务；缺失任务按位置返回空占位，便于前端固定六列渲染。
     *
     * @param datasetId        数据集ID（通过 query 参数传入）
     * @param candidateTaskIds 控制器中配置的候选任务ID列表
     * @return 推荐页算法效果对比响应对象
     */
    RecommendMetricsCompareVO getMetricsCompare(Long datasetId, List<Long> candidateTaskIds);
}
