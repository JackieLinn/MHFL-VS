package ynu.jackielinn.server.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ynu.jackielinn.server.dto.response.SystemResourcesVO;
import ynu.jackielinn.server.service.ResourceService;

/**
 * 资源管理服务实现
 */
@Slf4j
@Service
public class ResourceServiceImpl implements ResourceService {

    @Value("${python.fastapi.url:http://localhost:8000}")
    private String pythonFastApiUrl;

    @Resource
    private RestTemplate restTemplate;

    /**
     * 调用 Python FastAPI /api/resource/system/check 获取 CPU、内存、GPU 信息并封装为 SystemResourcesVO。
     *
     * @return 系统资源 VO
     * @throws RuntimeException 调用失败或解析失败时抛出
     */
    @Override
    public SystemResourcesVO getSystemResources() {
        try {
            String url = pythonFastApiUrl + "/api/resource/system/check";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JSONObject jsonResponse = JSON.parseObject(response.getBody());

                // 检查响应格式
                if (jsonResponse.getInteger("code") == 200) {
                    JSONObject data = jsonResponse.getJSONObject("data");

                    // 解析CPU信息
                    JSONObject cpuJson = data.getJSONObject("cpu");
                    SystemResourcesVO.CPUInfo cpuInfo = SystemResourcesVO.CPUInfo.builder()
                            .usagePercent(cpuJson.getDouble("usage_percent"))
                            .cores(cpuJson.getInteger("cores"))
                            .coresLogical(cpuJson.getInteger("cores_logical"))
                            .build();

                    // 解析内存信息
                    JSONObject memoryJson = data.getJSONObject("memory");
                    SystemResourcesVO.MemoryInfo memoryInfo = SystemResourcesVO.MemoryInfo.builder()
                            .total(memoryJson.getDouble("total"))
                            .used(memoryJson.getDouble("used"))
                            .free(memoryJson.getDouble("free"))
                            .usagePercent(memoryJson.getDouble("usage_percent"))
                            .build();

                    // 解析GPU信息（可选）
                    SystemResourcesVO.GPUInfo gpuInfo = null;
                    JSONObject gpuJson = data.getJSONObject("gpu");
                    if (gpuJson != null) {
                        gpuInfo = SystemResourcesVO.GPUInfo.builder()
                                .total(gpuJson.getDouble("total"))
                                .used(gpuJson.getDouble("used"))
                                .free(gpuJson.getDouble("free"))
                                .usagePercent(gpuJson.getDouble("usage_percent"))
                                .build();
                    }

                    return SystemResourcesVO.builder()
                            .cpu(cpuInfo)
                            .memory(memoryInfo)
                            .gpu(gpuInfo)
                            .build();
                } else {
                    log.error("Python FastAPI返回错误: {}", jsonResponse.getString("message"));
                    throw new RuntimeException("获取系统资源失败: " + jsonResponse.getString("message"));
                }
            } else {
                log.error("调用Python FastAPI失败，状态码: {}", response.getStatusCode());
                throw new RuntimeException("调用Python FastAPI失败");
            }
        } catch (Exception e) {
            log.error("获取系统资源异常", e);
            throw new RuntimeException("获取系统资源失败: " + e.getMessage());
        }
    }
}
