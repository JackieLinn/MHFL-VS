package ynu.jackielinn.server.websocket;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import ynu.jackielinn.server.service.RedisSubscriptionService;

import java.util.Set;

/**
 * 任务监控 WebSocket 处理器。
 * 连接路径：/ws/task/{taskId}。建立连接后注册会话并订阅该任务 Redis 通道；
 * 断开时移除会话，若无其他连接则取消订阅。
 * 权限校验（JWT、任务所有者/管理员/RECOMMENDED）待后续在握手或首包中实现。
 */
@Slf4j
@Component
public class TaskWebSocketHandler extends TextWebSocketHandler {

    @Resource
    WebSocketSessionManager sessionManager;

    @Resource
    RedisSubscriptionService subscriptionService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long taskId = extractTaskId(session);
        if (taskId == null) {
            session.close(CloseStatus.BAD_DATA.withReason("Invalid taskId"));
            return;
        }

        // TODO: 校验 JWT 与权限（任务所有者 / 管理员 / RECOMMENDED 任务可见）
        // 可从前端在连接 URL 上带 ?token=xxx 或首条消息传 token，校验后决定是否 close

        session.getAttributes().put("taskId", taskId);
        sessionManager.addSession(taskId, session);
        subscriptionService.subscribeTask(taskId);
        log.info("WebSocket connected for task {}", taskId);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long taskId = (Long) session.getAttributes().get("taskId");
        if (taskId == null) {
            taskId = extractTaskId(session);
        }
        if (taskId == null) {
            return;
        }
        sessionManager.removeSession(taskId, session);
        Set<WebSocketSession> remaining = sessionManager.getSessions(taskId);
        if (remaining.isEmpty()) {
            subscriptionService.unsubscribeTask(taskId);
        }
        log.info("WebSocket closed for task {}, status: {}", taskId, status);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 可选：前端发心跳（如 "ping"），此处回 "pong" 保活；暂无则忽略
    }

    private Long extractTaskId(WebSocketSession session) {
        if (session.getUri() == null) {
            return null;
        }
        String path = session.getUri().getPath();
        if (path == null) {
            return null;
        }
        String[] parts = path.split("/");
        // /ws/task/{taskId} => ["", "ws", "task", "{taskId}"]
        if (parts.length >= 4 && "task".equals(parts[2])) {
            try {
                return Long.parseLong(parts[3]);
            } catch (NumberFormatException e) {
                log.warn("Invalid taskId in path: {}", path);
            }
        }
        return null;
    }
}
