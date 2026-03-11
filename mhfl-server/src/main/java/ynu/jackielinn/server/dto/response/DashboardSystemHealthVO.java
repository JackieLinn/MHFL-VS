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
@Schema(description = "系统健康检查响应（MySQL、Redis、RabbitMQ、FastAPI）")
public class DashboardSystemHealthVO {

    @Schema(description = "MySQL 是否健康")
    private boolean mysql;

    @Schema(description = "Redis 是否健康")
    private boolean redis;

    @Schema(description = "RabbitMQ 是否健康")
    private boolean rabbitmq;

    @Schema(description = "FastAPI 是否健康")
    private boolean fastapi;
}
