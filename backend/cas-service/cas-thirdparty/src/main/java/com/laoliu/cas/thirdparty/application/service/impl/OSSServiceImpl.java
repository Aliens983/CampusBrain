package com.laoliu.cas.thirdparty.application.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.PutObjectRequest;
import com.laoliu.cas.common.exception.BusinessException;
import com.laoliu.cas.common.exception.code.CommonErrorCode;
import com.laoliu.cas.thirdparty.application.service.OSSService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

/**
 * @author forever-king
 */
@Slf4j
@Service
public class OSSServiceImpl implements OSSService {

    @Value("${aliyun.oss.endpoint}")
    private String endpoint;

    @Value("${aliyun.oss.access-key-id}")
    private String accessKeyId;

    @Value("${aliyun.oss.access-key-secret}")
    private String accessKeySecret;

    @Value("${aliyun.oss.bucket-name}")
    private String bucketName;

    @Override
    public String uploadFile(MultipartFile file) {
        long startMs = System.currentTimeMillis();
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        try {
            String originalFilename = file.getOriginalFilename();
            String fileName = UUID.randomUUID() + "-" + originalFilename;
            InputStream inputStream = file.getInputStream();

            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, fileName, inputStream);
            ossClient.putObject(putObjectRequest);

            // 生成带签名的临时访问URL（有效期1小时），避免私有bucket无法访问
            java.util.Date expiration = new java.util.Date(System.currentTimeMillis() + 3600 * 1000);
            java.net.URL signedUrl = ossClient.generatePresignedUrl(bucketName, fileName, expiration);
            log.info("oss_upload_completed bytes={} durationMs={}",
                    file.getSize(), System.currentTimeMillis() - startMs);
            return signedUrl.toString();
        } catch (Exception e) {
            // 不打印签名 URL / AK/SK，只保留大小与耗时
            log.error("oss_upload_failed bytes={} durationMs={}",
                    file.getSize(), System.currentTimeMillis() - startMs, e);
            throw new BusinessException(CommonErrorCode.FILE_UPLOAD_FAILED);
        } finally {
            ossClient.shutdown();
        }
    }
}
