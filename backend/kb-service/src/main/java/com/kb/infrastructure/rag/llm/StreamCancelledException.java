package com.kb.infrastructure.rag.llm;

/**
 * 流式回答因客户端断开被取消。
 * <p>
 * 属于"调用方主动终止"而非供应商故障：
 * 不计入熔断器失败率（{@code ignoreExceptions}），也不向用户回推错误、不落错误消息。
 * </p>
 *
 * @author forever-king
 */
public class StreamCancelledException extends RuntimeException {

    public StreamCancelledException() {
        super("stream cancelled by client");
    }
}
