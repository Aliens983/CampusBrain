package com.kb.infrastructure.persistence.qdrant;

import com.google.common.util.concurrent.Futures;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;
import io.qdrant.client.grpc.Collections.CollectionOperationResponse;
import io.qdrant.client.grpc.JsonWithInt.Value;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link QdrantSupport} 单元测试（Q-05 抽取的 Qdrant 公共样板）。
 *
 * @author forever-king
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("QdrantSupport 公共样板测试")
class QdrantSupportTest {

    @Mock
    private QdrantClient qdrantClient;

    private QdrantSupport support() {
        return new QdrantSupport(qdrantClient);
    }

    @Test
    @DisplayName("toValue: null 存为空串，各基础类型走 Qdrant 原生 Value")
    void convertsValues() {
        assertThat(support().toValue(null).hasStringValue()).isTrue();
        assertThat(support().toValue("x").getStringValue()).isEqualTo("x");
        assertThat(support().toValue(7).getIntegerValue()).isEqualTo(7L);
        assertThat(support().toValue(7L).getIntegerValue()).isEqualTo(7L);
        assertThat(support().toValue(1.5d).getDoubleValue()).isEqualTo(1.5d);
        assertThat(support().toValue(1.5f).getDoubleValue()).isEqualTo(1.5d);
        assertThat(support().toValue(true).getBoolValue()).isTrue();
        // 复杂对象退化为 toString（与抽取前行为一致）
        assertThat(support().toValue(List.of(1, 2)).getStringValue()).isEqualTo("[1, 2]");
    }

    @Test
    @DisplayName("fromValue: 四态还原 + null 兜底")
    void convertsFromValue() {
        Value stringValue = support().toValue("s");
        Value longValue = support().toValue(3L);
        Value doubleValue = support().toValue(2.0d);
        Value boolValue = support().toValue(false);
        assertThat(support().fromValue(stringValue)).isEqualTo("s");
        assertThat(support().fromValue(longValue)).isEqualTo(3L);
        assertThat(support().fromValue(doubleValue)).isEqualTo(2.0d);
        assertThat(support().fromValue(boolValue)).isEqualTo(false);
        assertThat(support().fromValue(null)).isNull();
    }

    @Test
    @DisplayName("toFloatList: 装箱保序")
    void convertsFloatArray() {
        List<Float> list = support().toFloatList(new float[]{1.0f, 2.5f});
        assertThat(list).containsExactly(1.0f, 2.5f);
    }

    @Test
    @DisplayName("ensureCollection: 已存在时跳过创建")
    void skipsWhenExists() throws Exception {
        when(qdrantClient.collectionExistsAsync("c"))
                .thenReturn(Futures.immediateFuture(true));

        assertThat(support().ensureCollection("c", 8, "Cosine", Distance.Dot, true)).isTrue();
        verify(qdrantClient, never()).createCollectionAsync(anyString(), any(VectorParams.class));
    }

    @Test
    @DisplayName("ensureCollection: 不存在时创建（Cosine 配置映射 Cosine 度量）")
    void createsWhenAbsent() throws Exception {
        when(qdrantClient.collectionExistsAsync("c"))
                .thenReturn(Futures.immediateFuture(false));
        when(qdrantClient.createCollectionAsync(anyString(), any(VectorParams.class)))
                .thenReturn(Futures.immediateFuture(CollectionOperationResponse.newBuilder().build()));

        assertThat(support().ensureCollection("c", 8, "Cosine", Distance.Euclid, true)).isTrue();
        verify(qdrantClient).createCollectionAsync(anyString(), any(VectorParams.class));
    }

    @Test
    @DisplayName("ensureCollection: failFast=false 时创建失败不抛异常返回 false（缓存旁路语义）")
    void swallowsWhenNotFailFast() {
        when(qdrantClient.collectionExistsAsync("c"))
                .thenReturn(Futures.immediateFailedFuture(new ExecutionException(new RuntimeException("boom"))));

        assertThatCode(() -> {
            boolean ok = support().ensureCollection("c", 8, "Cosine", Distance.Dot, false);
            assertThat(ok).isFalse();
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("ensureCollection: failFast=true 时创建失败包装 IllegalStateException（启动失败语义）")
    void throwsWhenFailFast() {
        when(qdrantClient.collectionExistsAsync("c"))
                .thenReturn(Futures.immediateFailedFuture(new ExecutionException(new RuntimeException("boom"))));

        assertThatThrownBy(() -> support().ensureCollection("c", 8, "Cosine", Distance.Dot, true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("c");
    }
}
