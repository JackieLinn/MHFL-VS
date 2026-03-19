package ynu.jackielinn.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutor;

import java.util.concurrent.Executor;

/**
 * 线程池配置类。
 * 提供 trainingMessageExecutor，用于异步处理 Redis 训练消息（先写 MySQL，再推 WebSocket），
 * 避免阻塞 Redis 订阅线程。
 */
@Configuration
public class AsyncConfiguration {

    /**
     * 提供训练消息处理专用线程池，用于异步处理 Redis 训练消息（先写 MySQL，再推 WebSocket），
     * 避免阻塞 Redis 订阅线程。核心 5、最大 20、队列 100，线程名前缀 training-message-。
     *
     * @return 用于训练消息处理的 Executor
     */
    @Bean(name = "trainingMessageExecutor")
    public Executor trainingMessageExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数，常驻池中
        executor.setCorePoolSize(5);
        // 最大线程数，任务多时再创建
        executor.setMaxPoolSize(20);
        // 队列容量，超过后才会创建超过核心数的线程
        executor.setQueueCapacity(100);
        // 线程名前缀，便于日志排查
        executor.setThreadNamePrefix("training-message-");
        // 关闭时等待已提交任务执行完
        executor.setWaitForTasksToCompleteOnShutdown(true);
        // 最多等待时间（秒），超时后强制结束
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    /**
     * 智能助手流式聊天专用线程池，用于异步消费 Python 流并转发 SSE。
     * 核心 2、最大 8、队列 50。
     *
     * @return 用于流式聊天的 Executor
     */
    @Bean(name = "assistantChatStreamExecutor")
    public Executor assistantChatStreamExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("assistant-chat-stream-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        // 传播 SecurityContext 到异步线程，避免 SSE 流式响应时 Access Denied
        return new DelegatingSecurityContextExecutor(executor);
    }
}
