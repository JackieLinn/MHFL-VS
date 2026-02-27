package ynu.jackielinn.server.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建任务结果：taskId + 是否从已有结果复制（copied=true 表示无需再训练）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "创建任务结果")
public class CreateTaskResultVO {

    @Schema(description = "新任务 ID")
    private Long taskId;

    @Schema(description = "是否从已有同配置任务复制结果（true 表示无需训练）")
    private Boolean copied;
}
