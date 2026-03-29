package ynu.jackielinn.server.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 系统资源模块接口集成测试。
 * 覆盖 Controller -> Service 链路，外部 FastAPI 调用全部 mock。
 */
class ResourceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * 成功场景：获取系统资源成功。
     */
    @Test
    void getSystemResourcesShouldReturnSuccess() throws Exception {
        String body = """
                {
                  "code": 200,
                  "data": {
                    "cpu": {
                      "usage_percent": 35.5,
                      "cores": 8,
                      "cores_logical": 16
                    },
                    "memory": {
                      "total": 32.0,
                      "used": 12.5,
                      "free": 19.5,
                      "usage_percent": 39.1
                    },
                    "gpu": {
                      "total": 12.0,
                      "used": 3.6,
                      "free": 8.4,
                      "usage_percent": 30.0
                    }
                  },
                  "message": "success"
                }
                """;
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok(body));

        mockMvc.perform(get("/api/system/resources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.cpu.usagePercent").value(35.5))
                .andExpect(jsonPath("$.data.memory.total").value(32.0))
                .andExpect(jsonPath("$.data.gpu.used").value(3.6));
    }

    /**
     * 失败场景：FastAPI 调用失败时接口返回 5xx。
     */
    @Test
    void getSystemResourcesShouldReturnFailureWhenFastApiUnavailable() throws Exception {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenThrow(new RuntimeException("FastAPI unavailable"));

        assertThatThrownBy(() -> mockMvc.perform(get("/api/system/resources")))
                .hasRootCauseInstanceOf(RuntimeException.class)
                .hasMessageContaining("获取系统资源失败");
    }
}
