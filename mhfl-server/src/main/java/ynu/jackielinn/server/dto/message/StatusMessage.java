package ynu.jackielinn.server.dto.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Redis 训练状态消息体
 * Python 发布至 task:experiment:status:{taskId}，包含 taskId、status、message、timestamp。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Redis 训练状态消息（Python 发布）")
public class StatusMessage {

    @JsonProperty("taskId")
    @Schema(description = "任务ID")
    private Long taskId;

    @Schema(description = "状态：IN_PROGRESS / SUCCESS / FAILED / CANCELLED")
    private String status;

    @Schema(description = "状态描述")
    private String message;

    @Schema(description = "时间戳（ISO 格式）")
    private String timestamp;
}
