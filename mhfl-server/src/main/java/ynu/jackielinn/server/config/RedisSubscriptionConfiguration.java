package ynu.jackielinn.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Redis 订阅相关配置。
 * 提供 RedisMessageListenerContainer，用于订阅训练消息 channel（round/client/status），
 * 与现有 Lettuce 连接工厂共用同一连接配置。
 */
@Configuration
public class RedisSubscriptionConfiguration {

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            LettuceConnectionFactory redisConnectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        return container;
    }
}
