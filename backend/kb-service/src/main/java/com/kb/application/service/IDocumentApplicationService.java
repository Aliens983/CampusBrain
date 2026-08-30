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

    List<Document> getAllDocuments();

    void deleteDocument(Long id);

    DocumentStatus getDocumentStatus(Long id);
}
