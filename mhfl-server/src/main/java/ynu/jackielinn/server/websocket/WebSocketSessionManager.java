package ynu.jackielinn.server.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 会话管理器。
 * 按 taskId 维护订阅该任务的会话集合，提供添加/移除/推送/关闭；训练消息处理器写库后调用 sendToTask 推送给前端。
 */
@Slf4j
@Component
public class WebSocketSessionManager {

    private final ConcurrentHashMap<Long, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 将已认证的 WebSocket 会话加入指定任务下的会话集合。
     *
     * @param taskId  任务 id
     * @param session WebSocket 会话
     */
    public void addSession(Long taskId, WebSocketSession session) {
        sessions.computeIfAbsent(taskId, k -> ConcurrentHashMap.newKeySet()).add(session);
        log.debug("WebSocket session added for task {}, total: {}", taskId, sessions.get(taskId).size());
    }

    /**
     * 从指定任务下移除该 WebSocket 会话；若该任务无剩余会话则从 map 中移除 taskId。
     *
     * @param taskId  任务 id
     * @param session 要移除的 WebSocket 会话
     */
    public void removeSession(Long taskId, WebSocketSession session) {
        Set<WebSocketSession> taskSessions = sessions.get(taskId);
        if (taskSessions != null) {
            taskSessions.remove(session);
            if (taskSessions.isEmpty()) {
                sessions.remove(taskId);
            }
            log.debug("WebSocket session removed for task {}, remaining: {}", taskId, taskSessions.size());
        }
    }

    /**
     * 获取某任务当前所有会话的只读快照，用于判断是否可取消 Redis 订阅等。
     *
     * @param taskId 任务 id
     * @return 该任务下的会话集合，无会话时返回空 Set
     */
    public Set<WebSocketSession> getSessions(Long taskId) {
        Set<WebSocketSession> taskSessions = sessions.get(taskId);
        if (taskSessions == null || taskSessions.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(taskSessions);
    }

    /**
     * 向订阅了该 taskId 的所有会话推送消息；先写库后推送由调用方保证顺序，发送失败或已关闭的会话会被移除。
     *
     * @param taskId  任务 id
     * @param payload 要序列化为 JSON 并推送的对象
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
        for (WebSocketSession session : Set.copyOf(taskSessions)) {
            if (!session.isOpen()) {
                removeSession(taskId, session);
                continue;
            }
            try {
                session.sendMessage(message);
            } catch (IOException e) {
                log.warn("Send to task {} session {} failed: {}, removing session", taskId, session.getId(), e.getMessage());
                removeSession(taskId, session);
            }
        }
    }

    /**
     * 关闭某任务下所有 WebSocket 会话，用于停止训练后主动断开监控连接。
     *
     * @param taskId 任务 id
     */
    public void closeAllSessionsForTask(Long taskId) {
        Set<WebSocketSession> taskSessions = sessions.get(taskId);
        if (taskSessions == null || taskSessions.isEmpty()) {
            return;
        }
        for (WebSocketSession session : Set.copyOf(taskSessions)) {
            removeSession(taskId, session);
            if (session.isOpen()) {
                try {
                    session.close(CloseStatus.NORMAL.withReason("训练已停止"));
                } catch (IOException e) {
                    log.warn("Close session {} for task {} failed: {}", session.getId(), taskId, e.getMessage());
                }
            }
        }
    }
}
