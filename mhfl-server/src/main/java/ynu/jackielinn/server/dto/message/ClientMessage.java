package ynu.jackielinn.server.dto.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Redis 客户端指标消息（Python 发布）")
public class ClientMessage {

    @JsonProperty("taskId")
    @Schema(description = "任务ID")
    private Long taskId;

    @JsonProperty("roundNum")
    @Schema(description = "轮次编号")
    private Integer roundNum;

    @JsonProperty("clientIndex")
    @Schema(description = "客户端索引")
    private Integer clientIndex;

    @Schema(description = "损失")
    private Double loss;

    @Schema(description = "准确率")
    private Double accuracy;

    @Schema(description = "精确率")
    private Double precision;

    @Schema(description = "召回率")
    private Double recall;

    @JsonProperty("f1Score")
    @Schema(description = "F1 分数")
    private Double f1Score;

    @Schema(description = "时间戳（ISO 格式）")
    private String timestamp;
}
