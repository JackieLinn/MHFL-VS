package ynu.jackielinn.server.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 会话管理：按 taskId 维护订阅该任务的会话，并支持向该任务推送消息。
 * 训练消息处理器在写库成功后调用 sendToTask 推送给前端。
 */
@Slf4j
@Component
public class WebSocketSessionManager {

    private final ConcurrentHashMap<Long, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void addSession(Long taskId, WebSocketSession session) {
        sessions.computeIfAbsent(taskId, k -> ConcurrentHashMap.newKeySet()).add(session);
        log.debug("WebSocket session added for task {}, total: {}", taskId, sessions.get(taskId).size());
    }

    public void removeSession(Long taskId, WebSocketSession session) {
        Set<WebSocketSession> taskSessions = sessions.get(taskId);
        if (taskSessions != null) {
            taskSessions.remove(session);
            if (taskSessions.isEmpty()) {
                sessions.remove(taskId);
            }
        }
    }

    /**
     * 向订阅了该 taskId 的所有会话推送消息（先写库后推送，由调用方保证顺序）
     */
    public void sendToTask(Long taskId, Object payload) {
        Set<WebSocketSession> taskSessions = sessions.get(taskId);
        if (taskSessions == null || taskSessions.isEmpty()) {
            return;
        }
        String text;
        try {
            text = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.warn("Serialize payload for task {} failed: {}", taskId, e.getMessage());
            return;
        }
        TextMessage message = new TextMessage(text);
        for (WebSocketSession session : taskSessions) {
            if (!session.isOpen()) {
                continue;
            }
            try {
                session.sendMessage(message);
            } catch (IOException e) {
                log.warn("Send to task {} session failed: {}", taskId, e.getMessage());
            }
        }
    }
}
