package ynu.jackielinn.server.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.Message;
import ynu.jackielinn.server.dto.message.ClientMessage;
import ynu.jackielinn.server.dto.message.RoundMessage;
import ynu.jackielinn.server.dto.message.StatusMessage;
import ynu.jackielinn.server.service.TrainingMessageHandler;
import ynu.jackielinn.server.utils.Const;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * TrainingMessageListener 单元测试：按 channel 解析 Round/Client/Status 并提交线程池执行。
 */
@ExtendWith(MockitoExtension.class)
class TrainingMessageListenerTest {

    private static final Charset UTF_8 = StandardCharsets.UTF_8;

    @Mock
    private TrainingMessageHandler messageHandler;

    @Mock
    private Executor executor;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private TrainingMessageListener listener;

    @Mock
    private Message message;

    @BeforeEach
    void setUp() {
        // lenient：shouldNotCallHandlerWhenChannelUnknown / shouldCatchExceptionWhenReadValueFails 不会调用 execute
        // 执行提交的 Runnable，便于验证 handler 被调用
        lenient().doAnswer(inv -> {
            Runnable r = inv.getArgument(0);
            r.run();
            return null;
        }).when(executor).execute(any(Runnable.class));
    }

    @Test
    void shouldHandleRoundMessageWhenChannelStartsWithRound() throws Exception {
        String channel = Const.TASK_EXPERIMENT_ROUND + "1";
        String body = "{\"taskId\":1,\"roundNum\":0}";
        when(message.getChannel()).thenReturn(channel.getBytes(UTF_8));
        when(message.getBody()).thenReturn(body.getBytes(UTF_8));
        RoundMessage roundMessage = new RoundMessage();
        when(objectMapper.readValue(eq(body), eq(RoundMessage.class))).thenReturn(roundMessage);

        listener.onMessage(message, null);

        verify(executor).execute(any(Runnable.class));
        verify(messageHandler).handleRoundMessage(roundMessage);
    }

    @Test
    void shouldHandleClientMessageWhenChannelStartsWithClient() throws Exception {
        String channel = Const.TASK_EXPERIMENT_CLIENT + "1";
        String body = "{\"taskId\":1,\"clientIndex\":0}";
        when(message.getChannel()).thenReturn(channel.getBytes(UTF_8));
        when(message.getBody()).thenReturn(body.getBytes(UTF_8));
        ClientMessage clientMessage = new ClientMessage();
        when(objectMapper.readValue(eq(body), eq(ClientMessage.class))).thenReturn(clientMessage);

        listener.onMessage(message, null);

        verify(executor).execute(any(Runnable.class));
        verify(messageHandler).handleClientMessage(clientMessage);
    }

    @Test
    void shouldHandleStatusMessageWhenChannelStartsWithStatus() throws Exception {
        String channel = Const.TASK_EXPERIMENT_STATUS + "1";
        String body = "{\"taskId\":1,\"status\":\"SUCCESS\"}";
        when(message.getChannel()).thenReturn(channel.getBytes(UTF_8));
        when(message.getBody()).thenReturn(body.getBytes(UTF_8));
        StatusMessage statusMessage = new StatusMessage();
        when(objectMapper.readValue(eq(body), eq(StatusMessage.class))).thenReturn(statusMessage);

        listener.onMessage(message, null);

        verify(executor).execute(any(Runnable.class));
        verify(messageHandler).handleStatusMessage(statusMessage);
    }

    @Test
    void shouldNotCallHandlerWhenChannelUnknown() throws Exception {
        when(message.getChannel()).thenReturn("other:channel".getBytes(UTF_8));
        when(message.getBody()).thenReturn("{}".getBytes(UTF_8));

        listener.onMessage(message, null);

        verify(executor, never()).execute(any(Runnable.class));
        verify(messageHandler, never()).handleRoundMessage(any());
        verify(messageHandler, never()).handleClientMessage(any());
        verify(messageHandler, never()).handleStatusMessage(any());
    }

    @Test
    void shouldCatchExceptionWhenReadValueFails() throws Exception {
        String channel = Const.TASK_EXPERIMENT_ROUND + "1";
        when(message.getChannel()).thenReturn(channel.getBytes(UTF_8));
        when(message.getBody()).thenReturn("invalid json".getBytes(UTF_8));
        when(objectMapper.readValue(anyString(), eq(RoundMessage.class))).thenThrow(new RuntimeException("parse error"));

        listener.onMessage(message, null);

        verify(executor, never()).execute(any(Runnable.class));
        verify(messageHandler, never()).handleRoundMessage(any());
    }
}
