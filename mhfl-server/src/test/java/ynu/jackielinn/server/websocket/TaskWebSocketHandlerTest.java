package ynu.jackielinn.server.websocket;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import ynu.jackielinn.server.common.Status;
import ynu.jackielinn.server.entity.Task;
import ynu.jackielinn.server.service.RedisSubscriptionService;
import ynu.jackielinn.server.service.TaskService;
import ynu.jackielinn.server.utils.JwtUtils;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * TaskWebSocketHandler 单元测试：连接建立/关闭、首包鉴权及权限分支。
 */
@ExtendWith(MockitoExtension.class)
class TaskWebSocketHandlerTest {

    @Mock
    private WebSocketSessionManager sessionManager;

    @Mock
    private RedisSubscriptionService subscriptionService;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private TaskService taskService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private TaskWebSocketHandler handler;

    @Mock
    private WebSocketSession session;

    @Mock
    private DecodedJWT decodedJwt;

    private final Map<String, Object> sessionAttrs = new HashMap<>();

    @BeforeEach
    void setUp() {
        // lenient: afterConnectionEstablishedShouldCloseWhenInvalidTaskId 中未调用 getAttributes
        lenient().when(session.getAttributes()).thenReturn(sessionAttrs);
        sessionAttrs.clear();
    }

    @AfterEach
    void tearDown() {
        sessionAttrs.clear();
    }

    @Test
    void afterConnectionEstablishedShouldCloseWhenInvalidTaskId() throws Exception {
        when(session.getUri()).thenReturn(URI.create("http://localhost/ws/task/abc"));

        handler.afterConnectionEstablished(session);

        verify(session).close(argThat(s -> s.getCode() == CloseStatus.BAD_DATA.getCode()));
    }

    @Test
    void afterConnectionEstablishedShouldCloseWhenUriNull() throws Exception {
        when(session.getUri()).thenReturn(null);

        handler.afterConnectionEstablished(session);

        verify(session).close(argThat(s -> s.getCode() == CloseStatus.BAD_DATA.getCode()));
    }

    @Test
    void afterConnectionEstablishedShouldCloseWhenPathNotMatching() throws Exception {
        when(session.getUri()).thenReturn(URI.create("http://localhost/other"));

        handler.afterConnectionEstablished(session);

        verify(session).close(argThat(s -> s.getCode() == CloseStatus.BAD_DATA.getCode()));
    }

    @Test
    void afterConnectionEstablishedShouldSetAttributesWhenValidPath() throws Exception {
        when(session.getUri()).thenReturn(URI.create("http://localhost/ws/task/1"));

        handler.afterConnectionEstablished(session);

        assertEquals(1L, sessionAttrs.get("taskId"));
        assertEquals(false, sessionAttrs.get("authenticated"));
        verify(session, never()).close(any());
    }

    @Test
    void afterConnectionClosedShouldDoNothingWhenTaskIdNull() throws Exception {
        when(session.getUri()).thenReturn(URI.create("http://localhost/other"));

        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        verify(sessionManager, never()).removeSession(anyLong(), any());
        verify(subscriptionService, never()).unsubscribeTask(anyLong());
    }

    @Test
    void afterConnectionClosedWhenAuthenticatedAndRemainingNotEmptyShouldNotUnsubscribe() throws Exception {
        sessionAttrs.put("taskId", 1L);
        sessionAttrs.put("authenticated", true);
        WebSocketSession other = mock(WebSocketSession.class);
        when(sessionManager.getSessions(1L)).thenReturn(Set.of(other));

        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        verify(sessionManager).removeSession(1L, session);
        verify(subscriptionService, never()).unsubscribeTask(anyLong());
    }

    @Test
    void afterConnectionClosedWhenAuthenticatedRemainingEmptyAndTaskTerminalShouldUnsubscribe() throws Exception {
        sessionAttrs.put("taskId", 1L);
        sessionAttrs.put("authenticated", true);
        when(sessionManager.getSessions(1L)).thenReturn(Set.of());
        Task task = new Task();
        task.setStatus(Status.SUCCESS);
        when(taskService.getById(1L)).thenReturn(task);

        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        verify(sessionManager).removeSession(1L, session);
        verify(subscriptionService).unsubscribeTask(1L);
    }

    @Test
    void afterConnectionClosedWhenRemainingEmptyButTaskNullShouldNotUnsubscribe() throws Exception {
        sessionAttrs.put("taskId", 1L);
        sessionAttrs.put("authenticated", true);
        when(sessionManager.getSessions(1L)).thenReturn(Set.of());
        when(taskService.getById(1L)).thenReturn(null);

        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        verify(sessionManager).removeSession(1L, session);
        verify(subscriptionService, never()).unsubscribeTask(anyLong());
    }

    @Test
    void afterConnectionClosedWhenRemainingEmptyButTaskNotTerminalShouldNotUnsubscribe() throws Exception {
        sessionAttrs.put("taskId", 1L);
        sessionAttrs.put("authenticated", true);
        when(sessionManager.getSessions(1L)).thenReturn(Set.of());
        Task task = new Task();
        task.setStatus(Status.IN_PROGRESS);
        when(taskService.getById(1L)).thenReturn(task);

        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        verify(sessionManager).removeSession(1L, session);
        verify(subscriptionService, never()).unsubscribeTask(anyLong());
    }

    @Test
    void handleTextMessageShouldReturnWhenAlreadyAuthenticated() throws Exception {
        sessionAttrs.put("taskId", 1L);
        sessionAttrs.put("authenticated", true);

        handler.handleTextMessage(session, new TextMessage("{\"token\":\"x\"}"));

        verify(session, never()).close(any());
        verify(sessionManager, never()).addSession(anyLong(), any());
    }

    @Test
    void handleTextMessageShouldCloseWhenTokenMissing() throws Exception {
        sessionAttrs.put("taskId", 1L);
        sessionAttrs.put("authenticated", false);

        handler.handleTextMessage(session, new TextMessage("{}"));

        verify(session).close(argThat(s -> s.getCode() == CloseStatus.POLICY_VIOLATION.getCode()));
        verify(sessionManager, never()).addSession(anyLong(), any());
    }

    @Test
    void handleTextMessageShouldCloseWhenTokenBlank() throws Exception {
        sessionAttrs.put("taskId", 1L);
        sessionAttrs.put("authenticated", false);
        when(objectMapper.readValue(anyString(), eq(Map.class))).thenReturn(Map.of("token", "   "));

        handler.handleTextMessage(session, new TextMessage("{\"token\":\"   \"}"));

        verify(session).close(argThat(s -> s.getCode() == CloseStatus.POLICY_VIOLATION.getCode()));
        verify(sessionManager, never()).addSession(anyLong(), any());
    }

    @Test
    void handleTextMessageShouldCloseWhenParseTokenThrows() throws Exception {
        sessionAttrs.put("taskId", 1L);
        sessionAttrs.put("authenticated", false);
        when(objectMapper.readValue(anyString(), eq(Map.class))).thenThrow(new RuntimeException("parse error"));

        handler.handleTextMessage(session, new TextMessage("invalid"));

        verify(session).close(argThat(s -> s.getCode() == CloseStatus.POLICY_VIOLATION.getCode()));
        verify(sessionManager, never()).addSession(anyLong(), any());
    }

    @Test
    void handleTextMessageShouldCloseWhenTokenNotStringInMap() throws Exception {
        sessionAttrs.put("taskId", 1L);
        sessionAttrs.put("authenticated", false);
        when(objectMapper.readValue(anyString(), eq(Map.class))).thenReturn(Map.of("token", 123));

        handler.handleTextMessage(session, new TextMessage("{\"token\":123}"));

        verify(session).close(argThat(s -> s.getCode() == CloseStatus.POLICY_VIOLATION.getCode()));
        verify(sessionManager, never()).addSession(anyLong(), any());
    }

    @Test
    void handleTextMessageShouldCloseWhenTaskIdNullInAttrs() throws Exception {
        sessionAttrs.put("authenticated", false);
        when(objectMapper.readValue(anyString(), eq(Map.class))).thenReturn(Map.of("token", "t"));
        when(jwtUtils.resolveJwt("Bearer t")).thenReturn(decodedJwt);
        // taskId 为 null 时直接 close，不调用 toId/toUser/getById

        handler.handleTextMessage(session, new TextMessage("{\"token\":\"t\"}"));

        verify(session).close(argThat(s -> s.getCode() == CloseStatus.BAD_DATA.getCode()));
        verify(sessionManager, never()).addSession(anyLong(), any());
    }

    @Test
    void handleTextMessageShouldAddSessionWhenAdminEvenNotOwner() throws Exception {
        sessionAttrs.put("taskId", 1L);
        sessionAttrs.put("authenticated", false);
        when(objectMapper.readValue(anyString(), eq(Map.class))).thenReturn(Map.of("token", "t"));
        when(jwtUtils.resolveJwt("Bearer t")).thenReturn(decodedJwt);
        when(jwtUtils.toId(decodedJwt)).thenReturn(999L);
        when(jwtUtils.toUser(decodedJwt)).thenReturn(org.springframework.security.core.userdetails.User.builder()
                .username("admin").password("").authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_admin"))).build());
        Task task = new Task();
        task.setUid(100L);
        when(taskService.getById(1L)).thenReturn(task);

        handler.handleTextMessage(session, new TextMessage("{\"token\":\"t\"}"));

        verify(session, never()).close(any());
        verify(sessionManager).addSession(1L, session);
        verify(subscriptionService).subscribeTask(1L);
    }

    @Test
    void handleTextMessageShouldCloseWhenJwtInvalid() throws Exception {
        sessionAttrs.put("taskId", 1L);
        sessionAttrs.put("authenticated", false);
        when(objectMapper.readValue(anyString(), eq(Map.class))).thenReturn(Map.of("token", "bad"));
        when(jwtUtils.resolveJwt("Bearer bad")).thenReturn(null);

        handler.handleTextMessage(session, new TextMessage("{\"token\":\"bad\"}"));

        verify(session).close(argThat(s -> s.getCode() == CloseStatus.POLICY_VIOLATION.getCode()));
        verify(sessionManager, never()).addSession(anyLong(), any());
    }

    @Test
    void handleTextMessageShouldCloseWhenTaskNotFound() throws Exception {
        sessionAttrs.put("taskId", 1L);
        sessionAttrs.put("authenticated", false);
        when(objectMapper.readValue(anyString(), eq(Map.class))).thenReturn(Map.of("token", "t"));
        when(jwtUtils.resolveJwt("Bearer t")).thenReturn(decodedJwt);
        when(taskService.getById(1L)).thenReturn(null);
        // task 为 null 时直接 close，不会调用 toId/toUser

        handler.handleTextMessage(session, new TextMessage("{\"token\":\"t\"}"));

        verify(session).close(argThat(s -> s.getCode() == CloseStatus.POLICY_VIOLATION.getCode()));
        verify(sessionManager, never()).addSession(anyLong(), any());
    }

    @Test
    void handleTextMessageShouldCloseWhenNoPermission() throws Exception {
        sessionAttrs.put("taskId", 1L);
        sessionAttrs.put("authenticated", false);
        when(objectMapper.readValue(anyString(), eq(Map.class))).thenReturn(Map.of("token", "t"));
        when(jwtUtils.resolveJwt("Bearer t")).thenReturn(decodedJwt);
        when(jwtUtils.toId(decodedJwt)).thenReturn(999L);
        when(jwtUtils.toUser(decodedJwt)).thenReturn(org.springframework.security.core.userdetails.User.builder()
                .username("u").password("").authorities(Collections.emptyList()).build());
        Task task = new Task();
        task.setUid(100L);
        when(taskService.getById(1L)).thenReturn(task);

        handler.handleTextMessage(session, new TextMessage("{\"token\":\"t\"}"));

        verify(session).close(argThat(s -> s.getCode() == CloseStatus.POLICY_VIOLATION.getCode()));
        verify(sessionManager, never()).addSession(anyLong(), any());
    }

    @Test
    void handleTextMessageShouldAddSessionAndSubscribeWhenAuthorized() throws Exception {
        sessionAttrs.put("taskId", 1L);
        sessionAttrs.put("authenticated", false);
        when(objectMapper.readValue(anyString(), eq(Map.class))).thenReturn(Map.of("token", "t"));
        when(jwtUtils.resolveJwt("Bearer t")).thenReturn(decodedJwt);
        when(jwtUtils.toId(decodedJwt)).thenReturn(100L);
        when(jwtUtils.toUser(decodedJwt)).thenReturn(org.springframework.security.core.userdetails.User.builder()
                .username("u").password("").authorities(Collections.emptyList()).build());
        Task task = new Task();
        task.setUid(100L);
        when(taskService.getById(1L)).thenReturn(task);

        handler.handleTextMessage(session, new TextMessage("{\"token\":\"t\"}"));

        verify(session, never()).close(any());
        verify(sessionManager).addSession(1L, session);
        verify(subscriptionService).subscribeTask(1L);
    }
}
