package ynu.jackielinn.server.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import ynu.jackielinn.server.dto.message.ClientMessage;
import ynu.jackielinn.server.dto.message.RoundMessage;
import ynu.jackielinn.server.dto.message.StatusMessage;
import ynu.jackielinn.server.service.TrainingMessageHandler;
import ynu.jackielinn.server.utils.Const;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;

/**
 * Redis 训练消息监听器。
 * 订阅 task:experiment:round|client|status:{taskId} 通道，收到消息后提交到线程池，
 * 由 TrainingMessageHandler 先写 MySQL 再推 WebSocket。
 */
@Slf4j
@Component
public class TrainingMessageListener implements MessageListener {

    @Resource
    TrainingMessageHandler messageHandler;

    @Resource(name = "trainingMessageExecutor")
    Executor executor;

    @Resource
    ObjectMapper objectMapper;

    /**
     * 收到 Redis 订阅消息时根据 channel 前缀解析为 Round/Client/Status 消息，
     * 提交到 trainingMessageExecutor 线程池，由 TrainingMessageHandler 写库并推 WebSocket。
     *
     * @param message Redis 消息体
     * @param pattern 订阅的 channel 模式（未使用）
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
        String body = new String(message.getBody(), StandardCharsets.UTF_8);

        try {
            if (channel.startsWith(Const.TASK_EXPERIMENT_ROUND)) {
                RoundMessage roundMessage = objectMapper.readValue(body, RoundMessage.class);
                executor.execute(() -> messageHandler.handleRoundMessage(roundMessage));
            } else if (channel.startsWith(Const.TASK_EXPERIMENT_CLIENT)) {
                ClientMessage clientMessage = objectMapper.readValue(body, ClientMessage.class);
                executor.execute(() -> messageHandler.handleClientMessage(clientMessage));
            } else if (channel.startsWith(Const.TASK_EXPERIMENT_STATUS)) {
                StatusMessage statusMessage = objectMapper.readValue(body, StatusMessage.class);
                executor.execute(() -> messageHandler.handleStatusMessage(statusMessage));
            }
        } catch (Exception e) {
            log.error("Failed to process Redis message from channel {}: {}", channel, e.getMessage(), e);
        }
    }
}
