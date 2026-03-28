package ynu.jackielinn.server.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 推荐页客户端单项指标对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "推荐页客户端单项指标对象")
public class RecommendClientMetricItemVO {

    @Schema(description = "客户端索引，范围 0~numNodes-1")
    private Integer clientIndex;

    @Schema(description = "六个算法在该客户端上的当前指标值（按算法顺序对齐，缺失为 null）")
    private List<Double> values;
}

