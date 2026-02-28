package ynu.jackielinn.server.service;

import ynu.jackielinn.server.dto.message.ClientMessage;
import ynu.jackielinn.server.dto.message.RoundMessage;
import ynu.jackielinn.server.dto.message.StatusMessage;

/**
 * 训练消息处理器：处理 Redis 订阅到的 Round/Client/Status 消息，
 * 先写 MySQL（Round/Client 表及 Task 状态），再推 WebSocket。
 */
public interface TrainingMessageHandler {

    /**
     * 处理轮次消息：写入或更新 Round，更新 Task 指标；写库后向该 taskId 的 WebSocket 推送。
     *
     * @param message 轮次指标消息（taskId、roundNum、五指标、timestamp）
     */
    void handleRoundMessage(RoundMessage message);

    /**
     * 处理客户端消息：按 (taskId, roundNum) 保证 Round 存在后写入 Client，再向该 taskId 的 WebSocket 推送。
     *
     * @param message 客户端指标消息（taskId、roundNum、clientIndex、五指标、timestamp）
     */
    void handleClientMessage(ClientMessage message);

    /**
     * 处理状态消息：更新 Task 状态（IN_PROGRESS/SUCCESS/FAILED/CANCELLED），终态时取消 Redis 订阅并可选关闭 WebSocket。
     *
     * @param message 状态消息（taskId、status、message、timestamp）
     */
    void handleStatusMessage(StatusMessage message);
}
