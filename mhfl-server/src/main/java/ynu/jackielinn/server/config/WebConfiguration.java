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

@Configuration
public class WebConfiguration implements WebMvcConfigurer {

    @Value("${web.upload.base-dir:uploads}")
    private String uploadBaseDir;

    @Bean
    BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path basePath = Paths.get(uploadBaseDir).toAbsolutePath().normalize();
        String location = "file:" + basePath + "/";
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }

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
