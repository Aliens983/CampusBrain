package com.kb.interfaces.rest;

import com.kb.application.service.IDocumentApplicationService;
import com.kb.interfaces.dto.ApiResponse;
import com.kb.interfaces.dto.DocumentDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * REST controller for document management.
 * @author forever-king
 */
@Tag(name = "文档管理", description = "文档上传、查询、删除等管理接口")
@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentController {

    /** 文档应用服务 */
    private final IDocumentApplicationService documentService;

    @Operation(summary = "上传文档", description = "上传文档文件（支持 PDF/MD/DOCX/TXT/HTML），后台异步处理")
    @PostMapping("/upload")
    public ApiResponse<Map<String, Object>> uploadDocument(
            @Parameter(description = "文档文件，最大 50MB") @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ApiResponse.badRequest("文件不能为空");
        }

        Long docId = documentService.uploadDocument(file);
        return ApiResponse.success(Map.of(
                "documentId", docId,
                "fileName", file.getOriginalFilename(),
                "message", "文档已上传，正在后台处理中..."
        ));
    }

    @Operation(summary = "获取文档详情", description = "根据文档 ID 查询文档元信息")
    @GetMapping("/{id}")
    public ApiResponse<DocumentDTO> getDocument(
            @Parameter(description = "文档 ID") @PathVariable Long id) {
        return ApiResponse.success(DocumentDTO.from(documentService.getDocument(id)));
    }

    @Operation(summary = "获取文档列表",
            description = "分页列出当前用户有权查看的文档：普通用户仅返回自己上传的，ADMIN 返回全部")
    @GetMapping
    public ApiResponse<List<DocumentDTO>> listDocuments(
            @Parameter(description = "页码，0 基") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页条数，最大 100") @RequestParam(defaultValue = "20") int size) {
        List<DocumentDTO> docs = documentService.listVisibleDocuments(page, size).stream()
                .map(DocumentDTO::from)
                .toList();
        return ApiResponse.success(docs);
    }

    @Operation(summary = "删除文档",
            description = "删除指定文档及其所有关联数据（向量、索引、分块）。文档 owner 本人或 ADMIN 可删除")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteDocument(
            @Parameter(description = "文档 ID") @PathVariable Long id) {
        documentService.deleteDocument(id);
        return ApiResponse.success();
    }

    @Operation(summary = "搜索文档",
            description = "按标题关键词分页搜索当前用户有权查看的文档（模糊匹配下推 SQL，普通用户仅搜自己的）")
    @GetMapping("/search")
    public ApiResponse<List<DocumentDTO>> searchDocuments(
            @Parameter(description = "搜索关键词") @RequestParam String keyword,
            @Parameter(description = "页码，0 基") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页条数，最大 100") @RequestParam(defaultValue = "20") int size) {
        List<DocumentDTO> results = documentService.searchVisibleDocuments(keyword, page, size).stream()
                .map(DocumentDTO::from)
                .toList();
        return ApiResponse.success(results);
    }
}
