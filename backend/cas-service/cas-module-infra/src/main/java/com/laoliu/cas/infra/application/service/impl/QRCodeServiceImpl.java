package com.laoliu.cas.infra.application.service.impl;

import cn.hutool.extra.qrcode.QrCodeUtil;
import com.laoliu.cas.common.exception.BusinessException;
import com.laoliu.cas.common.exception.code.CommonErrorCode;
import com.laoliu.cas.infra.application.service.FileService;
import com.laoliu.cas.infra.application.service.QRCodeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * @author forever-king
 */
@Slf4j
@Service
public class QRCodeServiceImpl implements QRCodeService {

    private final FileService fileService;

    @Value("${file.upload.server-address:http://localhost:18080}")
    private String serverAddress;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    public QRCodeServiceImpl(FileService fileService) {
        this.fileService = fileService;
    }

    @Override
    public String generateQrCode(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST);
        }

        File tempFile = null;
        try {
            BufferedImage qrCodeImage = QrCodeUtil.generate(content, 300, 300);
            tempFile = File.createTempFile("qrcode-", ".png");
            ImageIO.write(qrCodeImage, "PNG", tempFile);

            String fileUrl = fileService.uploadFile(tempFile);
            String fullUrl = serverAddress + contextPath + fileUrl;
            log.info("二维码已生成并存储到本地: {}", fullUrl);
            return fullUrl;
        } catch (Exception e) {
            log.error("生成二维码失败", e);
            throw new BusinessException(CommonErrorCode.QR_CODE_FAILED);
        } finally {
            if (tempFile != null) {
                try { Files.deleteIfExists(tempFile.toPath()); } catch (IOException ignored) {}
            }
        }
    }
}
