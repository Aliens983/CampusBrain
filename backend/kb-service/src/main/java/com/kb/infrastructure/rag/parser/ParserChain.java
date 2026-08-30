package com.kb.infrastructure.rag.parser;

import com.kb.domain.rag.ParsedDocument;
import com.kb.infrastructure.common.ParseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

/**
 * 解析器责任链 — 主解析器失败时自动 fallback 到下一个
 * <p>
 * 使用方式：
 * <pre>
 * ParsedDocument result = parserChain.parse(stream, "file.pdf",
 *         registry.getParserChain("pdf"));
 * </pre>
 * 按优先级依次尝试，全部失败则抛出 {@link ParseException}
 * </p>
 *
 * @author forever-king
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ParserChain {

    private final ParserRegistry registry;

    /**
     * 使用解析器链解析文档（带 fallback）
     *
     * @param stream   文档输入流
     * @param fileName 原始文件名
     * @param parsers  按优先级排序的解析器列表
     * @return 解析结果
     * @throws ParseException 所有解析器均失败
     */
    public ParsedDocument parse(InputStream stream, String fileName,
                                 List<DocumentParserSpi> parsers) throws ParseException {
        if (parsers == null || parsers.isEmpty()) {
            throw new ParseException("No parser available for file: " + fileName);
        }

        ParseException lastException = null;

        for (int i = 0; i < parsers.size(); i++) {
            DocumentParserSpi parser = parsers.get(i);
            try {
                log.debug("Attempting parser [{}/{}]: {} for {}",
                        i + 1, parsers.size(), parser.getName(), fileName);
                return parser.parse(stream, fileName);
            } catch (ParseException e) {
                log.warn("Parser {} failed for {}: {} — trying next",
                        parser.getName(), fileName, e.getMessage());
                lastException = e;
            } catch (Exception e) {
                log.warn("Parser {} threw unexpected error for {}: {}",
                        parser.getName(), fileName, e.getMessage());
                lastException = new ParseException(
                        "Unexpected error in " + parser.getName() + ": " + e.getMessage(), e);
            }
        }

        throw new ParseException(
                "All parsers failed for file: " + fileName +
                ". Last error: " + (lastException != null ? lastException.getMessage() : "unknown"),
                lastException);
    }

    /**
     * 便捷方法：根据扩展名自动获取解析器链并执行
     */
    public ParsedDocument parseAuto(InputStream stream, String fileName) throws ParseException {
        String ext = extractExtension(fileName);
        List<DocumentParserSpi> chain = registry.getParserChain(ext);
        if (chain.isEmpty()) {
            throw new ParseException("Unsupported file type: " + ext);
        }
        return parse(stream, fileName, chain);
    }

    private String extractExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) return "unknown";
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }
}
