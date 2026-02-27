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

    /**
     * 注册 WebSocket 处理器，将任务监控端点 /ws/task/{taskId} 与 TaskWebSocketHandler 绑定，
     * 并允许所有来源的跨域连接。
     *
     * @param registry WebSocketHandlerRegistry，用于注册处理器与路径
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(taskWebSocketHandler, "/ws/task/{taskId}")
                .setAllowedOriginPatterns("*");
    }
}
