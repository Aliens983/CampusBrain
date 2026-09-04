package com.kb.domain.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Value object representing a parsed document after extraction.
 * Contains clean text content and metadata extracted by the parser.
 * @author forever-king
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParsedDocument {

    /** 文档标题（从文件名或内容中提取） */
    private String title;

    /** 解析后的纯文本内容 */
    private String content;

    /** 原始文件类型（如 pdf、md、txt、xlsx 等） */
    private String fileType;

    /** 从文档中提取的元数据（作者、页数、创建日期等） */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * Get a metadata value safely.
     */
    public Object getMetadataValue(String key) {
        return metadata != null ? metadata.get(key) : null;
    }

    /**
     * Estimated character count.
     */
    public int getCharCount() {
        return content != null ? content.length() : 0;
    }
}
