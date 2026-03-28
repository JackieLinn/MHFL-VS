package ynu.jackielinn.server.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 推荐展示页-算法效果对比单列响应对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "推荐页算法效果对比单列对象")
public class RecommendMetricsCompareItemVO {

    @Schema(description = "任务ID；若该位置任务未命中则为 null")
    private Long taskId;

    @Schema(description = "算法名称")
    private String algorithmName;

    @Schema(description = "loss")
    private Double loss;

    @Schema(description = "accuracy")
    private Double accuracy;

    @Schema(description = "precision")
    private Double precision;

    @Schema(description = "recall")
    private Double recall;

    @Schema(description = "f1Score")
    private Double f1Score;
}
