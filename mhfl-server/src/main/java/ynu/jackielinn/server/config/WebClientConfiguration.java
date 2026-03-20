package ynu.jackielinn.server.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * WebClient 配置。用于流式调用 Python FastAPI（如智能助手 chat/stream），
 * 连接超时 10 秒、读写超时 5 分钟，适配 LLM 长时响应。
 */
@Configuration
public class WebClientConfiguration {

    @Value("${python.fastapi.url:http://localhost:8000}")
    private String pythonFastApiUrl;

    @Value("${assistant.stream.connect-timeout-ms:10000}")
    private int assistantStreamConnectTimeoutMs;

    @Value("${assistant.stream.response-timeout-ms:900000}")
    private long assistantStreamResponseTimeoutMs;

    @Value("${assistant.stream.read-timeout-seconds:900}")
    private long assistantStreamReadTimeoutSeconds;

    @Value("${assistant.stream.write-timeout-seconds:120}")
    private long assistantStreamWriteTimeoutSeconds;

    @Bean
    public WebClient assistantWebClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, assistantStreamConnectTimeoutMs)
                .responseTimeout(Duration.ofMillis(assistantStreamResponseTimeoutMs))
                .doOnConnected(conn -> {
                    conn.addHandlerLast(new ReadTimeoutHandler(assistantStreamReadTimeoutSeconds, TimeUnit.SECONDS));
                    conn.addHandlerLast(new WriteTimeoutHandler(assistantStreamWriteTimeoutSeconds, TimeUnit.SECONDS));
                });

        return WebClient.builder()
                .baseUrl(pythonFastApiUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
