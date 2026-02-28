package ynu.jackielinn.server.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ynu.jackielinn.server.service.FileService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

@Service
public class FileServiceImpl implements FileService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");

    @Value("${web.upload.base-dir:uploads}")
    private String uploadBaseDir;

    @Value("${app.base-url:http://localhost:8088}")
    private String baseUrl;

    /**
     * 保存用户头像到配置目录，文件名格式 userId_timestamp.扩展名；返回完整访问 URL。
     *
     * @param userId 用户 id
     * @param file   图片文件（仅允许 jpg、jpeg、png、gif、webp）
     * @return 完整访问 URL
     * @throws IllegalArgumentException 文件为空、类型不允许或保存失败
     */
    @Override
    public String saveAvatar(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择要上传的图片");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("文件名无效");
        }
        String ext = getExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("仅支持 jpg、jpeg、png、gif、webp 格式的图片");
        }

        String filename = userId + "_" + System.currentTimeMillis() + "." + ext;
        Path basePath = Paths.get(uploadBaseDir, "avatar").toAbsolutePath().normalize();
        Path targetFile = basePath.resolve(filename);

        try {
            Files.createDirectories(basePath);
            file.transferTo(targetFile.toFile());
        } catch (Exception e) {
            throw new IllegalArgumentException("保存文件失败: " + e.getMessage());
        }

        String urlPath = "/uploads/avatar/" + filename;
        return baseUrl.endsWith("/") ? baseUrl + urlPath.substring(1) : baseUrl + urlPath;
    }

    /**
     * 从文件名中解析扩展名（小写）。
     *
     * @param filename 文件名
     * @return 扩展名，无扩展名返回空字符串
     */
    private static String getExtension(String filename) {
        int i = filename.lastIndexOf('.');
        return i > 0 ? filename.substring(i + 1).toLowerCase() : "";
    }
}
