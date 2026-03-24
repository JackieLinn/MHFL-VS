package ynu.jackielinn.server.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;
import ynu.jackielinn.server.common.Status;
import ynu.jackielinn.server.dto.message.ClientMessage;
import ynu.jackielinn.server.dto.message.RoundMessage;
import ynu.jackielinn.server.dto.message.StatusMessage;
import ynu.jackielinn.server.entity.Client;
import ynu.jackielinn.server.entity.Round;
import ynu.jackielinn.server.entity.Task;
import ynu.jackielinn.server.service.ClientService;
import ynu.jackielinn.server.service.RedisSubscriptionService;
import ynu.jackielinn.server.service.RoundService;
import ynu.jackielinn.server.service.TaskService;
import ynu.jackielinn.server.websocket.WebSocketSessionManager;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainingMessageHandlerImplTest {

    @InjectMocks
    private TrainingMessageHandlerImpl service;

    @Mock
    private RoundService roundService;

    @Mock
    private ClientService clientService;

    @Mock
    private TaskService taskService;

    @Mock
    private WebSocketSessionManager sessionManager;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private RedisSubscriptionService redisSubscriptionService;

    @Test
    void handleRoundMessageShouldUpdateExistingRoundAndTaskMetrics() {
        RoundMessage message = RoundMessage.builder()
                .taskId(1L).roundNum(2)
                .loss(0.4).accuracy(0.8).precision(0.7).recall(0.6).f1Score(0.65)
                .build();
        when(roundService.getByTidAndRoundNum(1L, 2)).thenReturn(Round.builder().id(100L).tid(1L).roundNum(2).build());
        when(taskService.getById(1L)).thenReturn(Task.builder().id(1L).numSteps(10).status(Status.NOT_STARTED).accuracy(0.7).build());

        service.handleRoundMessage(message);

        verify(roundService).updateById(any(Round.class));
        verify(taskService, times(2)).updateById(any(Task.class));
        verify(sessionManager).sendToTask(1L, message);
    }

    @Test
    void handleRoundMessageShouldSaveRoundWhenMissingAndSetSuccessOnLastRound() {
        RoundMessage message = RoundMessage.builder()
                .taskId(2L).roundNum(4)
                .loss(0.3).accuracy(0.9).precision(0.88).recall(0.87).f1Score(0.875)
                .build();
        when(roundService.getByTidAndRoundNum(2L, 4)).thenReturn(null);
        when(taskService.getById(2L)).thenReturn(Task.builder().id(2L).numSteps(5).status(Status.IN_PROGRESS).accuracy(0.85).build());

        service.handleRoundMessage(message);

        verify(roundService).saveRound(any(Round.class));
        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskService, times(2)).updateById(captor.capture());
        assertThat(captor.getAllValues().stream().anyMatch(t -> t.getStatus() == Status.SUCCESS)).isTrue();
    }

    @Test
    void handleRoundMessageShouldHandleTaskNullAndAccuracyNull() {
        RoundMessage message = RoundMessage.builder().taskId(3L).roundNum(1).accuracy(null).build();
        when(roundService.getByTidAndRoundNum(3L, 1)).thenReturn(null);
        when(taskService.getById(3L)).thenReturn(null);

        service.handleRoundMessage(message);

        verify(roundService).saveRound(any(Round.class));
        verify(taskService, never()).updateById(any(Task.class));
        verify(sessionManager).sendToTask(3L, message);
    }

    @Test
    void handleRoundMessageShouldSwallowException() {
        RoundMessage message = RoundMessage.builder().taskId(4L).roundNum(1).build();
        when(roundService.getByTidAndRoundNum(4L, 1)).thenThrow(new RuntimeException("boom"));

        service.handleRoundMessage(message);

        verify(sessionManager, never()).sendToTask(eq(4L), any());
    }

    @Test
    void handleClientMessageShouldUseExistingRoundAndSaveClient() {
        ClientMessage message = ClientMessage.builder()
                .taskId(10L).roundNum(1).clientIndex(3)
                .loss(0.5).accuracy(0.6).precision(0.61).recall(0.62).f1Score(0.63)
                .timestamp("2026-03-24T12:00:00")
                .build();
        when(roundService.getByTidAndRoundNum(10L, 1)).thenReturn(Round.builder().id(99L).tid(10L).roundNum(1).build());

        service.handleClientMessage(message);

        verify(roundService, never()).saveRound(any(Round.class));
        ArgumentCaptor<Client> captor = ArgumentCaptor.forClass(Client.class);
        verify(clientService).saveClient(captor.capture());
        assertThat(captor.getValue().getRid()).isEqualTo(99L);
        assertThat(captor.getValue().getTimestamp()).isNotNull();
        verify(sessionManager).sendToTask(10L, message);
    }

    @Test
    void handleClientMessageShouldCreatePlaceholderRoundWhenMissing() {
        ClientMessage message = ClientMessage.builder()
                .taskId(11L).roundNum(2).clientIndex(4)
                .timestamp("bad-ts")
                .build();
        when(roundService.getByTidAndRoundNum(11L, 2)).thenReturn(null);
        doAnswer(invocation -> {
            Round round = invocation.getArgument(0);
            round.setId(123L);
            return null;
        }).when(roundService).saveRound(any(Round.class));

        service.handleClientMessage(message);

        verify(roundService).saveRound(any(Round.class));
        ArgumentCaptor<Client> captor = ArgumentCaptor.forClass(Client.class);
        verify(clientService).saveClient(captor.capture());
        assertThat(captor.getValue().getRid()).isEqualTo(123L);
        assertThat(captor.getValue().getTimestamp()).isNotNull();
    }

    @Test
    void handleClientMessageShouldHandleNullAndOffsetTimestamps() {
        ClientMessage m1 = ClientMessage.builder().taskId(12L).roundNum(1).clientIndex(1).timestamp(null).build();
        ClientMessage m2 = ClientMessage.builder().taskId(12L).roundNum(2).clientIndex(2).timestamp("2026-03-24T12:00:00+08:00").build();
        when(roundService.getByTidAndRoundNum(12L, 1)).thenReturn(Round.builder().id(1L).build());
        when(roundService.getByTidAndRoundNum(12L, 2)).thenReturn(Round.builder().id(2L).build());

        service.handleClientMessage(m1);
        service.handleClientMessage(m2);

        verify(clientService, times(2)).saveClient(any(Client.class));
    }

    @Test
    void handleClientMessageShouldSwallowException() {
        ClientMessage message = ClientMessage.builder().taskId(13L).roundNum(1).clientIndex(1).build();
        doThrow(new RuntimeException("x")).when(roundService).getByTidAndRoundNum(13L, 1);

        service.handleClientMessage(message);

        verify(sessionManager, never()).sendToTask(eq(13L), any());
    }

    @Test
    void handleStatusMessageShouldReturnWhenUnknownStatus() {
        StatusMessage message = StatusMessage.builder().taskId(20L).status("UNKNOWN").build();

        service.handleStatusMessage(message);

        verify(taskService, never()).updateById(any(Task.class));
        verify(sessionManager, never()).sendToTask(eq(20L), any());
    }

    @Test
    void handleStatusMessageShouldUpdateTaskAndSendWhenInProgress() {
        StatusMessage message = StatusMessage.builder().taskId(21L).status("IN_PROGRESS").build();
        when(taskService.getById(21L)).thenReturn(Task.builder().id(21L).status(Status.NOT_STARTED).build());

        service.handleStatusMessage(message);

        verify(taskService).updateById(any(Task.class));
        verify(redisSubscriptionService, never()).unsubscribeTask(any());
        verify(sessionManager).sendToTask(21L, message);
    }

    @Test
    void handleStatusMessageShouldUnsubscribeOnTerminalStatuses() {
        when(applicationContext.getBean(RedisSubscriptionService.class)).thenReturn(redisSubscriptionService);
        for (String s : new String[]{"SUCCESS", "FAILED", "CANCELLED"}) {
            StatusMessage message = StatusMessage.builder().taskId(22L).status(s).build();
            when(taskService.getById(22L)).thenReturn(Task.builder().id(22L).build());
            service.handleStatusMessage(message);
        }
        verify(redisSubscriptionService, times(3)).unsubscribeTask(22L);
    }

    @Test
    void handleStatusMessageShouldSendEvenWhenTaskNull() {
        StatusMessage message = StatusMessage.builder().taskId(23L).status("SUCCESS").build();
        when(taskService.getById(23L)).thenReturn(null);

        service.handleStatusMessage(message);

        verify(taskService, never()).updateById(any(Task.class));
        verify(redisSubscriptionService, never()).unsubscribeTask(any());
        verify(sessionManager).sendToTask(23L, message);
    }

    @Test
    void handleStatusMessageShouldSwallowException() {
        StatusMessage message = StatusMessage.builder().taskId(24L).status("SUCCESS").build();
        when(taskService.getById(24L)).thenThrow(new RuntimeException("status-ex"));

        service.handleStatusMessage(message);

        verify(sessionManager, never()).sendToTask(eq(24L), any());
    }

    @Test
    void privateToStatusAndParseTimestampShouldCoverBranches() {
        Status s1 = ReflectionTestUtils.invokeMethod(service, "toStatus", "IN_PROGRESS");
        Status s2 = ReflectionTestUtils.invokeMethod(service, "toStatus", "SUCCESS");
        Status s3 = ReflectionTestUtils.invokeMethod(service, "toStatus", "FAILED");
        Status s4 = ReflectionTestUtils.invokeMethod(service, "toStatus", "CANCELLED");
        Status s5 = ReflectionTestUtils.invokeMethod(service, "toStatus", "X");
        Status s6 = ReflectionTestUtils.invokeMethod(service, "toStatus", (Object) null);

        LocalDateTime t1 = ReflectionTestUtils.invokeMethod(service, "parseTimestamp", "2026-03-24T12:00:00");
        LocalDateTime t2 = ReflectionTestUtils.invokeMethod(service, "parseTimestamp", "2026-03-24T12:00:00Z");
        LocalDateTime t3 = ReflectionTestUtils.invokeMethod(service, "parseTimestamp", "2026-03-24T12:00:00+08:00");
        LocalDateTime t4 = ReflectionTestUtils.invokeMethod(service, "parseTimestamp", "bad");
        LocalDateTime t5 = ReflectionTestUtils.invokeMethod(service, "parseTimestamp", "  ");

        assertThat(s1).isEqualTo(Status.IN_PROGRESS);
        assertThat(s2).isEqualTo(Status.SUCCESS);
        assertThat(s3).isEqualTo(Status.FAILED);
        assertThat(s4).isEqualTo(Status.CANCELLED);
        assertThat(s5).isNull();
        assertThat(s6).isNull();
        assertThat(t1).isNotNull();
        assertThat(t2).isNotNull();
        assertThat(t3).isNotNull();
        assertThat(t4).isNotNull();
        assertThat(t5).isNotNull();
    }

    @Test
    void lastRoundTimeShouldBeUpdatedAndRemoved() {
        RoundMessage rm = RoundMessage.builder().taskId(30L).roundNum(1).accuracy(0.8).build();
        when(roundService.getByTidAndRoundNum(30L, 1)).thenReturn(null);
        when(taskService.getById(30L)).thenReturn(null);
        service.handleRoundMessage(rm);

        Map<Long, Long> map = (Map<Long, Long>) ReflectionTestUtils.getField(service, "lastRoundTime");
        assertThat(map).containsKey(30L);

        when(taskService.getById(30L)).thenReturn(Task.builder().id(30L).build());
        when(applicationContext.getBean(RedisSubscriptionService.class)).thenReturn(redisSubscriptionService);
        service.handleStatusMessage(StatusMessage.builder().taskId(30L).status("SUCCESS").build());
        assertThat(map).doesNotContainKey(30L);
    }
}
