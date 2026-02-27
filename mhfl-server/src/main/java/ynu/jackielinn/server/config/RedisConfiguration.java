package ynu.jackielinn.server.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.protocol.ProtocolVersion;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

/**
 * Redis 配置类。使用 Lettuce 客户端，并采用 RESP2 协议以兼容 Redis 7.x，避免 RESP3 兼容性问题。
 */
@Configuration
public class RedisConfiguration {

    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    @Value("${spring.data.redis.password}")
    private String password;

    @Value("${spring.data.redis.database}")
    private int database;

    /**
     * 创建 Redis 连接工厂，使用 standalone 配置与 RESP2 协议。
     *
     * @return LettuceConnectionFactory 实例，供 RedisTemplate 与 Redis 订阅共用
     */
    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        // Redis 服务器配置
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(host);
        redisConfig.setPort(port);
        redisConfig.setPassword(RedisPassword.of(password));
        redisConfig.setDatabase(database);

        // Lettuce 客户端配置，使用 RESP2 协议
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .clientOptions(ClientOptions.builder()
                        .protocolVersion(ProtocolVersion.RESP2)
                        .build())
                .build();

        return new LettuceConnectionFactory(redisConfig, clientConfig);
    }
}
