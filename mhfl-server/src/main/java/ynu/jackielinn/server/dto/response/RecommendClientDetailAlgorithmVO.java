package ynu.jackielinn.server.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 推荐页客户端详情中单个算法的折线数据对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "推荐页客户端详情-单算法折线数据对象")
public class RecommendClientDetailAlgorithmVO {

    @Schema(description = "任务ID，未命中时为 null")
    private Long taskId;

    @Schema(description = "算法名称")
    private String algorithmName;

    @Schema(description = "指标曲线值，按 rounds 对齐，缺失轮次为 null")
    private List<Double> values;
}

