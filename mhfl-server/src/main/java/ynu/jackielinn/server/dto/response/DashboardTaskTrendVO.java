package ynu.jackielinn.server.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "近7天任务趋势响应对象")
public class DashboardTaskTrendVO {

    @Schema(description = "日期列表，格式 MM-dd，从 7 天前到今天")
    private List<String> dates;

    @Schema(description = "每日任务创建数，与 dates 一一对应")
    private List<Long> counts;
}
