package com.laoliu.cas.infra.application.service.impl;

import com.laoliu.cas.common.exception.BusinessException;
import com.laoliu.cas.common.exception.code.CommonErrorCode;
import com.laoliu.cas.infra.application.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

/**
 * @author forever-king
 */
@Slf4j
@Service
public class FileServiceImpl implements FileService {

    @Value("${file.upload.dir:uploads}")
    private String uploadDir;

    @Value("${file.upload.url-prefix:/uploads}")
    private String urlPrefix;

    @Override
    public String uploadFile(MultipartFile multipartFile) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new BusinessException(CommonErrorCode.FILE_EMPTY);
        }

        String originalFilename = multipartFile.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String newFileName = UUID.randomUUID().toString().replace("-", "") + extension;
        // 用绝对路径：Tomcat 对相对路径的 transferTo 会解析到临时目录，导致找不到文件
        File baseDir = new File(uploadDir).getAbsoluteFile();
        File destFile = new File(baseDir, newFileName);

        try {
            if (!baseDir.exists()) {
                baseDir.mkdirs();
            }
            multipartFile.transferTo(destFile);
            log.info("文件上传成功: {}", destFile.getAbsolutePath());
            return urlPrefix + "/" + newFileName;
        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new BusinessException(CommonErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    @Override
    public String uploadFile(File file) {
        if (file == null || !file.exists()) {
            throw new BusinessException(CommonErrorCode.FILE_EMPTY);
        }

        String originalFilename = file.getName();
        String extension = "";
        if (originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String newFileName = UUID.randomUUID().toString().replace("-", "") + extension;
        File destFile = new File(uploadDir, newFileName);

        try {
            // 先确保目标目录存在，再开流写入（顺序不能反）
            File parentDir = destFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            try (FileOutputStream fos = new FileOutputStream(destFile)) {
                fos.write(java.nio.file.Files.readAllBytes(file.toPath()));
            }
            log.info("文件上传成功: {}", destFile.getAbsolutePath());
            return urlPrefix + "/" + newFileName;
        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new BusinessException(CommonErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    @Override
    public String uploadFile(MultipartFile file, String subDir) {
        String url = storeMultipart(file, subDir);
        return url;
    }

    @Override
    public String uploadFile(File file, String subDir) {
        String url = storeFile(file, subDir);
        return url;
    }

    /** 目标目录：uploads[/subDir] 绝对路径，不存在则创建 */
    private File resolveDir(String subDir) {
        File base = (subDir == null || subDir.isEmpty())
                ? new File(uploadDir)
                : new File(uploadDir, subDir);
        File dir = base.getAbsoluteFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    private String storeMultipart(MultipartFile multipartFile, String subDir) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new BusinessException(CommonErrorCode.FILE_EMPTY);
        }
        String originalFilename = multipartFile.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String newFileName = UUID.randomUUID().toString().replace("-", "") + extension;
        File dir = resolveDir(subDir);
        try {
            multipartFile.transferTo(new File(dir, newFileName));
            return (subDir == null || subDir.isEmpty() ? urlPrefix : urlPrefix + "/" + subDir) + "/" + newFileName;
        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new BusinessException(CommonErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    private String storeFile(File file, String subDir) {
        if (file == null || !file.exists()) {
            throw new BusinessException(CommonErrorCode.FILE_EMPTY);
        }
        String originalFilename = file.getName();
        String extension = "";
        if (originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String newFileName = UUID.randomUUID().toString().replace("-", "") + extension;
        File dir = resolveDir(subDir);
        try {
            File dest = new File(dir, newFileName);
            try (FileOutputStream fos = new FileOutputStream(dest)) {
                fos.write(java.nio.file.Files.readAllBytes(file.toPath()));
            }
            return (subDir == null || subDir.isEmpty() ? urlPrefix : urlPrefix + "/" + subDir) + "/" + newFileName;
        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new BusinessException(CommonErrorCode.FILE_UPLOAD_FAILED);
        }
    }
}
