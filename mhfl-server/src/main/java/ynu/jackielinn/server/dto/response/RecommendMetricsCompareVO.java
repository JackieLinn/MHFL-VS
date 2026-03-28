package ynu.jackielinn.server.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 推荐展示页-算法效果对比响应对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "推荐页算法效果对比响应对象")
public class RecommendMetricsCompareVO {

    @Schema(description = "数据集ID")
    private Long datasetId;

    @Schema(description = "对比列表（按控制器候选任务ID顺序返回）")
    private List<RecommendMetricsCompareItemVO> items;
}
