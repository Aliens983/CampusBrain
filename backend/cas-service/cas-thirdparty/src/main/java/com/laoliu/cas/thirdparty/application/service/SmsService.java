package com.laoliu.cas.thirdparty.application.service;

/**
 * 短信应用层服务接口。
 *
 * @author forever-king
 */
public interface SmsService {
    /** 发送短信 */
    void sendSms(String phoneNumber, String templateCode, String templateParam);
}
