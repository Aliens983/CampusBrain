package com.kb.infrastructure.rag.llm;

import com.kb.domain.rag.LlmService.ChatMessage;
import com.kb.domain.rag.RetrievalResult;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Builds the full prompt for RAG-based answer generation.
 * <p>
 * Composes System Prompt + Retrieved Context + Conversation History + User Query
 * into a structured prompt that guides the LLM to answer accurately with citations.
 * </p>
 *
 * @author forever-king
 */
@Component
public class PromptTemplateEngine {

    /**
     * System prompt that defines the assistant's behavior.
     */
    public String buildSystemPrompt() {
        return """
你是一个企业智能知识库助手，专门根据企业内部文档回答员工的问题。

## 核心规则
1. 只根据下方【参考文档内容】提供的信息回答问题，绝对不要编造或臆测
2. 如果文档内容不足以回答问题，请明确回答："根据已有文档，我暂时无法回答这个问题。建议您补充相关文档或联系对应知识管理员。"
3. 回答要准确、简洁、有条理，使用中文
4. 引用来源时标注格式：[文档名]

## 回答格式
- 先用 1-2 句话给出直接答案
- 如有必要，再展开详细说明
- 最后列出参考的文档来源

## 注意事项
- 如果用户问的是事实性问题（日期、数字、政策条款），务必精确引用原文
- 如果用户问的是操作性问题（如何做、流程），给出步骤清晰的指引
- 如果用户问的是比较性问题，逐一对比并引用不同文档的依据
""";
    }

    /**
     * Build the context prompt from retrieved document chunks.
     */
    public String buildContextPrompt(List<RetrievalResult> retrievedDocs) {
        if (retrievedDocs == null || retrievedDocs.isEmpty()) {
            return "（未找到相关文档内容）";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("## 参考文档内容\n\n");
        for (int i = 0; i < retrievedDocs.size(); i++) {
            RetrievalResult doc = retrievedDocs.get(i);
            sb.append(String.format("--- [文档片段 %d] 来源: %s",
                    i + 1, doc.getDocumentTitle()));
            if (doc.getSectionTitle() != null && !doc.getSectionTitle().isEmpty()) {
                sb.append(" > ").append(doc.getSectionTitle());
            }
            sb.append(" ---\n");
            sb.append(doc.getContent()).append("\n\n");
        }
        return sb.toString();
    }

    /**
     * Build the full message list for the LLM call.
     */
    public List<ChatMessage> buildFullPrompt(String query,
                                              List<RetrievalResult> retrievedDocs,
                                              List<ChatMessage> conversationHistory) {
        String systemPrompt = buildSystemPrompt();
        String contextPrompt = buildContextPrompt(retrievedDocs);

        return List.of(
                ChatMessage.system(systemPrompt),
                ChatMessage.user(contextPrompt),
                // Include recent history for multi-turn context
                ChatMessage.user("用户的当前问题是：" + query)
        );
    }
}
