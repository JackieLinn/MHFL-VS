package ynu.jackielinn.server.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 推荐展示页-测试集曲线中单个算法的曲线数据。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "推荐页测试集曲线-算法曲线对象")
public class RecommendCurveAlgorithmVO {

    @Schema(description = "任务ID；若该位置任务未命中则为 null")
    private Long taskId;

    @Schema(description = "算法名称")
    private String algorithmName;

    @Schema(description = "accuracy 原始曲线")
    private List<Double> accuracyRaw;

    @Schema(description = "precision 原始曲线")
    private List<Double> precisionRaw;

    @Schema(description = "recall 原始曲线")
    private List<Double> recallRaw;

    @Schema(description = "f1 原始曲线")
    private List<Double> f1Raw;

    @Schema(description = "accuracy 高斯平滑曲线（sigma=5）")
    private List<Double> accuracySmooth;

    @Schema(description = "precision 高斯平滑曲线（sigma=5）")
    private List<Double> precisionSmooth;

    @Schema(description = "recall 高斯平滑曲线（sigma=5）")
    private List<Double> recallSmooth;

    @Schema(description = "f1 高斯平滑曲线（sigma=5）")
    private List<Double> f1Smooth;
}
