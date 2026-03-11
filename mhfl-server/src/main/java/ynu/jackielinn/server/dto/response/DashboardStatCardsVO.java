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
@Schema(description = "Dashboard 统计卡片响应对象")
public class DashboardStatCardsVO {

    @Schema(description = "任务总数（该用户/全平台全部状态）")
    private Long total;

    @Schema(description = "进行中任务数")
    private Long running;

    @Schema(description = "已完成任务数（SUCCESS + RECOMMENDED）")
    private Long success;

    @Schema(description = "今日创建任务数")
    private Long today;
}
