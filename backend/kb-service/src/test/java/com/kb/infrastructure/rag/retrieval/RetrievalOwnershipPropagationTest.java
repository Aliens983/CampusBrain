package com.kb.infrastructure.rag.retrieval;

import com.kb.domain.rag.RetrievalResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 检索归属传播回归测试（P1-01）。
 * <p>
 * P1-01 的根因是：混合检索在 {@code retrievalExecutor} 池线程上执行，而归属用户
 * 原先靠在检索器内部读 SecurityContext（MODE_THREADLOCAL）获得 —— 池线程从未
 * set 过身份，ownerId 恒为 null，导致<b>用户私有文档永远无法被召回</b>。
 * <p>
 * 该缺陷此前测不出来的原因是：原有用例用同步执行器（direct executor）作替身，
 * 任务在提交线程上跑，ThreadLocal 恰好可用。因此本测试<b>必须使用真实线程池</b>，
 * 才能复现跨线程场景。
 *
 * @author forever-king
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("检索归属传播（P1-01 回归）")
class RetrievalOwnershipPropagationTest {

    @Mock
    private KeywordRetriever keywordRetriever;
    @Mock
    private VectorRetriever vectorRetriever;

    private ExecutorService executor;

    @AfterEach
    void shutdown() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    private HybridRetriever newRetriever() {
        // 真实线程池：任务在池线程执行，而非提交线程
        executor = Executors.newFixedThreadPool(2);
        HybridRetriever retriever =
                new HybridRetriever(keywordRetriever, vectorRetriever, executor);
        ReflectionTestUtils.setField(retriever, "finalTopK", 10);
        ReflectionTestUtils.setField(retriever, "rrfK", 60.0);
        ReflectionTestUtils.setField(retriever, "retrievalTimeoutSeconds", 30);
        return retriever;
    }

    @Test
    @DisplayName("池线程执行检索时，ownerId 是请求方传入的用户，而非恒为 null")
    void ownerIdIsPropagatedToPoolThreads() {
        Long alice = 100L;
        when(keywordRetriever.retrieve(anyString(), eq(alice))).thenReturn(List.of());
        when(vectorRetriever.retrieve(anyString(), eq(alice))).thenReturn(List.of());

        newRetriever().hybridRetrieve("我的私有文档写了什么", alice);

        // 关键断言：两个检索器收到的必须是 alice。
        // 修复前这里拿到的是 null（池线程取不到 SecurityContext）。
        verify(keywordRetriever).retrieve(eq("我的私有文档写了什么"), eq(alice));
        verify(vectorRetriever).retrieve(eq("我的私有文档写了什么"), eq(alice));
    }

    @Test
    @DisplayName("不同用户各自携带自己的 ownerId，互不串号")
    void differentUsersCarryTheirOwnOwnerId() {
        Long alice = 100L;
        Long bob = 200L;
        when(keywordRetriever.retrieve(anyString(), eq(alice))).thenReturn(List.of());
        when(vectorRetriever.retrieve(anyString(), eq(alice))).thenReturn(List.of());
        when(keywordRetriever.retrieve(anyString(), eq(bob)))
                .thenReturn(List.of(RetrievalResult.builder().content("bob-doc").build()));
        when(vectorRetriever.retrieve(anyString(), eq(bob))).thenReturn(List.of());

        HybridRetriever retriever = newRetriever();
        retriever.hybridRetrieve("问题", alice);
        retriever.hybridRetrieve("问题", bob);

        verify(keywordRetriever).retrieve(eq("问题"), eq(alice));
        verify(keywordRetriever).retrieve(eq("问题"), eq(bob));
    }
}
