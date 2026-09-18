package com.kb.infrastructure.adapter;

import com.kb.domain.document.KnowledgeCacheInvalidator;
import com.kb.infrastructure.cache.KnowledgeCacheEvictor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 问答缓存淘汰端口的实现适配器。
 * <p>
 * {@link KnowledgeCacheEvictor} 位于基础设施 cache 包（跨 AI 协作中归属对方），
 * 本适配器仅做委托转发，不改动其源码，让应用层经 {@link KnowledgeCacheInvalidator}
 * 触发文档变更后的两级问答缓存淘汰（12-04）。行为与原调用完全一致。
 * </p>
 *
 * @author forever-king
 */
@Component
@RequiredArgsConstructor
public class KnowledgeCacheEvictorInvalidator implements KnowledgeCacheInvalidator {

    private final KnowledgeCacheEvictor delegate;

    @Override
    public void evictAllQaCaches(String reason) {
        delegate.evictAllQaCaches(reason);
    }
}