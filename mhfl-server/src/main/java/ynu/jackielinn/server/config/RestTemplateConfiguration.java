package ynu.jackielinn.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate 配置类。提供用于调用 Python FastAPI 的 RestTemplate，配置连接与读取超时。
 */
@Configuration
public class RestTemplateConfiguration {

    /**
     * 创建 RestTemplate，连接超时 5 秒、读取超时 10 秒，用于请求 Python 训练服务等。
     *
     * @return 配置好超时的 RestTemplate 实例
     */
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);  // 连接超时5秒
        factory.setReadTimeout(10000);    // 读取超时10秒
        return new RestTemplate(factory);
    }
}
