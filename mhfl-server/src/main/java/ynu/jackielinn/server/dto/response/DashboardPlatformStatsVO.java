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
@Schema(description = "平台概览统计响应对象")
public class DashboardPlatformStatsVO {

    @Schema(description = "用户总数")
    private Long totalUsers;

    @Schema(description = "任务总数")
    private Long totalTasks;

    @Schema(description = "数据集总数")
    private Long totalDatasets;

    @Schema(description = "算法总数")
    private Long totalAlgorithms;
}
