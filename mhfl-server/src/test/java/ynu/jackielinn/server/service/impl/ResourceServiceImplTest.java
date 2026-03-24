package ynu.jackielinn.server.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import ynu.jackielinn.server.dto.response.SystemResourcesVO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceServiceImplTest {

    @InjectMocks
    private ResourceServiceImpl service;

    @Mock
    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "pythonFastApiUrl", "http://localhost:8000");
    }

    @Test
    void getSystemResourcesShouldReturnVoWithGpu() {
        String body = """
                {
                  "code":200,
                  "data":{
                    "cpu":{"usage_percent":10.5,"cores":8,"cores_logical":16},
                    "memory":{"total":32.0,"used":12.0,"free":20.0,"usage_percent":37.5},
                    "gpu":{"total":24.0,"used":6.0,"free":18.0,"usage_percent":25.0}
                  }
                }
                """;
        when(restTemplate.exchange(eq("http://localhost:8000/api/resource/system/check"), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(body, HttpStatus.OK));

        SystemResourcesVO result = service.getSystemResources();

        assertThat(result.getCpu().getUsagePercent()).isEqualTo(10.5);
        assertThat(result.getMemory().getTotal()).isEqualTo(32.0);
        assertThat(result.getGpu()).isNotNull();
        assertThat(result.getGpu().getUsagePercent()).isEqualTo(25.0);
    }

    @Test
    void getSystemResourcesShouldReturnVoWithoutGpu() {
        String body = """
                {
                  "code":200,
                  "data":{
                    "cpu":{"usage_percent":10.5,"cores":8,"cores_logical":16},
                    "memory":{"total":32.0,"used":12.0,"free":20.0,"usage_percent":37.5}
                  }
                }
                """;
        when(restTemplate.exchange(eq("http://localhost:8000/api/resource/system/check"), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(body, HttpStatus.OK));

        SystemResourcesVO result = service.getSystemResources();

        assertThat(result.getGpu()).isNull();
        assertThat(result.getCpu().getCores()).isEqualTo(8);
    }

    @Test
    void getSystemResourcesShouldThrowWhenCodeNot200() {
        when(restTemplate.exchange(eq("http://localhost:8000/api/resource/system/check"), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>("{\"code\":500,\"message\":\"bad\"}", HttpStatus.OK));

        assertThatThrownBy(() -> service.getSystemResources())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("获取系统资源失败");
    }

    @Test
    void getSystemResourcesShouldThrowWhenNon2xxOrBodyNull() {
        when(restTemplate.exchange(eq("http://localhost:8000/api/resource/system/check"), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>((String) null, HttpStatus.OK));
        assertThatThrownBy(() -> service.getSystemResources())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("获取系统资源失败");

        when(restTemplate.exchange(eq("http://localhost:8000/api/resource/system/check"), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>("{\"code\":200}", HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> service.getSystemResources())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("获取系统资源失败");
    }

    @Test
    void getSystemResourcesShouldThrowWhenRestThrows() {
        when(restTemplate.exchange(eq("http://localhost:8000/api/resource/system/check"), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenThrow(new RuntimeException("boom"));

        assertThatThrownBy(() -> service.getSystemResources())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("boom");
    }
}

