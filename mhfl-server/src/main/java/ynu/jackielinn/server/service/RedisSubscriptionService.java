package ynu.jackielinn.server.service;

/**
 * 按任务动态订阅/取消订阅 Redis 训练消息通道。
 * 当有 WebSocket 连接订阅某任务时订阅；该任务最后一个连接断开时取消订阅。
 */
public interface RedisSubscriptionService {

    /**
     * 订阅该任务对应的 Round/Client/Status 通道（若尚未订阅）。
     */
    void subscribeTask(Long taskId);

    /**
     * 取消订阅该任务对应的通道（无连接时调用）。
     */
    void unsubscribeTask(Long taskId);
}
