package com.kb.infrastructure.notification;

import com.kb.domain.event.DocumentProcessedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 文档状态变更通知 — 监听领域事件并通过 WebSocket 推送给用户。
 * @author forever-king
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentStatusNotifier {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 异步监听 {@link DocumentProcessedEvent}，推送给文档所属用户。
     */
    @Async
    @EventListener
    public void onDocumentProcessed(DocumentProcessedEvent event) {
        Map<String, Object> payload = Map.of(
                "documentId", event.getDocumentId(),
                "title", event.getDocumentTitle(),
                "status", event.getStatus().name(),
                "isReady", event.isReady(),
                "error", event.getErrorMsg() != null ? event.getErrorMsg() : ""
        );

        // 推送到文档所有者的私有队列
        String userDestination = "/queue/document-status";
        messagingTemplate.convertAndSendToUser(
                String.valueOf(event.getOwnerId()), userDestination, payload);

        // 同时推送到公共通知频道
        messagingTemplate.convertAndSend("/topic/notifications", payload);

        log.info("Document status notification sent: docId={}, status={}",
                event.getDocumentId(), event.getStatus());
    }
}
