package ynu.jackielinn.server.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置类。声明邮件队列及 JSON 消息转换器，供异步发邮件等使用。
 */
@Configuration
public class RabbitConfiguration {

    /**
     * 声明持久化邮件队列 MHFLVSMail，用于发送邮件等异步任务。
     *
     * @return 持久化 Queue 实例
     */
    @Bean("MHFLVSEmailQueue")
    public Queue emailQueue() {
        return QueueBuilder
                .durable("MHFLVSMail")
                .build();
    }

    /**
     * 提供 JSON 消息转换器，使 RabbitMQ 消息以 JSON 序列化/反序列化。
     *
     * @return Jackson2JsonMessageConverter 实例
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
