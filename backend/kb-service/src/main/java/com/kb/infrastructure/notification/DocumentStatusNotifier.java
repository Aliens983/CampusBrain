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
 * 文档状态变更通知 — 监听领域事件并通过 WebSocket 推送给文档所属用户
 *
 * <p>4.6（深度审查 P2）：此前除按归属用户推送外，还向公共频道
 * {@code /topic/notifications} 广播含文档标题与错误信息的 payload；
 * 该频道面向所有连接用户，会造成文档标题/错误细节越权可见（WebSocket
 * 未鉴权或泄露给无关会话），已移除公共频道广播，仅保留按归属用户私推。
 * @author forever-king
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentStatusNotifier {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 异步监听 {@link DocumentProcessedEvent}，推送给文档所属用户
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

        // 推送到文档所有者的私有队列（4.6：不再向 /topic/notifications 公共频道广播）
        String userDestination = "/queue/document-status";
        messagingTemplate.convertAndSendToUser(
                String.valueOf(event.getOwnerId()), userDestination, payload);

        log.info("Document status notification sent: docId={}, status={}",
                event.getDocumentId(), event.getStatus());
    }
}
