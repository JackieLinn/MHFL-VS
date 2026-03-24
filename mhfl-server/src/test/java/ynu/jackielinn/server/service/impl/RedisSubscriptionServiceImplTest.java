package ynu.jackielinn.server.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.test.util.ReflectionTestUtils;
import ynu.jackielinn.server.listener.TrainingMessageListener;
import ynu.jackielinn.server.utils.Const;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RedisSubscriptionServiceImplTest {

    @InjectMocks
    private RedisSubscriptionServiceImpl service;

    @Mock
    private RedisMessageListenerContainer redisMessageListenerContainer;

    @Mock
    private TrainingMessageListener messageListener;

    @BeforeEach
    void setUp() {
        // no-op
    }

    @Test
    void subscribeTaskShouldSubscribeThreeChannels() {
        service.subscribeTask(11L);

        ArgumentCaptor<ChannelTopic> captor = ArgumentCaptor.forClass(ChannelTopic.class);
        verify(redisMessageListenerContainer, times(3))
                .addMessageListener(org.mockito.ArgumentMatchers.eq(messageListener), captor.capture());
        List<String> topics = captor.getAllValues().stream().map(ChannelTopic::getTopic).toList();
        assertThat(topics).containsExactlyInAnyOrder(
                Const.TASK_EXPERIMENT_ROUND + 11L,
                Const.TASK_EXPERIMENT_CLIENT + 11L,
                Const.TASK_EXPERIMENT_STATUS + 11L
        );
    }

    @Test
    void subscribeTaskShouldBeIdempotent() {
        service.subscribeTask(12L);
        service.subscribeTask(12L);

        verify(redisMessageListenerContainer, times(3))
                .addMessageListener(org.mockito.ArgumentMatchers.eq(messageListener), org.mockito.ArgumentMatchers.any(ChannelTopic.class));
    }

    @Test
    void unsubscribeTaskShouldDoNothingWhenNotSubscribed() {
        service.unsubscribeTask(13L);

        verify(redisMessageListenerContainer, never())
                .removeMessageListener(org.mockito.ArgumentMatchers.eq(messageListener), org.mockito.ArgumentMatchers.any(ChannelTopic.class));
    }

    @Test
    void unsubscribeTaskShouldRemoveThreeChannelsAndBeIdempotent() {
        service.subscribeTask(14L);
        service.unsubscribeTask(14L);
        service.unsubscribeTask(14L);

        ArgumentCaptor<ChannelTopic> captor = ArgumentCaptor.forClass(ChannelTopic.class);
        verify(redisMessageListenerContainer, times(3))
                .removeMessageListener(org.mockito.ArgumentMatchers.eq(messageListener), captor.capture());
        List<String> topics = captor.getAllValues().stream().map(ChannelTopic::getTopic).toList();
        assertThat(topics).containsExactlyInAnyOrder(
                Const.TASK_EXPERIMENT_ROUND + 14L,
                Const.TASK_EXPERIMENT_CLIENT + 14L,
                Const.TASK_EXPERIMENT_STATUS + 14L
        );
    }

    @Test
    void subscribedTasksSetShouldBeUpdated() {
        service.subscribeTask(15L);
        java.util.Set<Long> set = (java.util.Set<Long>) ReflectionTestUtils.getField(service, "subscribedTasks");
        assertThat(set).contains(15L);

        service.unsubscribeTask(15L);
        assertThat(set).doesNotContain(15L);
    }
}

