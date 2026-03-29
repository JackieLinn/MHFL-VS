package ynu.jackielinn.server.integration;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestTemplate;

/**
 * 集成测试基类。
 * 统一启用 test 配置，关闭定时任务，避免测试期间触发外部依赖调用。
 */
@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    /**
     * Mock Redis 连接工厂，避免连接真实 Redis。
     */
    @MockitoBean
    protected LettuceConnectionFactory redisConnectionFactory;

    /**
     * Mock Redis 模板，避免测试访问真实 Redis。
     */
    @MockitoBean
    protected StringRedisTemplate stringRedisTemplate;

    /**
     * Mock RabbitTemplate，避免测试访问真实 RabbitMQ。
     */
    @MockitoBean
    protected RabbitTemplate rabbitTemplate;

    /**
     * Mock RestTemplate，避免测试访问真实 FastAPI。
     */
    @MockitoBean
    protected RestTemplate restTemplate;

}
