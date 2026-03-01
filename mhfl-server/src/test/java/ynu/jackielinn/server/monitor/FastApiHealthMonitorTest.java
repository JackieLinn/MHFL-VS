package ynu.jackielinn.server.monitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * FastApiHealthMonitor 单元测试：健康检查请求、响应解析与异常分支。
 */
@ExtendWith(MockitoExtension.class)
class FastApiHealthMonitorTest {

    private static final String BASE_URL = "http://localhost:8000";

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private FastApiHealthMonitor monitor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(monitor, "pythonFastApiUrl", BASE_URL);
    }

    @Test
    void shouldCallCorrectUrlWhenCheckFastApiHealth() {
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(),
                eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"code\":200,\"message\":\"ok\",\"data\":{\"status\":\"healthy\"}}"));

        monitor.checkFastApiHealth();

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(restTemplate).exchange(urlCaptor.capture(), eq(HttpMethod.GET), any(), eq(String.class));
        assertEquals(BASE_URL + "/api/health", urlCaptor.getValue());
    }

    @Test
    void shouldNotThrowWhenResponseIsHealthy() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"code\":200,\"message\":\"ok\",\"data\":{\"status\":\"healthy\"}}"));

        assertDoesNotThrow(() -> monitor.checkFastApiHealth());
    }

    @Test
    void shouldNotThrowWhenResponseCode200ButStatusNotHealthy() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"code\":200,\"message\":\"warn\",\"data\":{\"status\":\"unhealthy\"}}"));

        assertDoesNotThrow(() -> monitor.checkFastApiHealth());
    }

    @Test
    void shouldNotThrowWhenResponseCode200ButDataNull() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"code\":200,\"message\":\"ok\"}"));

        assertDoesNotThrow(() -> monitor.checkFastApiHealth());
    }

    @Test
    void shouldNotThrowWhenResponseCodeNot200() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"code\":500,\"message\":\"error\"}"));

        assertDoesNotThrow(() -> monitor.checkFastApiHealth());
    }

    @Test
    void shouldNotThrowWhenResponseBodyNull() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok().build());

        assertDoesNotThrow(() -> monitor.checkFastApiHealth());
    }

    @Test
    void shouldNotThrowWhenHttpStatusNot2xx() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("error"));

        assertDoesNotThrow(() -> monitor.checkFastApiHealth());
    }

    @Test
    void shouldCatchRestClientExceptionAndNotThrow() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenThrow(new RestClientException("Connection refused"));

        assertDoesNotThrow(() -> monitor.checkFastApiHealth());
    }

    @Test
    void shouldCatchGenericExceptionAndNotThrow() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenThrow(new RuntimeException("unexpected"));

        assertDoesNotThrow(() -> monitor.checkFastApiHealth());
    }

    @Test
    void shouldUseConfiguredBaseUrl() {
        ReflectionTestUtils.setField(monitor, "pythonFastApiUrl", "http://custom:9000");
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"code\":200,\"message\":\"ok\",\"data\":{\"status\":\"healthy\"}}"));

        monitor.checkFastApiHealth();

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(restTemplate).exchange(urlCaptor.capture(), eq(HttpMethod.GET), any(), eq(String.class));
        assertEquals("http://custom:9000/api/health", urlCaptor.getValue());
    }
}
