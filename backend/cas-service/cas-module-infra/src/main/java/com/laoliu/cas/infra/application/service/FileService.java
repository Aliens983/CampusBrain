package com.laoliu.cas.infra.application.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;

/**
 * 文件存储应用层服务接口。
 *
 * @author forever-king
 */
public interface FileService {

    /** 上传MultipartFile文件 */
    String uploadFile(MultipartFile file);

    /** 上传File文件 */
    String uploadFile(File file);
}
