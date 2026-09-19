package com.laoliu.cas.infra.application.service.impl;

import com.laoliu.cas.common.exception.BusinessException;
import com.laoliu.cas.common.exception.code.CommonErrorCode;
import com.laoliu.cas.infra.api.file.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 本地文件存储。
 * <p>
 * 安全约束（4.1.14）：
 * <ul>
 *   <li>扩展名白名单：仅常见图片与办公文档，<b>不含 svg/html 等可承载脚本的类型</b>；</li>
 *   <li>存储名固定为 32 位 UUID + 校验过的小写扩展名，天然杜绝双扩展名/可执行文件；</li>
 *   <li>subDir 仅允许单层安全字符，落盘前再做 normalize + 目录包含校验，防御路径穿越；</li>
 *   <li>大小双重限制：Spring multipart 配置 + service 内显式校验。</li>
 * </ul>
 *
 * @author forever-king
 */
@Slf4j
@Service
public class FileServiceImpl implements FileService {

    /** 允许上传的扩展名（小写、带点）。svg 可内嵌脚本故不开放。 */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp",
            ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",
            ".txt", ".md");

    /** 子目录白名单：单层目录、字母数字下划线短横、1-32 字符 */
    private static final Pattern SUBDIR_PATTERN = Pattern.compile("[a-zA-Z0-9_-]{1,32}");

    @Value("${file.upload.dir:uploads}")
    private String uploadDir;

    @Value("${file.upload.url-prefix:/uploads}")
    private String urlPrefix;

    /** service 层显式大小上限（字节），与 Spring multipart 配置构成双重限制 */
    @Value("${file.upload.max-size:10485760}")
    private long maxSize;

    @Override
    public String uploadFile(MultipartFile multipartFile) {
        return uploadFile(multipartFile, null);
    }

    @Override
    public String uploadFile(File file) {
        return uploadFile(file, null);
    }

    @Override
    public String uploadFile(MultipartFile file, String subDir) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(CommonErrorCode.FILE_EMPTY);
        }
        if (file.getSize() > maxSize) {
            throw new BusinessException(CommonErrorCode.FILE_TOO_LARGE);
        }
        String extension = resolveAllowedExtension(file.getOriginalFilename());
        File dest = prepareDestination(extension, subDir);
        try {
            // 用绝对路径：Tomcat 对相对路径的 transferTo 会解析到临时目录
            file.transferTo(dest);
        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new BusinessException(CommonErrorCode.FILE_UPLOAD_FAILED);
        }
        log.info("文件上传成功: {}", dest.getAbsolutePath());
        return buildAccessUrl(subDir, dest);
    }

    @Override
    public String uploadFile(File file, String subDir) {
        if (file == null || !file.exists()) {
            throw new BusinessException(CommonErrorCode.FILE_EMPTY);
        }
        if (file.length() > maxSize) {
            throw new BusinessException(CommonErrorCode.FILE_TOO_LARGE);
        }
        String extension = resolveAllowedExtension(file.getName());
        File dest = prepareDestination(extension, subDir);
        try (FileOutputStream fos = new FileOutputStream(dest)) {
            fos.write(java.nio.file.Files.readAllBytes(file.toPath()));
        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new BusinessException(CommonErrorCode.FILE_UPLOAD_FAILED);
        }
        log.info("文件上传成功: {}", dest.getAbsolutePath());
        return buildAccessUrl(subDir, dest);
    }

    /**
     * 校验并提取扩展名：必须存在、在白名单内；统一转小写，
     * 避免 {@code .PNG} 与 {@code .png.png} 之类的混淆。
     */
    private String resolveAllowedExtension(String originalFilename) {
        if (originalFilename == null) {
            throw new BusinessException(CommonErrorCode.FILE_TYPE_NOT_ALLOWED);
        }
        int dot = originalFilename.lastIndexOf('.');
        if (dot < 0 || dot == originalFilename.length() - 1) {
            throw new BusinessException(CommonErrorCode.FILE_TYPE_NOT_ALLOWED);
        }
        // 文件名中再次出现路径分隔符也视为非法
        if (originalFilename.contains("/") || originalFilename.contains("\\")) {
            throw new BusinessException(CommonErrorCode.FILE_PATH_INVALID);
        }
        String extension = originalFilename.substring(dot).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            log.warn("拒绝非白名单文件类型: original={}, ext={}", originalFilename, extension);
            throw new BusinessException(CommonErrorCode.FILE_TYPE_NOT_ALLOWED);
        }
        return extension;
    }

    /**
     * 准备目标文件：校验 subDir、创建目录、normalize 后确认结果仍在上传根目录内。
     */
    private File prepareDestination(String extension, String subDir) {
        File baseDir = new File(uploadDir).getAbsoluteFile();
        File dir;
        if (subDir == null || subDir.isEmpty()) {
            dir = baseDir;
        } else {
            if (!SUBDIR_PATTERN.matcher(subDir).matches()) {
                throw new BusinessException(CommonErrorCode.FILE_PATH_INVALID);
            }
            dir = new File(baseDir, subDir);
        }
        // 规范化后做目录包含校验，杜绝任何形式的 ../ 穿越。
        // 必须用 getCanonicalFile()：getAbsoluteFile() 只补全相对路径，
        // 不解析 .. 也不解析符号链接，起不到"规范化"的作用（此前变量名
        // 叫 canonicalDir 却用的是 getAbsoluteFile，属于纸糊的防御）。
        // getCanonicalFile 会抛 IOException，失败时按拒绝处理（fail-closed）。
        File canonicalDir;
        try {
            canonicalDir = dir.getCanonicalFile();
        } catch (java.io.IOException e) {
            log.warn("上传目录规范化失败，拒绝写入: subDir={}", subDir, e);
            throw new BusinessException(CommonErrorCode.FILE_PATH_INVALID);
        }
        String basePath;
        try {
            basePath = baseDir.getCanonicalFile().getPath();
        } catch (java.io.IOException e) {
            log.error("上传根目录规范化失败: {}", uploadDir, e);
            throw new BusinessException(CommonErrorCode.FILE_PATH_INVALID);
        }
        String dirPath = canonicalDir.getPath();
        if (!dirPath.equals(basePath) && !dirPath.startsWith(basePath + File.separator)) {
            log.warn("拒绝越界的上传目录: subDir={}, resolved={}", subDir, dirPath);
            throw new BusinessException(CommonErrorCode.FILE_PATH_INVALID);
        }
        if (!canonicalDir.exists() && !canonicalDir.mkdirs()) {
            log.error("创建上传目录失败: {}", canonicalDir.getAbsolutePath());
            throw new BusinessException(CommonErrorCode.FILE_UPLOAD_FAILED);
        }
        String newFileName = UUID.randomUUID().toString().replace("-", "") + extension;
        return new File(canonicalDir, newFileName);
    }

    private String buildAccessUrl(String subDir, File destFile) {
        // 存储名与扩展名均由服务端生成，subDir 已过白名单，拼接无注入风险
        String fileName = destFile.getName();
        return (subDir == null || subDir.isEmpty()
                ? urlPrefix
                : urlPrefix + "/" + subDir) + "/" + fileName;
    }
}
