package com.kb.infrastructure.rag.parser;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 解析器注册中心 — 管理所有 {@link DocumentParserSpi} 实现。
 * <p>
 * Spring 自动发现标注了 {@code @Component} 的解析器实现并注册。
 * 通过扩展名查找时按优先级排序，同扩展名取最高优先级。
 * </p>
 *
 * @author forever-king
 */
@Slf4j
@Component
public class ParserRegistry {

    /** 扩展名 → 排序后的解析器列表（优先级最高在前） */
    private final Map<String, List<DocumentParserSpi>> registry = new ConcurrentHashMap<>();

    /** 所有已注册解析器 */
    private final List<DocumentParserSpi> allParsers =
            Collections.synchronizedList(new ArrayList<>());

    /**
     * 注册解析器（由 Spring 构造函数注入调用）。
     */
    public void register(DocumentParserSpi parser) {
        allParsers.add(parser);
        for (String ext : parser.supportedExtensions()) {
            registry.computeIfAbsent(ext.toLowerCase(), k -> new ArrayList<>()).add(parser);
            // 保持优先级排序
            registry.get(ext.toLowerCase()).sort(Comparator.comparingInt(DocumentParserSpi::priority));
        }
        log.info("Registered parser: {} for extensions: {} (priority={})",
                parser.getName(), parser.supportedExtensions(), parser.priority());
    }

    /**
     * 获取支持指定扩展名的最高优先级解析器。
     */
    public Optional<DocumentParserSpi> getParser(String extension) {
        if (extension == null) return Optional.empty();
        List<DocumentParserSpi> parsers = registry.get(extension.toLowerCase());
        if (parsers == null || parsers.isEmpty()) return Optional.empty();
        return Optional.of(parsers.get(0));
    }

    /**
     * 获取支持指定扩展名的所有解析器（用于 fallback 链）。
     */
    public List<DocumentParserSpi> getParserChain(String extension) {
        if (extension == null) return List.of();
        return registry.getOrDefault(extension.toLowerCase(), List.of());
    }

    /**
     * 返回所有已注册解析器的统计信息。
     */
    public Map<String, Object> stats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalParsers", allParsers.size());
        stats.put("supportedExtensions", registry.keySet().size());
        stats.put("parsers", allParsers.stream().map(p -> Map.of(
                "name", p.getName(),
                "extensions", p.supportedExtensions(),
                "priority", p.priority()
        )).toList());
        return stats;
    }
}
