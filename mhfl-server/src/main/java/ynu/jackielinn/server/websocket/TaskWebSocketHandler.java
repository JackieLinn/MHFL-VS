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
import ynu.jackielinn.server.common.Status;
import ynu.jackielinn.server.entity.Task;
import ynu.jackielinn.server.service.RedisSubscriptionService;
import ynu.jackielinn.server.service.TaskService;
import ynu.jackielinn.server.utils.JwtUtils;

import java.util.Map;
import java.util.Set;

/**
 * 任务监控 WebSocket 处理器。
 * 连接路径 /ws/task/{taskId}；建立连接后等首包携带 token，校验 JWT 与权限（任务所有者或管理员）通过后加入会话并订阅 Redis，
 * 断开时移除会话，无剩余连接且任务终态时取消订阅。
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

    /**
     * 连接建立后从路径解析 taskId，写入 session 属性并标记未认证，等待前端首包携带 token。
     *
     * @param session 当前 WebSocket 会话
     * @throws Exception 关闭会话等可能抛出的异常
     */
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

    /**
     * 连接关闭时若已认证则从会话管理移除；若该任务下无剩余连接且任务已终态则取消 Redis 订阅。
     *
     * @param session 当前 WebSocket 会话
     * @param status  关闭状态
     * @throws Exception 可能抛出的异常
     */
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
                Task task = taskService.getById(taskId);
                if (task != null && isTerminalStatus(task.getStatus())) {
                    subscriptionService.unsubscribeTask(taskId);
                }
            }
        }
        log.info("WebSocket closed for task {}, authenticated: {}, status: {}", taskId, authenticated, status);
    }

    /**
     * 处理首包文本消息：解析 JSON 中的 token，校验 JWT 及任务权限（所有者或管理员），
     * 通过后标记已认证、加入会话管理并订阅该 taskId 的 Redis 通道。
     *
     * @param session 当前 WebSocket 会话
     * @param message 文本消息（约定 {"token": "eyJ..."}）
     * @throws Exception 关闭会话或解析可能抛出的异常
     */
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
     * 从首包 JSON 中解析 token 字段，约定格式：{"token": "eyJ..."}。
     *
     * @param payload 首包文本内容（JSON 字符串）
     * @return token 字符串，解析失败或不存在返回 null
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

    /**
     * 从 WebSocket 连接路径 /ws/task/{taskId} 中解析 taskId。
     *
     * @param session 当前 WebSocket 会话
     * @return taskId，解析失败返回 null
     */
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

    /**
     * 判断任务状态是否为终态（SUCCESS、RECOMMENDED、FAILED、CANCELLED），用于决定是否可取消 Redis 订阅。
     *
     * @param status 任务状态
     * @return 是否为终态
     */
    private static boolean isTerminalStatus(Status status) {
        return status == Status.SUCCESS || status == Status.RECOMMENDED
                || status == Status.FAILED || status == Status.CANCELLED;
    }
}
