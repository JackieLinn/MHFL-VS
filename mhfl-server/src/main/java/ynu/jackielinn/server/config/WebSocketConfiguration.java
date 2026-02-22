package ynu.jackielinn.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import ynu.jackielinn.server.websocket.TaskWebSocketHandler;

/**
 * WebSocket 配置：注册任务监控 WebSocket 端点 /ws/task/{taskId}。
 * 前端连接后可按 taskId 接收该任务的 Round/Client/Status 实时推送。
 */
@Configuration
@EnableWebSocket
public class WebSocketConfiguration implements WebSocketConfigurer {

    private final TaskWebSocketHandler taskWebSocketHandler;

    public WebSocketConfiguration(TaskWebSocketHandler taskWebSocketHandler) {
        this.taskWebSocketHandler = taskWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(taskWebSocketHandler, "/ws/task/{taskId}")
                .setAllowedOriginPatterns("*");
    }
}
