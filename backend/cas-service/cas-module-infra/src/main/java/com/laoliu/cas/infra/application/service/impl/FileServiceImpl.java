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
        File destFile = new File(uploadDir, newFileName);

        try {
            File parentDir = destFile.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
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
}
