package com.kb.infrastructure.rag.parser;

import com.kb.infrastructure.common.ParseException;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 文档解析器工厂 — 委托给 {@link ParserRegistry} 和 {@link ParserChain}。
 * <p>
 * 保持向后兼容的 API，内部使用 SPI 注册中心和责任链模式。
 * </p>
 *
 * @author forever-king
 */
@Component
public class ParserFactory {

    private final ParserRegistry registry;
    private final ParserChain chain;

    /**
     * 启动时自动注册所有 {@link DocumentParserSpi} 实现。
     */
    public ParserFactory(java.util.List<DocumentParserSpi> parsers,
                         ParserRegistry registry, ParserChain chain) {
        this.registry = registry;
        this.chain = chain;
        for (DocumentParserSpi parser : parsers) {
            registry.register(parser);
        }
    }

    /**
     * 获取支持指定文件类型的解析器（最高优先级）。
     *
     * @throws IllegalArgumentException 无支持的解析器
     */
    public DocumentParser getParser(String fileType) {
        String ext = fileType != null ? fileType.toLowerCase() : "";
        DocumentParserSpi spi = registry.getParser(ext)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unsupported file type: " + fileType +
                        ". Supported: " + getSupportedTypes()));
        return new DocumentParser() {
            @Override
            public boolean supports(String ft) {
                return spi.supports(ft);
            }
            @Override
            public com.kb.domain.rag.ParsedDocument parse(
                    java.io.InputStream inputStream, String fileName) throws ParseException {
                return spi.parse(inputStream, fileName);
            }
        };
    }

    /**
     * 使用解析器链解析（带 fallback）。
     */
    public com.kb.domain.rag.ParsedDocument parseWithFallback(
            java.io.InputStream stream, String fileName) throws ParseException {
        return chain.parseAuto(stream, fileName);
    }

    public boolean isSupported(String fileType) {
        return fileType != null && registry.getParser(fileType.toLowerCase()).isPresent();
    }

    public Set<String> getSupportedTypes() {
        Set<String> types = new java.util.HashSet<>();
        types.add("pdf");
        types.add("md");
        types.add("markdown");
        types.add("txt");
        types.add("xlsx");
        types.add("xls");
        return types;
    }
}
