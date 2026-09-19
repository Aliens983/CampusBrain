package com.laoliu.cas.infra.api.file;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;

/**
 * 文件存储跨模块服务接口（2.3：契约独立至 cas-module-infra-api，实现在 cas-module-infra）
 *
 * @author forever-king
 */
public interface FileService {

    /** 上传MultipartFile文件 */
    String uploadFile(MultipartFile file);

    /** 上传File文件 */
    String uploadFile(File file);

    /** 上传 MultipartFile 到 uploads/<subDir>/（如 carousel） */
    String uploadFile(MultipartFile file, String subDir);

    /** 上传 File 到 uploads/<subDir>/（如 captcha） */
    String uploadFile(File file, String subDir);

    /**
     * 上传字节内容到 uploads/<subDir>/（2.10：供不依赖 Servlet 的应用层调用）。
     *
     * @param content          文件字节
     * @param originalFilename 原始文件名（仅用于扩展名白名单校验）
     * @param subDir           单层子目录（如 carousel）
     * @return 文件访问 URL
     */
    String uploadFile(byte[] content, String originalFilename, String subDir);

    /**
     * 按访问 URL 删除本地文件（2.11：业务记录删除时清理物理文件，防孤儿堆积）。
     * <p>文件不存在或 URL 无法解析到上传根目录内时静默返回（幂等、fail-open），
     * 避免存储层故障阻塞业务删除。
     *
     * @param accessUrl 此前 uploadFile 返回的访问 URL
     */
    void deleteByUrl(String accessUrl);
}
