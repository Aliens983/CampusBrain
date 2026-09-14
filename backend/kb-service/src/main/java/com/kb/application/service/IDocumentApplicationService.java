package com.kb.application.service;

import com.kb.domain.document.Document;
import com.kb.domain.document.DocumentStatus;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文档管理应用服务接口
 * @author forever-king
 */
public interface IDocumentApplicationService {

    Long uploadDocument(MultipartFile file);

    Document getDocument(Long id);

    /**
     * 列出当前登录用户可见的文档（普通用户仅自己上传的，ADMIN 全部）。
     */
    List<Document> listVisibleDocuments();

    /**
     * 按标题关键词搜索当前登录用户可见的文档。
     */
    List<Document> searchVisibleDocuments(String keyword);

    void deleteDocument(Long id);

    DocumentStatus getDocumentStatus(Long id);
}
