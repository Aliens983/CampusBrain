package com.kb.infrastructure.rag.embedding;

import com.kb.domain.rag.EmbeddingService;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * OpenAI Embedding service implementation via LangChain4j.
 * <p>
 * Uses text-embedding-3-small (default) or text-embedding-3-large.
 * 文本按可配置批次切分后，在 embeddingExecutor 上并发调用模型 API，
 * 各批次结果按提交顺序回填，保证返回向量与输入文本严格对齐。
 * </p>
 *
 * @author forever-king
 */
@Slf4j
@Service
@Primary
public class OpenAiEmbeddingService implements EmbeddingService {

    /** 嵌入模型实例 */
    private final EmbeddingModel embeddingModel;

    /** 模型名称 */
    @Value("${langchain4j.openai.embedding-model.model-name:text-embedding-3-small}")
    private String modelName;

    /**
     * 单次 API 请求携带的文本条数上限，可配置。
     * 切得越大单请求越慢、超限重试代价越高；32 条是吞吐与单请求延迟的平衡点。
     */
    @Value("${kb.embedding.batch-size:32}")
    private int batchSize;

    /** 单文档内批次并发执行器（由 AsyncExecutorConfig 提供，并发度可配） */
    @Resource(name = "embeddingExecutor")
    private ExecutorService embeddingExecutor;

    public OpenAiEmbeddingService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    @Override
    public float[] embed(String text) {
        if (text == null || text.isEmpty()) {
            return new float[0];
        }
        try {
            Embedding embedding = embeddingModel.embed(text).content();
            return embedding.vector();
        } catch (Exception e) {
            log.error("Embedding failed for text (length={})", text.length(), e);
            throw new RuntimeException("Embedding failed", e);
        }
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }
        int effectiveBatchSize = Math.max(1, batchSize);

        // 按批次切分并提交并发任务
        List<BatchTask> tasks = new ArrayList<>();
        for (int start = 0; start < texts.size(); start += effectiveBatchSize) {
            int end = Math.min(start + effectiveBatchSize, texts.size());
            List<String> batch = texts.subList(start, end);
            int batchIndex = start / effectiveBatchSize;
            Future<List<float[]>> future = embeddingExecutor.submit(() -> embedOneBatch(batch));
            tasks.add(new BatchTask(batchIndex, batch.size(), future));
        }

        // 按批次顺序回填，保证返回下标与输入下标一一对应（并发执行、顺序合并）
        List<List<float[]>> batchResults = new ArrayList<>(tasks.size());
        for (int i = 0; i < tasks.size(); i++) {
            batchResults.add(null);
        }
        for (BatchTask task : tasks) {
            try {
                List<float[]> vectors = task.future().get();
                if (vectors.size() != task.expectedSize()) {
                    // 模型返回条数与请求不一致属于契约性错误：宁可整文档失败重试，
                    // 也不能静默写入零向量污染向量库
                    throw new IllegalStateException(String.format(
                            "Embedding 批次返回条数不匹配: batch=%d, expected=%d, actual=%d",
                            task.batchIndex(), task.expectedSize(), vectors.size()));
                }
                batchResults.set(task.batchIndex(), vectors);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Embedding 批处理被中断", e);
            } catch (ExecutionException e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                throw new RuntimeException("Embedding 批处理失败: batch=" + task.batchIndex(), cause);
            }
        }

        List<float[]> allEmbeddings = new ArrayList<>(texts.size());
        for (List<float[]> batchResult : batchResults) {
            allEmbeddings.addAll(batchResult);
        }
        return allEmbeddings;
    }

    /**
     * 调用模型嵌入单批文本，返回与入参等长、同序的向量列表。
     */
    private List<float[]> embedOneBatch(List<String> batch) {
        Response<List<Embedding>> response = embeddingModel.embedAll(
                batch.stream().map(TextSegment::from).toList()
        );
        List<float[]> vectors = new ArrayList<>(batch.size());
        for (Embedding embedding : response.content()) {
            vectors.add(embedding.vector());
        }
        log.debug("Embedded batch of {} texts", batch.size());
        return vectors;
    }

    /** 批次任务句柄：批次序号（决定回填位置）、期望条数、Future */
    private record BatchTask(int batchIndex, int expectedSize, Future<List<float[]>> future) {
    }
}
