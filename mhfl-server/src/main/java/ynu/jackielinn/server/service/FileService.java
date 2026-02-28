package ynu.jackielinn.server.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileService {

    /**
     * 保存用户头像，文件名格式：userId_timestamp.扩展名
     * 不删除历史文件，仅新增；展示时以用户表中 avatar 字段（最新 URL）为准。
     *
     * @param userId 用户 ID
     * @param file   图片文件（仅允许 jpg、jpeg、png、gif、webp）
     * @return 完整访问 URL（存入数据库，用于前端展示）
     * @throws IllegalArgumentException 文件为空、类型不允许等
     */
    String saveAvatar(Long userId, MultipartFile file);
}
