package ynu.jackielinn.server.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 轮次信息响应对象，用于任务历史曲线数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "轮次信息响应对象")
public class RoundVO {

    @Schema(description = "轮次记录ID")
    private Long id;

    @Schema(description = "轮次编号")
    private Integer roundNum;

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
