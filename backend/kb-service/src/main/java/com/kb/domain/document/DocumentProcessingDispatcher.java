package com.kb.domain.document;

/**
 * 文档处理投递南向端口（A-01 应用层端口化）。
 * <p>
 * 声明"把文档处理任务投递到异步处理链路"这一领域能力，由基础设施层实现
 * （当前为 RabbitMQ 消息生产者）。应用层在事务提交后调用本端口触发异步
 * 解析与入库，不直接耦合具体消息中间件。
 * </p>
 *
 * @author forever-king
 */
public interface DocumentProcessingDispatcher {

    /**
     * 投递文档处理消息。
     *
     * @param documentId 待异步处理的文档 ID
     */
    void send(Long documentId);
}