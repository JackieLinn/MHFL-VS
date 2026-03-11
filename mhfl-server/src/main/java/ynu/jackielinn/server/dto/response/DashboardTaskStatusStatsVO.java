package ynu.jackielinn.server.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "任务状态分布统计响应对象")
public class DashboardTaskStatusStatsVO {

    @Schema(description = "未开始任务数")
    private Long notStarted;

    @Schema(description = "进行中任务数")
    private Long inProgress;

    @Schema(description = "已完成任务数（SUCCESS + RECOMMENDED）")
    private Long completed;

    @Schema(description = "失败任务数")
    private Long failed;
}
