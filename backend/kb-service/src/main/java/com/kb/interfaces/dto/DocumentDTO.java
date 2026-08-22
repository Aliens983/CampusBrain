package com.kb.interfaces.dto;

import com.kb.domain.document.Document;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档信息 DTO — 替代 Map&lt;String, Object&gt; 的强类型响应。
 *
 * @author forever-king
 */
@Data
@Builder
@Schema(description = "文档信息")
public class DocumentDTO {

    @Schema(description = "文档 ID")
    private Long id;
    @Schema(description = "文档标题")
    private String title;
    @Schema(description = "文件类型")
    private String fileType;
    @Schema(description = "文件大小(字节)")
    private Long fileSize;
    @Schema(description = "处理状态")
    private String status;
    @Schema(description = "分块数量")
    private Integer chunkCount;
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    public static DocumentDTO from(Document doc) {
        return DocumentDTO.builder()
                .id(doc.getId())
                .title(doc.getTitle())
                .fileType(doc.getFileType())
                .fileSize(doc.getFileSize())
                .status(doc.getStatus().name())
                .chunkCount(doc.getChunkCount())
                .createdAt(doc.getCreatedAt())
                .build();
    }
}
