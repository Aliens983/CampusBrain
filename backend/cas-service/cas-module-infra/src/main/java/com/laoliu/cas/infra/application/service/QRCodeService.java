package com.laoliu.cas.infra.application.service;

/**
 * 二维码生成应用层服务接口。
 *
 * @author forever-king
 */
public interface QRCodeService {

    /** 生成二维码并上传 */
    String generateQrCode(String content);
}
