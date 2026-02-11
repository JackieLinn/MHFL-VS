package ynu.jackielinn.server.monitor;

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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * FastAPI 健康监控服务
 * 定期检查 FastAPI 服务的健康状态
 */
@Slf4j
@Service
public class FastApiHealthMonitor {

    @Value("${python.fastapi.url:http://localhost:8000}")
    private String pythonFastApiUrl;

    @Resource
    private RestTemplate restTemplate;

    /**
     * 检查FastAPI健康状态
     * 每 30 分钟执行一次（1800000毫秒）
     */
    @Scheduled(fixedRate = 1800000)
    public void checkFastApiHealth() {
        try {
            String url = pythonFastApiUrl + "/api/health";

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
                Integer code = jsonResponse.getInteger("code");
                String message = jsonResponse.getString("message");

                if (code != null && code == 200) {
                    JSONObject data = jsonResponse.getJSONObject("data");
                    String status = data != null ? data.getString("status") : "unknown";

                    if ("healthy".equals(status)) {
                        log.info("FastAPI服务健康检查通过: {}", message);
                    } else {
                        log.warn("FastAPI服务状态异常: status={}, message={}", status, message);
                    }
                } else {
                    log.warn("FastAPI健康检查返回异常: code={}, message={}", code, message);
                }
            } else {
                log.warn("FastAPI健康检查失败: HTTP状态码={}", response.getStatusCode());
            }
        } catch (RestClientException e) {
            log.error("FastAPI健康检查请求失败: {}", e.getMessage());
        } catch (Exception e) {
            log.error("FastAPI健康检查异常", e);
        }
    }
}
