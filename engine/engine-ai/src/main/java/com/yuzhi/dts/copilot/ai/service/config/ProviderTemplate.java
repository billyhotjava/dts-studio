package com.yuzhi.dts.copilot.ai.service.config;

/**
 * Built-in provider templates with sensible defaults.
 */
public enum ProviderTemplate {

    OLLAMA("Ollama", "http://localhost:11434", null, "llama3.1", 0.7, 4096, 120),
    OPENAI("OpenAI", "https://api.openai.com", null, "gpt-4o", 0.7, 4096, 120),
    DEEPSEEK("DeepSeek", "https://api.deepseek.com", null, "deepseek-chat", 0.7, 4096, 120),
    QWEN("Qwen", "https://dashscope.aliyuncs.com/compatible-mode", null, "qwen-plus", 0.7, 4096, 120),
    ZHIPU("ZhiPu", "https://open.bigmodel.cn/api/paas", null, "glm-4", 0.7, 4096, 120);

    private final String displayName;
    private final String defaultBaseUrl;
    private final String defaultApiKey;
    private final String defaultModel;
    private final double defaultTemperature;
    private final int defaultMaxTokens;
    private final int defaultTimeoutSeconds;

    ProviderTemplate(String displayName, String defaultBaseUrl, String defaultApiKey,
                     String defaultModel, double defaultTemperature,
                     int defaultMaxTokens, int defaultTimeoutSeconds) {
        this.displayName = displayName;
        this.defaultBaseUrl = defaultBaseUrl;
        this.defaultApiKey = defaultApiKey;
        this.defaultModel = defaultModel;
        this.defaultTemperature = defaultTemperature;
        this.defaultMaxTokens = defaultMaxTokens;
        this.defaultTimeoutSeconds = defaultTimeoutSeconds;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDefaultBaseUrl() {
        return defaultBaseUrl;
    }

    public String getDefaultApiKey() {
        return defaultApiKey;
    }

    public String getDefaultModel() {
        return defaultModel;
    }

    public double getDefaultTemperature() {
        return defaultTemperature;
    }

    public int getDefaultMaxTokens() {
        return defaultMaxTokens;
    }

    public int getDefaultTimeoutSeconds() {
        return defaultTimeoutSeconds;
    }
}
