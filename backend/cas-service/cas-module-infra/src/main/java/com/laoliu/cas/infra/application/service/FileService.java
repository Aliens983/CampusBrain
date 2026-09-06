package com.laoliu.cas.infra.application.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;

/**
 * 文件存储应用层服务接口
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
}
