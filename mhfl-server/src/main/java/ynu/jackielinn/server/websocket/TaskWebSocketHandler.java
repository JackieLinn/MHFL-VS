package ynu.jackielinn.server.websocket;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import ynu.jackielinn.server.entity.Task;
import ynu.jackielinn.server.service.RedisSubscriptionService;
import ynu.jackielinn.server.service.TaskService;
import ynu.jackielinn.server.utils.JwtUtils;

import java.util.Map;
import java.util.Set;

/**
 * 任务监控 WebSocket 处理器。
 * 连接路径：/ws/task/{taskId}。建立连接后不立即加入会话，等前端首包携带 token；
 * 校验 JWT 与权限（任务所有者或管理员）通过后再 addSession 并订阅 Redis，之后推送训练消息。
 * 断开时若已认证则移除会话，若无其他连接则取消订阅。
 */
@Slf4j
@Component
public class TaskWebSocketHandler extends TextWebSocketHandler {

    private static final String ATTR_TASK_ID = "taskId";
    private static final String ATTR_AUTHENTICATED = "authenticated";
    private static final String AUTH_JSON_KEY_TOKEN = "token";

    @Resource
    WebSocketSessionManager sessionManager;

    @Resource
    RedisSubscriptionService subscriptionService;

    @Resource
    JwtUtils jwtUtils;

    @Resource
    TaskService taskService;

    @Resource
    ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long taskId = extractTaskId(session);
        if (taskId == null) {
            session.close(CloseStatus.BAD_DATA.withReason("Invalid taskId"));
            return;
        }
        session.getAttributes().put(ATTR_TASK_ID, taskId);
        session.getAttributes().put(ATTR_AUTHENTICATED, false);
        log.debug("WebSocket connection opened for task {}, awaiting auth message", taskId);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long taskId = (Long) session.getAttributes().get(ATTR_TASK_ID);
        if (taskId == null) {
            taskId = extractTaskId(session);
        }
        if (taskId == null) {
            return;
        }
        Boolean authenticated = (Boolean) session.getAttributes().get(ATTR_AUTHENTICATED);
        if (Boolean.TRUE.equals(authenticated)) {
            sessionManager.removeSession(taskId, session);
            Set<WebSocketSession> remaining = sessionManager.getSessions(taskId);
            if (remaining.isEmpty()) {
                subscriptionService.unsubscribeTask(taskId);
            }
        }
        log.info("WebSocket closed for task {}, authenticated: {}, status: {}", taskId, authenticated, status);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        if (Boolean.TRUE.equals(session.getAttributes().get(ATTR_AUTHENTICATED))) {
            return;
        }

        String token = parseTokenFromPayload(message.getPayload());
        if (token == null || token.isBlank()) {
            session.close(CloseStatus.POLICY_VIOLATION.withReason("Token required"));
            return;
        }

        DecodedJWT jwt = jwtUtils.resolveJwt("Bearer " + token);
        if (jwt == null) {
            session.close(CloseStatus.POLICY_VIOLATION.withReason("Invalid or expired token"));
            return;
        }

        Long taskId = (Long) session.getAttributes().get(ATTR_TASK_ID);
        if (taskId == null) {
            session.close(CloseStatus.BAD_DATA.withReason("Invalid taskId"));
            return;
        }

        Task task = taskService.getById(taskId);
        if (task == null) {
            session.close(CloseStatus.POLICY_VIOLATION.withReason("Task not found"));
            return;
        }

        Long userId = jwtUtils.toId(jwt);
        boolean isAdmin = jwtUtils.toUser(jwt).getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_admin"::equals);

        if (!task.getUid().equals(userId) && !isAdmin) {
            session.close(CloseStatus.POLICY_VIOLATION.withReason("No permission"));
            return;
        }

        session.getAttributes().put(ATTR_AUTHENTICATED, true);
        sessionManager.addSession(taskId, session);
        subscriptionService.subscribeTask(taskId);
        log.info("WebSocket authenticated for task {}, userId: {}", taskId, userId);
    }

    /**
     * 从首包 JSON 中解析 token 字段，约定格式：{"token": "eyJ..."}
     */
    private String parseTokenFromPayload(String payload) {
        if (payload == null || payload.isBlank()) {
            return null;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.readValue(payload, Map.class);
            Object v = map == null ? null : map.get(AUTH_JSON_KEY_TOKEN);
            return v instanceof String ? (String) v : null;
        } catch (Exception e) {
            log.debug("Failed to parse auth payload: {}", e.getMessage());
            return null;
        }
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
