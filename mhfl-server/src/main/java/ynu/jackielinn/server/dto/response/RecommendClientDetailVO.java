package ynu.jackielinn.server.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 推荐页客户端详情响应对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "推荐页客户端详情响应对象")
public class RecommendClientDetailVO {

    @Schema(description = "数据集ID")
    private Long datasetId;

    @Schema(description = "客户端索引，范围 0~numNodes-1")
    private Integer clientIndex;

    @Schema(description = "当前指标名称（accuracy/precision/recall/f1）")
    private String metric;

    @Schema(description = "轮次列表（从 1 开始）")
    private List<Integer> rounds;

    @Schema(description = "六个算法的客户端指标曲线")
    private List<RecommendClientDetailAlgorithmVO> algorithms;
}

