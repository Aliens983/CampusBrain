package com.kb.domain.document;

import java.util.Set;

/**
 * 文档类型解析器注册表南向端口（A-01 应用层端口化；Q-03 判据唯一源）。
 * <p>
 * 声明"某个文件类型是否可被解析"这一领域能力。上传时以本端口为准做类型校验，
 * 避免"白名单放行、异步处理时才报不支持"的错位；白名单/支持类型收敛与此处一致。
 * </p>
 *
 * @author forever-king
 */
public interface DocumentTypeRegistry {

    /**
     * 该文件类型是否注册了可用的解析器链。
     *
     * @param fileType 小写扩展名，如 "txt"、"pdf"
     * @return true 表示可处理
     */
    boolean supports(String fileType);

    /**
     * 当前所有受支持（可解析）的扩展名集合，用于错误提示。
     *
     * @return 支持的扩展名集合（已排序副本）
     */
    Set<String> supportedExtensions();

    /**
     * 从文件名中提取扩展名（文件类型判据的入参来源，唯一实现于端口层）。
     * <p>
     * 收敛原 DocumentDomainService / DocumentApplicationService 各一份的复制实现，
     * 避免"同一字符串提取逻辑多处漂移"（Q-03）。
     *
     * @param fileName 原始文件名；可能包含路径
     * @return 小写扩展名；无扩展名或尾部点号时返回 {@code "unknown"}
     */
    default String extractFileType(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "unknown";
        }
        String ext = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        // 尾部点号（如 "file."）没有实际扩展名，同样返回 unknown
        return ext.isEmpty() ? "unknown" : ext;
    }
}