package com.kb.domain.rag;

/**
 * 流式回答因客户端断开被取消。
 * <p>
 * 属于"调用方主动终止"而非供应商故障：
 * 不计入熔断器失败率（{@code ignoreExceptions}），也不向用户回推错误、不落错误消息。
 * </p>
 * <p>
 * 2.7（深度审查 P2）：取消是领域信号，本异常从 {@code infrastructure.rag.llm}
 * 移入 {@code domain.rag}，与 {@link CancellationToken} 同层，消除应用层对
 * 基础设施异常的编译期反向依赖。
 * </p>
 *
 * @author forever-king
 */
public class StreamCancelledException extends RuntimeException {

    public StreamCancelledException() {
        super("stream cancelled by client");
    }
}