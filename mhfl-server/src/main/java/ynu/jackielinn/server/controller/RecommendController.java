package ynu.jackielinn.server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ynu.jackielinn.server.common.RestResponse;
import ynu.jackielinn.server.dto.response.RecommendExperimentSettingsVO;
import ynu.jackielinn.server.dto.response.RecommendMetricsCompareVO;
import ynu.jackielinn.server.dto.response.RecommendTestCurvesVO;
import ynu.jackielinn.server.entity.Dataset;
import ynu.jackielinn.server.service.DatasetService;
import ynu.jackielinn.server.service.RecommendService;

import java.util.List;

@RestController
@RequestMapping("/api/recommended")
@Tag(name = "推荐页面操作接口", description = "推荐页面展示相关接口")
public class RecommendController {

    /**
     * CIFAR-100 推荐任务ID列表（按前端算法展示顺序配置）。
     * 后续可直接修改该列表中的数字。
     */
    private static final List<Long> CIFAR100_TASK_IDS = List.of(1L, 2L, 3L, 4L, 5L, 13L);

    /**
     * Tiny-ImageNet 推荐任务ID列表（按前端算法展示顺序配置）。
     * 后续可直接修改该列表中的数字。
     */
    private static final List<Long> TINY_IMAGENET_TASK_IDS = List.of(6L, 8L, 9L, 10L, 11L, 14L);

    @Resource
    private RecommendService recommendService;

    @Resource
    private DatasetService datasetService;

    /**
     * 推荐页实验设置接口。
     * 通过 datasetId 选择预置任务ID列表，仅使用“存在且状态为 RECOMMENDED 且 did 匹配”的任务；
     * 如果列表里任务尚未训练完成或不是推荐状态，会自动忽略。
     *
     * @param datasetId 数据集ID（query 参数）
     * @return 实验设置与算法名称列表
     */
    @Operation(summary = "推荐页实验设置接口", description = "根据 datasetId 返回推荐页实验设置与算法名称列表")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功"),
            @ApiResponse(responseCode = "400", description = "datasetId 无效或数据集不存在")
    })
    @GetMapping("/experiment-settings")
    public RestResponse<RecommendExperimentSettingsVO> getExperimentSettings(
            @Parameter(description = "数据集ID", required = true) @RequestParam Long datasetId) {
        Dataset dataset = datasetService.getById(datasetId);
        if (dataset == null) {
            return RestResponse.failure(400, "数据集不存在");
        }

        List<Long> candidateTaskIds = resolveTaskIdsByDataset(dataset);
        RecommendExperimentSettingsVO vo = recommendService.getExperimentSettings(datasetId, candidateTaskIds);
        return RestResponse.success(vo);
    }

    /**
     * 推荐页算法效果对比接口。
     * 通过 datasetId 选择预置任务ID列表，仅使用“存在且状态为 RECOMMENDED 且 did 匹配”的任务；
     * 未命中的任务位置返回空占位，前端可继续保持六列渲染。
     *
     * @param datasetId 数据集ID（query 参数）
     * @return 算法效果对比数据
     */
    @Operation(summary = "推荐页算法效果对比接口", description = "根据 datasetId 返回推荐页算法效果对比数据")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功"),
            @ApiResponse(responseCode = "400", description = "datasetId 无效或数据集不存在")
    })
    @GetMapping("/metrics-compare")
    public RestResponse<RecommendMetricsCompareVO> getMetricsCompare(
            @Parameter(description = "数据集ID", required = true) @RequestParam Long datasetId) {
        Dataset dataset = datasetService.getById(datasetId);
        if (dataset == null) {
            return RestResponse.failure(400, "数据集不存在");
        }

        List<Long> candidateTaskIds = resolveTaskIdsByDataset(dataset);
        RecommendMetricsCompareVO vo = recommendService.getMetricsCompare(datasetId, candidateTaskIds);
        return RestResponse.success(vo);
    }

    /**
     * 推荐页测试集曲线接口。
     * 返回每个算法的原始曲线与高斯平滑曲线（sigma=5）；前端展示平滑曲线，tooltip 显示原始值。
     *
     * @param datasetId 数据集ID（query 参数）
     * @return 测试集曲线数据
     */
    @Operation(summary = "推荐页测试集曲线接口", description = "根据 datasetId 返回推荐页测试集曲线（含原始值与平滑值）")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功"),
            @ApiResponse(responseCode = "400", description = "datasetId 无效或数据集不存在")
    })
    @GetMapping("/test-curves")
    public RestResponse<RecommendTestCurvesVO> getTestCurves(
            @Parameter(description = "数据集ID", required = true) @RequestParam Long datasetId,
            @Parameter(description = "高斯平滑 sigma，范围 0~5，默认 2.5") @RequestParam(defaultValue = "2.5") Double sigma) {
        Dataset dataset = datasetService.getById(datasetId);
        if (dataset == null) {
            return RestResponse.failure(400, "数据集不存在");
        }

        List<Long> candidateTaskIds = resolveTaskIdsByDataset(dataset);
        RecommendTestCurvesVO vo = recommendService.getTestCurves(datasetId, candidateTaskIds, sigma);
        return RestResponse.success(vo);
    }

    /**
     * 按数据集名称选择控制器中的预置任务ID列表。
     * 包含 tiny 的视为 Tiny-ImageNet，否则默认按 CIFAR-100 处理。
     *
     * @param dataset 数据集实体
     * @return 候选任务ID列表
     */
    private List<Long> resolveTaskIdsByDataset(Dataset dataset) {
        String name = dataset.getDataName();
        if (StringUtils.hasText(name) && name.toLowerCase().contains("tiny")) {
            return TINY_IMAGENET_TASK_IDS;
        }
        return CIFAR100_TASK_IDS;
    }
}
