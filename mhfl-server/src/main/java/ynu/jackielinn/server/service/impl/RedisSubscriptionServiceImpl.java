package ynu.jackielinn.server.service.impl;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Service;
import ynu.jackielinn.server.listener.TrainingMessageListener;
import ynu.jackielinn.server.service.RedisSubscriptionService;
import ynu.jackielinn.server.utils.Const;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 按任务动态订阅 Redis 训练消息通道。
 * WebSocket 连接并首包鉴权通过后调用 subscribeTask；该任务最后一个连接断开时调用 unsubscribeTask。
 */
@Slf4j
@Service
public class RedisSubscriptionServiceImpl implements RedisSubscriptionService {

    @Resource
    RedisMessageListenerContainer redisMessageListenerContainer;

    @Resource
    TrainingMessageListener messageListener;

    private final Set<Long> subscribedTasks = ConcurrentHashMap.newKeySet();

    @Override
    public void subscribeTask(Long taskId) {
        if (subscribedTasks.contains(taskId)) {
            return;
        }
        String roundChannel = Const.TASK_EXPERIMENT_ROUND + taskId;
        String clientChannel = Const.TASK_EXPERIMENT_CLIENT + taskId;
        String statusChannel = Const.TASK_EXPERIMENT_STATUS + taskId;
        redisMessageListenerContainer.addMessageListener(messageListener, new ChannelTopic(roundChannel));
        redisMessageListenerContainer.addMessageListener(messageListener, new ChannelTopic(clientChannel));
        redisMessageListenerContainer.addMessageListener(messageListener, new ChannelTopic(statusChannel));
        subscribedTasks.add(taskId);
        log.info("Subscribed to Redis channels for task {}", taskId);
    }

    @Override
    public void unsubscribeTask(Long taskId) {
        if (!subscribedTasks.contains(taskId)) {
            return;
        }
        String roundChannel = Const.TASK_EXPERIMENT_ROUND + taskId;
        String clientChannel = Const.TASK_EXPERIMENT_CLIENT + taskId;
        String statusChannel = Const.TASK_EXPERIMENT_STATUS + taskId;
        redisMessageListenerContainer.removeMessageListener(messageListener, new ChannelTopic(roundChannel));
        redisMessageListenerContainer.removeMessageListener(messageListener, new ChannelTopic(clientChannel));
        redisMessageListenerContainer.removeMessageListener(messageListener, new ChannelTopic(statusChannel));
        subscribedTasks.remove(taskId);
        log.info("Unsubscribed from Redis channels for task {}", taskId);
    }
}
