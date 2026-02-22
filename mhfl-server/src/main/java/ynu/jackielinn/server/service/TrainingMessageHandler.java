package ynu.jackielinn.server.service;

import ynu.jackielinn.server.dto.message.ClientMessage;
import ynu.jackielinn.server.dto.message.RoundMessage;
import ynu.jackielinn.server.dto.message.StatusMessage;

/**
 * 训练消息处理器：处理 Redis 订阅到的 Round/Client/Status 消息，
 * 先写 MySQL（Round/Client 表及 Task 状态），再推 WebSocket。
 */
public interface TrainingMessageHandler {

    void handleRoundMessage(RoundMessage message);

    void handleClientMessage(ClientMessage message);

    void handleStatusMessage(StatusMessage message);
}
