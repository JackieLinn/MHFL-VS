package ynu.jackielinn.server.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc / OpenAPI 配置类。定义 API 文档元信息、JWT 安全方案及接口分组（公开/需认证）。
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "${api.title: Model Heterogeneous Federated Learning End-to-End Visualization and Simulation Platform}",
                version = "${api.version: v1}",
                description = "${api.description: 云南大学2022级软件工程专业毕业设计项目}",
                contact = @Contact(name = "Xiaoyi Lin", url = "https://github.com/JackieLinn"),
                license = @License(name = "Apache 2.0", url = "https://www.apache.org/licenses/LICENSE-2.0")
        )
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer"
)
public class SpringDocConfiguration {

    /**
     * 定义可匿名调用的 API 分组，包含 /auth/**、/doc/** 等路径。
     *
     * @return 公开接口分组 GroupedOpenApi
     */
    @Bean
    public GroupedOpenApi publicApi() {
        String[] publicPaths = {"/doc/**", "/auth/**", "/captcha/**", "/error/**", "/uploads/**", "/ws/**"};
        return GroupedOpenApi.builder().group("可匿名调用的API接口")
                .pathsToMatch(publicPaths)
                .build();
    }

    /**
     * 定义需认证后可调用的 API 分组，包含 /api/** 路径。
     *
     * @return 需认证接口分组 GroupedOpenApi
     */
    @Bean
    public GroupedOpenApi protectedApi() {
        return GroupedOpenApi.builder().group("需认证后才可调用的API接口")
                .pathsToMatch("/api/**")
                .build();
    }
}
