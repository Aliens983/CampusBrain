package com.kb.infrastructure.rag.llm;

/**
 * LLM 提供商枚举，支持多模型运行时切换。
 * <p>
 * 通过配置项 {@code llm.provider} 指定，默认 {@code deepseek}。
 * </p>
 * @author forever-king
 */
public enum ModelProvider {
    /** DeepSeek（默认） — OpenAI 兼容接口 */
    DEEPSEEK("DeepSeek", "https://api.deepseek.com/v1"),
    /** 通义千问 — DashScope API */
    QWEN("通义千问", "https://dashscope.aliyuncs.com/compatible-mode/v1"),
    /** OpenAI */
    OPENAI("OpenAI", "https://api.openai.com/v1"),
    /** 本地 Ollama */
    OLLAMA("Ollama", "http://localhost:11434/v1"),
    /** 硅基流动（SiliconFlow） */
    SILICONFLOW("硅基流动", "https://api.siliconflow.cn/v1");

    /** 显示名称 */
    private final String displayName;
    /** API Base URL */
    private final String baseUrl;

    ModelProvider(String displayName, String baseUrl) {
        this.displayName = displayName;
        this.baseUrl = baseUrl;
    }

    public String getDisplayName() { return displayName; }
    public String getBaseUrl() { return baseUrl; }

    /**
     * 根据名称解析 Provider，不区分大小写。
     */
    public static ModelProvider fromName(String name) {
        for (ModelProvider p : values()) {
            if (p.name().equalsIgnoreCase(name)) return p;
        }
        return DEEPSEEK;
    }
}
