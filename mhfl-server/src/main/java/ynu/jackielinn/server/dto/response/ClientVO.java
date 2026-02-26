package ynu.jackielinn.server.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 客户端记录响应对象。rid 不直接返回，用 roundNum（来自 Round 表）代替。
 * 未参与训练的客户端占位：roundNum=-1，五指标=-1，timestamp=null。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "客户端记录响应对象")
public class ClientVO {

    @Schema(description = "客户端记录ID，占位时为 null")
    private Long id;

    @Schema(description = "轮次编号（来自 Round.roundNum），无对应 Round 时为 -1")
    private Integer roundNum;

    @Schema(description = "客户端索引 0～numNodes-1")
    private Integer clientIndex;

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

    @Schema(description = "时间戳，占位时为 null")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime timestamp;
}
