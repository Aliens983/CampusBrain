package com.kb.infrastructure.rag.llm;

/**
 * LLM/Embedding 供应商在所有尝试（主供应商 + 备选供应商）后仍不可用时抛出。
 * <p>
 * 该异常向外抛出是为了让 Resilience4j 熔断器真实统计失败率：
 * 此前异常在服务内部被吞成普通字符串返回，熔断器永远处于 CLOSED，
 * 供应商持续故障期间既不快速失败也不会触发熔断告警。
 * </p>
 *
 * @author forever-king
 */
public class LlmUnavailableException extends RuntimeException {

    public LlmUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public LlmUnavailableException(String message) {
        super(message);
    }
}
