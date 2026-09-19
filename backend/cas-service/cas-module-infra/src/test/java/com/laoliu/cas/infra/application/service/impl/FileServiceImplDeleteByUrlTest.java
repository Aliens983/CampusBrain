package com.laoliu.cas.infra.application.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FileServiceImpl#deleteByUrl 安全测试（2.11）。
 * <p>
 * 重点不是"能删文件"，而是删除路径必须被 canonical 目录包含校验约束在上传根内：
 * {@code /uploads/../../x} 式 URL 穿越绝不能删掉根目录之外的任何文件；
 * query/锚点尾巴要剥掉；删除整体 fail-open、对不存在文件幂等。
 *
 * @author forever-king
 */
class FileServiceImplDeleteByUrlTest {

    @TempDir
    Path tempDir;

    private Path uploadRoot;
    private FileServiceImpl fileService;

    @BeforeEach
    void setUp() {
        uploadRoot = tempDir.resolve("uploads");
        fileService = new FileServiceImpl();
        ReflectionTestUtils.setField(fileService, "uploadDir", uploadRoot.toString());
        ReflectionTestUtils.setField(fileService, "urlPrefix", "/uploads");
        ReflectionTestUtils.setField(fileService, "maxSize", 10485760L);
    }

    private Path seedFile(String relativePath, String content) throws IOException {
        Path file = uploadRoot.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
        return file;
    }

    @Test
    @DisplayName("正常 URL：删除上传根内对应文件")
    void shouldDeleteFileInsideUploadRoot() throws IOException {
        Path target = seedFile("carousel/abc.png", "img");

        fileService.deleteByUrl("/uploads/carousel/abc.png");

        assertFalse(Files.exists(target), "根目录内文件应被删除");
    }

    @Test
    @DisplayName("路径穿越：/uploads/../../outside.txt 不得删除上传根之外的文件")
    void shouldNotDeleteOutsideUploadRoot() throws IOException {
        // 在上传根外（tempDir 下）放一个"受害者"文件，穿越 URL 解析后指向它
        Path victim = tempDir.resolve("outside.txt");
        Files.writeString(victim, "secret");

        fileService.deleteByUrl("/uploads/../outside.txt");

        assertTrue(Files.exists(victim), "canonical 目录包含校验必须拦截越界删除");
    }

    @Test
    @DisplayName("深层穿越：/uploads/carousel/../../../../etc 形态同样被拦截")
    void shouldNotDeleteOutsideUploadRootDeepTraversal() throws IOException {
        Path victim = tempDir.resolve("deep-outside.txt");
        Files.writeString(victim, "secret");

        fileService.deleteByUrl("/uploads/carousel/../../deep-outside.txt");

        assertTrue(Files.exists(victim), "深层 ../ 穿越必须被拦截");
    }

    @Test
    @DisplayName("带 query/锚点的 URL：剥掉尾巴后仍能正确删除目标文件")
    void shouldStripQueryAndFragmentBeforeDelete() throws IOException {
        Path target = seedFile("carousel/x.png", "img");

        fileService.deleteByUrl("/uploads/carousel/x.png?token=abc&t=1#frag");

        assertFalse(Files.exists(target));
    }

    @Test
    @DisplayName("目标文件不存在：静默幂等，不抛异常")
    void shouldBeIdempotentWhenFileMissing() {
        fileService.deleteByUrl("/uploads/carousel/never-existed.png");
        // 无异常即通过
    }

    @Test
    @DisplayName("null/空白 URL：直接忽略")
    void shouldIgnoreBlankUrl() {
        fileService.deleteByUrl(null);
        fileService.deleteByUrl("   ");
    }

    @Test
    @DisplayName("仅前缀本身（剥完为空）：忽略，不删目录")
    void shouldIgnorePrefixOnlyUrl() {
        fileService.deleteByUrl("/uploads");
        assertTrue(Files.notExists(uploadRoot) || Files.isDirectory(uploadRoot));
    }

    @Test
    @DisplayName("往返闭环：字节版 uploadFile 返回的 URL 可被 deleteByUrl 精确清理")
    void shouldRoundTripUploadThenDelete() {
        String url = fileService.uploadFile(new byte[]{1, 2, 3}, "pic.png", "carousel");
        Path stored = uploadRoot.resolve("carousel").resolve(url.substring(url.lastIndexOf('/') + 1));
        assertTrue(Files.exists(stored), "前置：字节上传落盘成功");

        fileService.deleteByUrl(url);

        assertFalse(Files.exists(stored), "uploadFile 返回的 URL 必须能反向清理物理文件");
    }
}
