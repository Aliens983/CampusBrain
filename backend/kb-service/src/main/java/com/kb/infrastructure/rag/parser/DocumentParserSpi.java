package com.kb.infrastructure.rag.parser;

import com.kb.domain.rag.ParsedDocument;
import com.kb.infrastructure.common.ParseException;

import java.io.InputStream;
import java.util.Set;

/**
 * 文档解析器 SPI（Service Provider Interface）
 * <p>
 * 新增文件格式支持只需实现此接口并注册到 {@link ParserRegistry}，
 * 无需修改任何现有代码。符合开闭原则（OCP）
 * </p>
 *
 * <h3>实现指南</h3>
 * <ol>
 *   <li>实现 {@link #parse(InputStream, String)} 方法</li>
 *   <li>通过 {@link #supportedExtensions()} 声明支持的扩展名</li>
 *   <li>通过 {@link #priority()} 指定优先级（数字越小越优先）</li>
 *   <li>标注 {@code @Component} 即可被自动发现</li>
 * </ol>
 *
 * @author forever-king
 */
public interface DocumentParserSpi {

    /**
     * 解析文档输入流为纯文本和元数据
     *
     * @param inputStream 文档输入流（调用方负责关闭）
     * @param fileName    原始文件名（用于日志和元数据）
     * @return 解析结果
     * @throws ParseException 解析失败
     */
    ParsedDocument parse(InputStream inputStream, String fileName) throws ParseException;

    /**
     * 此解析器支持的扩展名集合（小写，不含点）
     * <p>
     * 示例：{@code Set.of("pdf", "md", "markdown", "txt")}
     * </p>
     */
    Set<String> supportedExtensions();

    /**
     * 解析器优先级。数字越小优先级越高
     * - 1-10: 专用解析器（PDFBox、POI 等）
     * - 11-50: 通用解析器（Tika 等）
     * - 51+: 兜底解析器
     */
    default int priority() { return 50; }

    /**
     * 是否支持给定文件扩展名
     */
    default boolean supports(String extension) {
        return supportedExtensions().contains(extension.toLowerCase());
    }

    /**
     * 解析器显示名称（用于日志和诊断）
     */
    default String getName() {
        return getClass().getSimpleName();
    }
}
