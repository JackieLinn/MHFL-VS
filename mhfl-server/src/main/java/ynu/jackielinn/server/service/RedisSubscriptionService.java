package ynu.jackielinn.server.service;

public interface RedisSubscriptionService {

    /**
     * 订阅该任务对应的 Round/Client/Status 通道（若尚未订阅则订阅，已订阅则幂等）。
     *
     * @param taskId 任务 id
     */
    void subscribeTask(Long taskId);

    /**
     * 取消订阅该任务对应的通道（无连接或任务终态时调用）。
     *
     * @param taskId 任务 id
     */
    void unsubscribeTask(Long taskId);
}
