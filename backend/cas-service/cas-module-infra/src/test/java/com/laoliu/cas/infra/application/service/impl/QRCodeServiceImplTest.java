package com.laoliu.cas.infra.application.service.impl;

import cn.hutool.extra.qrcode.QrCodeUtil;
import com.laoliu.cas.common.exception.BusinessException;
import com.laoliu.cas.common.exception.code.CommonErrorCode;
import com.laoliu.cas.infra.application.service.FileService;
import com.laoliu.cas.infra.application.service.QRCodeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.io.File;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * QRCodeServiceImpl 单元测试。
 *
 * @author forever-king
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("二维码服务单元测试")
class QRCodeServiceImplTest {

    @Mock
    private FileService fileService;

    private MockedStatic<QrCodeUtil> qrCodeUtilMock;

    private QRCodeService qrCodeService;

    private static final String CONTENT = "https://example.com/booking/123";
    private static final String FILE_PATH = "/uploads/qrcode-abc123.png";
    private static final String SERVER_ADDRESS = "http://localhost:18080";
    private static final String CONTEXT_PATH = "/api/v1";

    @BeforeEach
    void setUp() throws Exception {
        qrCodeUtilMock = mockStatic(QrCodeUtil.class);
        qrCodeService = new QRCodeServiceImpl(fileService);

        // 通过反射设置 @Value 注入的字段
        var serverField = QRCodeServiceImpl.class.getDeclaredField("serverAddress");
        serverField.setAccessible(true);
        serverField.set(qrCodeService, SERVER_ADDRESS);

        var contextField = QRCodeServiceImpl.class.getDeclaredField("contextPath");
        contextField.setAccessible(true);
        contextField.set(qrCodeService, CONTEXT_PATH);
    }

    @AfterEach
    void tearDown() {
        qrCodeUtilMock.close();
    }

    @Nested
    @DisplayName("生成二维码 - generateQrCode")
    class GenerateQrCodeTests {

        @Test
        @DisplayName("应当成功生成二维码并返回本地文件访问地址")
        void shouldGenerateQrCodeSuccessfully() {
            // Given
            BufferedImage mockImage = new BufferedImage(300, 300, BufferedImage.TYPE_INT_RGB);
            qrCodeUtilMock.when(() -> QrCodeUtil.generate(CONTENT, 300, 300)).thenReturn(mockImage);
            when(fileService.uploadFile(any(File.class))).thenReturn(FILE_PATH);

            // When
            String result = qrCodeService.generateQrCode(CONTENT);

            // Then
            assertNotNull(result);
            String expectedUrl = SERVER_ADDRESS + CONTEXT_PATH + FILE_PATH;
            assertEquals(expectedUrl, result);
            verify(fileService).uploadFile(any(File.class));
        }

        @Test
        @DisplayName("内容为 null 时应当抛出 BAD_REQUEST 异常")
        void shouldThrowExceptionWhenContentIsNull() {
            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> qrCodeService.generateQrCode(null));
            assertEquals(CommonErrorCode.BAD_REQUEST.getCode(), exception.getCode());
            verify(fileService, never()).uploadFile(any(File.class));
        }

        @Test
        @DisplayName("内容为空字符串时应当抛出 BAD_REQUEST 异常")
        void shouldThrowExceptionWhenContentIsBlank() {
            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> qrCodeService.generateQrCode("   "));
            assertEquals(CommonErrorCode.BAD_REQUEST.getCode(), exception.getCode());
            verify(fileService, never()).uploadFile(any(File.class));
        }
    }
}
