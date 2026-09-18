package com.kb.infrastructure.rag.parser;

import com.kb.domain.document.DocumentTypeRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 解析器注册中心 — 管理所有 {@link DocumentParserSpi} 实现
 * <p>
 * Spring 自动发现标注了 {@code @Component} 的解析器实现并注册
 * 通过扩展名查找时按优先级排序，同扩展名取最高优先级
 * </p>
 * <p>
 * 同时是文件类型判据的南向端口实现（{@link DocumentTypeRegistry}）：
 * 上传校验以这里为准，扩展名白名单只增不删，与 Q-03 的判据收敛保持一致。
 * </p>
 *
 * @author forever-king
 */
@Slf4j
@Component
public class ParserRegistry implements DocumentTypeRegistry {

    /** 扩展名 → 排序后的解析器列表（优先级最高在前） */
    private final Map<String, List<DocumentParserSpi>> registry = new ConcurrentHashMap<>();

    /** 所有已注册解析器 */
    private final List<DocumentParserSpi> allParsers =
            Collections.synchronizedList(new ArrayList<>());

    /**
     * 注册解析器（由 Spring 构造函数注入调用）
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
     * 获取支持指定扩展名的最高优先级解析器
     */
    public Optional<DocumentParserSpi> getParser(String extension) {
        if (extension == null) return Optional.empty();
        List<DocumentParserSpi> parsers = registry.get(extension.toLowerCase());
        if (parsers == null || parsers.isEmpty()) return Optional.empty();
        return Optional.of(parsers.get(0));
    }

    /**
     * 获取支持指定扩展名的所有解析器（用于 fallback 链）
     */
    public List<DocumentParserSpi> getParserChain(String extension) {
        if (extension == null) return List.of();
        return registry.getOrDefault(extension.toLowerCase(), List.of());
    }

    /**
     * 该文件类型是否注册了可用解析器（上传类型校验的判据唯一源）。
     */
    @Override
    public boolean supports(String fileType) {
        return fileType != null && !getParserChain(fileType).isEmpty();
    }

    /**
     * 返回当前所有受支持（可解析）的扩展名集合（已排序副本）。
     * 上传校验应以这里为准，避免"白名单允许、实际无解析器"的错位。
     */
    @Override
    public Set<String> supportedExtensions() {
        return new TreeSet<>(registry.keySet());
    }

    /**
     * 返回所有已注册解析器的统计信息
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
