package ynu.jackielinn.server.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 推荐展示页客户端最新指标响应对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "推荐页客户端最新指标响应对象")
public class RecommendClientMetricsVO {

    @Schema(description = "数据集ID")
    private Long datasetId;

    @Schema(description = "当前指标名称（accuracy/precision/recall/f1）")
    private String metric;

    @Schema(description = "算法名称列表（顺序与候选任务ID一致）")
    private List<String> algorithmNames;

    @Schema(description = "客户端指标列表")
    private List<RecommendClientMetricItemVO> clients;
}

