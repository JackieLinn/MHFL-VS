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
import ynu.jackielinn.server.dto.response.RecommendClientMetricsVO;
import ynu.jackielinn.server.dto.response.RecommendClientDetailVO;
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
     */
    private static final List<Long> CIFAR100_TASK_IDS = List.of(1L, 2L, 3L, 4L, 5L, 13L);

    /**
     * Tiny-ImageNet 推荐任务ID列表（按前端算法展示顺序配置）。
     */
    private static final List<Long> TINY_IMAGENET_TASK_IDS = List.of(6L, 8L, 9L, 10L, 11L, 14L);

    @Resource
    private RecommendService recommendService;

    @Resource
    private DatasetService datasetService;

    /**
     * 推荐页实验设置接口。
     *
     * @param datasetId 数据集ID
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
     *
     * @param datasetId 数据集ID
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
     *
     * @param datasetId 数据集ID
     * @param sigma 高斯平滑 sigma，范围 0~5，默认 2.5
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
     * 推荐页客户端最新指标接口（按单一指标）。
     *
     * @param datasetId 数据集ID
     * @param metric 指标名称，仅支持 accuracy/precision/recall/f1
     * @return 客户端指标数据
     */
    @Operation(summary = "推荐页客户端最新指标接口", description = "根据 datasetId 与 metric 返回推荐页客户端卡片所需最新指标")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功"),
            @ApiResponse(responseCode = "400", description = "datasetId 无效、数据集不存在或 metric 非法")
    })
    @GetMapping("/clients-metrics")
    public RestResponse<RecommendClientMetricsVO> getClientMetrics(
            @Parameter(description = "数据集ID", required = true) @RequestParam Long datasetId,
            @Parameter(description = "指标名称：accuracy/precision/recall/f1", required = true) @RequestParam String metric) {
        Dataset dataset = datasetService.getById(datasetId);
        if (dataset == null) {
            return RestResponse.failure(400, "数据集不存在");
        }
        List<Long> candidateTaskIds = resolveTaskIdsByDataset(dataset);
        try {
            RecommendClientMetricsVO vo = recommendService.getClientMetrics(datasetId, candidateTaskIds, metric);
            return RestResponse.success(vo);
        } catch (IllegalArgumentException ex) {
            return RestResponse.failure(400, ex.getMessage());
        }
    }

    /**
     * 推荐页客户端详情接口（原始曲线，不做平滑）。
     *
     * @param datasetId 数据集ID
     * @param clientIndex 客户端索引
     * @param metric 指标名称，仅支持 accuracy/precision/recall/f1
     * @return 客户端详情曲线数据
     */
    @Operation(summary = "推荐页客户端详情接口", description = "根据 datasetId、clientIndex、metric 返回客户端详情原始指标曲线")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功"),
            @ApiResponse(responseCode = "400", description = "参数非法或数据集不存在")
    })
    @GetMapping("/client-detail")
    public RestResponse<RecommendClientDetailVO> getClientDetail(
            @Parameter(description = "数据集ID", required = true) @RequestParam Long datasetId,
            @Parameter(description = "客户端索引，范围 0~numNodes-1", required = true) @RequestParam Integer clientIndex,
            @Parameter(description = "指标名称：accuracy/precision/recall/f1", required = true) @RequestParam String metric) {
        Dataset dataset = datasetService.getById(datasetId);
        if (dataset == null) {
            return RestResponse.failure(400, "数据集不存在");
        }
        if (clientIndex == null || clientIndex < 0) {
            return RestResponse.failure(400, "clientIndex 非法");
        }
        List<Long> candidateTaskIds = resolveTaskIdsByDataset(dataset);
        try {
            RecommendClientDetailVO vo = recommendService.getClientDetail(datasetId, candidateTaskIds, clientIndex, metric);
            return RestResponse.success(vo);
        } catch (IllegalArgumentException ex) {
            return RestResponse.failure(400, ex.getMessage());
        }
    }

    /**
     * 按数据集名称选择控制器中的预置任务ID列表。
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
