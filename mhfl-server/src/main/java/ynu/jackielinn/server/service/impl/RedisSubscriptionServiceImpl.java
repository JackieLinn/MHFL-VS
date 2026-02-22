package ynu.jackielinn.server.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ynu.jackielinn.server.service.RedisSubscriptionService;

/**
 * Redis 按任务动态订阅的占位实现。
 * 阶段 4 仅做日志占位；阶段 5 将改为依赖 RedisMessageListenerContainer + TrainingMessageListener 做真实订阅/取消订阅。
 */
@Slf4j
@Service
public class RedisSubscriptionServiceImpl implements RedisSubscriptionService {

    @Override
    public void subscribeTask(Long taskId) {
        log.debug("subscribeTask({}) - placeholder until Phase 5 Redis listener is wired", taskId);
    }

    @Override
    public void unsubscribeTask(Long taskId) {
        log.debug("unsubscribeTask({}) - placeholder until Phase 5 Redis listener is wired", taskId);
    }
}
