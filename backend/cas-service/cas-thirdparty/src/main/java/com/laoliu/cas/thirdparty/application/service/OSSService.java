package com.laoliu.cas.thirdparty.application.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 对象存储应用层服务接口。
 *
 * @author forever-king
 */
public interface OSSService {

    /** 上传文件到OSS */
    String uploadFile(MultipartFile file);
}
