package ynu.jackielinn.server.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 推荐展示页-测试集曲线响应对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "推荐页测试集曲线响应对象")
public class RecommendTestCurvesVO {

    @Schema(description = "数据集ID")
    private Long datasetId;

    @Schema(description = "横轴轮次标签（1开始）")
    private List<Integer> rounds;

    @Schema(description = "算法曲线列表（按控制器候选任务ID顺序返回）")
    private List<RecommendCurveAlgorithmVO> algorithms;
}
