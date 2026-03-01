package ynu.jackielinn.server.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * FlowUtils 单元测试：限流一次检查。
 */
@ExtendWith(MockitoExtension.class)
class FlowUtilsTest {

    @Mock
    private StringRedisTemplate template;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private FlowUtils flowUtils;

    @BeforeEach
    void setUp() {
        // lenient: 部分用例只调用 hasKey 不调用 opsForValue，避免 UnnecessaryStubbingException
        lenient().when(template.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void shouldReturnFalseWhenKeyAlreadyExists() {
        when(template.hasKey("existing-key")).thenReturn(true);

        boolean result = flowUtils.limitOnceCheck("existing-key", 3);

        assertFalse(result);
        verify(template).hasKey("existing-key");
        verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void shouldReturnTrueAndSetKeyWhenKeyDoesNotExist() {
        when(template.hasKey("new-key")).thenReturn(false);

        boolean result = flowUtils.limitOnceCheck("new-key", 5);

        assertTrue(result);
        verify(template).hasKey("new-key");
        verify(valueOperations).set(eq("new-key"), eq(""), eq(5L), eq(TimeUnit.SECONDS));
    }

    @Test
    void shouldUseBlockTimeZeroWhenProvided() {
        when(template.hasKey("key")).thenReturn(false);

        flowUtils.limitOnceCheck("key", 0);

        verify(valueOperations).set(eq("key"), eq(""), eq(0L), eq(TimeUnit.SECONDS));
    }

    @Test
    void shouldUseBlockTimePositiveWhenProvided() {
        when(template.hasKey("key")).thenReturn(false);

        flowUtils.limitOnceCheck("key", 10);

        verify(valueOperations).set(eq("key"), eq(""), eq(10L), eq(TimeUnit.SECONDS));
    }
}
