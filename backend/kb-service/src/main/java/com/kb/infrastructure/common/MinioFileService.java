package com.kb.infrastructure.common;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;

/**
 * File storage service backed by MinIO (S3-compatible).
 *
 * @author forever-king
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MinioFileService {

    /** MinIO客户端实例 */
    private final MinioClient minioClient;

    /** MinIO存储桶名称 */
    @Value("${minio.bucket}")
    private String bucket;

    /**
     * Upload a file to MinIO.
     *
     * @param objectName  unique object name in the bucket
     * @param inputStream file content
     * @param size        file size in bytes
     * @param contentType MIME type
     */
    public void upload(String objectName, InputStream inputStream,
                        long size, String contentType) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .stream(inputStream, size, -1)
                            .contentType(contentType)
                            .build()
            );
            log.info("Uploaded file to MinIO: bucket={}, object={}, size={}",
                    bucket, objectName, size);
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file to MinIO: " + objectName, e);
        }
    }

    /**
     * Download a file from MinIO.
     *
     * @param objectName the object name in the bucket
     * @return file content as InputStream
     */
    public InputStream getObject(String objectName) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to download file from MinIO: " + objectName, e);
        }
    }

    /**
     * Delete a file from MinIO.
     */
    public void delete(String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build()
            );
            log.info("Deleted file from MinIO: bucket={}, object={}", bucket, objectName);
        } catch (Exception e) {
            log.warn("Failed to delete file from MinIO: {}", objectName, e);
        }
    }
}
