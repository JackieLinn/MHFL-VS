package ynu.jackielinn.server.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * WebSocketSessionManager 单元测试：add/remove/getSessions、sendToTask、closeAllSessionsForTask。
 */
class WebSocketSessionManagerTest {

    private WebSocketSessionManager manager;

    @BeforeEach
    void setUp() {
        manager = new WebSocketSessionManager();
    }

    @Test
    void addSessionShouldStoreSessionForTask() {
        Long taskId = 1L;
        WebSocketSession session = mock(WebSocketSession.class);

        manager.addSession(taskId, session);

        Set<WebSocketSession> sessions = manager.getSessions(taskId);
        assertEquals(1, sessions.size());
        assertTrue(sessions.contains(session));
    }

    @Test
    void addSessionMultipleForSameTaskShouldAllBeStored() {
        Long taskId = 1L;
        WebSocketSession s1 = mock(WebSocketSession.class);
        WebSocketSession s2 = mock(WebSocketSession.class);

        manager.addSession(taskId, s1);
        manager.addSession(taskId, s2);

        assertEquals(2, manager.getSessions(taskId).size());
    }

    @Test
    void removeSessionShouldRemoveFromTaskAndClearTaskIfEmpty() {
        Long taskId = 1L;
        WebSocketSession session = mock(WebSocketSession.class);
        manager.addSession(taskId, session);

        manager.removeSession(taskId, session);

        assertTrue(manager.getSessions(taskId).isEmpty());
    }

    @Test
    void getSessionsWhenNoTaskShouldReturnEmptySet() {
        Set<WebSocketSession> sessions = manager.getSessions(999L);
        assertNotNull(sessions);
        assertTrue(sessions.isEmpty());
    }

    @Test
    void sendToTaskWhenNoSessionsShouldDoNothing() {
        manager.sendToTask(1L, new Object());
        // no exception
    }

    @Test
    void sendToTaskShouldSendToOpenSessions() throws Exception {
        Long taskId = 1L;
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        when(session.getId()).thenReturn("s1");
        manager.addSession(taskId, session);

        manager.sendToTask(taskId, "{\"key\":\"value\"}");

        verify(session).sendMessage(argThat(msg -> msg instanceof TextMessage && ((TextMessage) msg).getPayload().contains("key")));
    }

    @Test
    void sendToTaskShouldRemoveSessionWhenNotOpen() throws Exception {
        Long taskId = 1L;
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(false);
        manager.addSession(taskId, session);

        manager.sendToTask(taskId, "payload");

        verify(session, never()).sendMessage(any());
        assertTrue(manager.getSessions(taskId).isEmpty());
    }

    @Test
    void sendToTaskShouldRemoveSessionWhenSendThrows() throws Exception {
        Long taskId = 1L;
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        when(session.getId()).thenReturn("s1");
        doThrow(new IOException("send failed")).when(session).sendMessage(any(TextMessage.class));
        manager.addSession(taskId, session);

        manager.sendToTask(taskId, "payload");

        assertTrue(manager.getSessions(taskId).isEmpty());
    }

    @Test
    void closeAllSessionsForTaskShouldRemoveAndCloseOpenSessions() throws Exception {
        Long taskId = 1L;
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        when(session.getId()).thenReturn("s1");
        manager.addSession(taskId, session);

        manager.closeAllSessionsForTask(taskId);

        verify(session).close(any());
        assertTrue(manager.getSessions(taskId).isEmpty());
    }

    @Test
    void closeAllSessionsForTaskWhenNoSessionsShouldDoNothing() {
        manager.closeAllSessionsForTask(1L);
        // no exception
    }

    @Test
    void removeSessionWhenTaskNotInMapShouldDoNothing() {
        WebSocketSession session = mock(WebSocketSession.class);
        manager.removeSession(999L, session);
        assertTrue(manager.getSessions(999L).isEmpty());
    }

    @Test
    void sendToTaskWhenSerializeFailsShouldNotSendAndNotThrow() throws Exception {
        ObjectMapper mockMapper = mock(ObjectMapper.class);
        when(mockMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("serialize fail") {
        });
        ReflectionTestUtils.setField(manager, "objectMapper", mockMapper);
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        manager.addSession(1L, session);

        manager.sendToTask(1L, "payload");

        verify(session, never()).sendMessage(any(TextMessage.class));
    }

    @Test
    void closeAllSessionsForTaskWhenCloseThrowsShouldRemoveSessionAndNotThrow() throws Exception {
        Long taskId = 1L;
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        when(session.getId()).thenReturn("s1");
        doThrow(new IOException("close failed")).when(session).close(any());
        manager.addSession(taskId, session);

        manager.closeAllSessionsForTask(taskId);

        assertTrue(manager.getSessions(taskId).isEmpty());
    }
}
