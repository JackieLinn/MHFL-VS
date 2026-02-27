package ynu.jackielinn.server.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Web 层配置类。负责密码编码器、静态资源映射（上传文件 /uploads/**）以及上传目录初始化。
 */
@Configuration
public class WebConfiguration implements WebMvcConfigurer {

    @Value("${web.upload.base-dir:uploads}")
    private String uploadBaseDir;

    /**
     * 提供 BCrypt 密码编码器，用于用户密码加密与校验。
     *
     * @return BCryptPasswordEncoder 实例
     */
    @Bean
    BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 将 /uploads/** 请求映射到配置的上传根目录（file:...），用于访问头像等上传文件。
     *
     * @param registry 资源处理器注册表
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path basePath = Paths.get(uploadBaseDir).toAbsolutePath().normalize();
        String location = "file:" + basePath + "/";
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }

    /**
     * 应用启动后创建上传所需目录（如 avatar），若创建失败则抛出 IllegalStateException。
     */
    @PostConstruct
    public void initUploadDirs() {
        try {
            Path avatarDir = Paths.get(uploadBaseDir, "avatar").toAbsolutePath().normalize();
            Files.createDirectories(avatarDir);
        } catch (Exception e) {
            throw new IllegalStateException("无法创建上传目录: " + e.getMessage());
        }
    }
}
