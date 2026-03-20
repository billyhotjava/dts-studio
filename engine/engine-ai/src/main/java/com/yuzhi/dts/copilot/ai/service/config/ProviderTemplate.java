package com.yuzhi.dts.copilot.ai.service.config;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Built-in provider templates with sensible defaults.
 */
public enum ProviderTemplate {

    // 本地部署
    OLLAMA("Ollama", "http://localhost:11434/v1", null, "qwen2.5-coder:7b", 0.3, 4096, 60, true, "LOCAL", false, 10),
    VLLM("vLLM", "http://localhost:8000/v1", null, "default", 0.3, 4096, 60, true, "LOCAL", false, 20),
    LMSTUDIO("LM Studio", "http://localhost:1234/v1", null, "default", 0.3, 4096, 60, true, "LOCAL", false, 30),

    // 国际主流
    OPENAI("OpenAI", "https://api.openai.com/v1", null, "gpt-4o", 0.3, 4096, 60, false, "INTERNATIONAL", false, 110),
    AZURE_OPENAI("Azure OpenAI", "https://{resource}.openai.azure.com/openai/deployments/{deployment}", null, "gpt-4o", 0.3, 4096, 60, false, "INTERNATIONAL", false, 120),
    ANTHROPIC("Anthropic Claude", "https://api.anthropic.com/v1/", null, "claude-sonnet-4-5", 0.3, 4096, 60, false, "INTERNATIONAL", false, 130),
    GEMINI("Google Gemini", "https://generativelanguage.googleapis.com/v1beta/openai/", null, "gemini-2.5-flash", 0.3, 4096, 60, false, "INTERNATIONAL", false, 140),
    GROQ("Groq", "https://api.groq.com/openai/v1", null, "llama-3.3-70b-versatile", 0.3, 4096, 60, false, "INTERNATIONAL", false, 150),
    MISTRAL("Mistral", "https://api.mistral.ai/v1", null, "mistral-small-latest", 0.3, 4096, 60, false, "INTERNATIONAL", false, 160),

    // 中国主流
    DEEPSEEK("DeepSeek", "https://api.deepseek.com/v1", null, "deepseek-chat", 0.3, 4096, 60, false, "CHINA", true, 210),
    QWEN("通义千问", "https://dashscope.aliyuncs.com/compatible-mode/v1", null, "qwen-plus", 0.3, 4096, 60, false, "CHINA", false, 220),
    ZHIPU("智谱 GLM", "https://open.bigmodel.cn/api/paas/v4", null, "glm-4-flash", 0.3, 4096, 60, false, "CHINA", false, 230),
    MOONSHOT("Moonshot Kimi", "https://api.moonshot.cn/v1", null, "moonshot-v1-8k", 0.3, 4096, 60, false, "CHINA", false, 240),
    BAIDU_QIANFAN("百度千帆", "https://qianfan.baidubce.com/v2", null, "ernie-4.5-8k-preview", 0.3, 4096, 60, false, "CHINA", false, 250),
    DOUBAO("火山方舟 / 豆包", "https://ark.cn-beijing.volces.com/api/v3", null, "doubao-seed-1-6-flash-250828", 0.3, 4096, 60, false, "CHINA", false, 260),
    SILICONFLOW("硅基流动", "https://api.siliconflow.cn/v1", null, "deepseek-ai/DeepSeek-V3", 0.3, 4096, 60, false, "CHINA", false, 270),
    MINIMAX("MiniMax", "https://api.minimaxi.com/anthropic", null, "MiniMax-M2.7", 0.3, 4096, 60, false, "CHINA", false, 280);

    private final String displayName;
    private final String defaultBaseUrl;
    private final String defaultApiKey;
    private final String defaultModel;
    private final double defaultTemperature;
    private final int defaultMaxTokens;
    private final int defaultTimeoutSeconds;
    private final boolean local;
    private final String region;
    private final boolean recommended;
    private final int sortOrder;

    ProviderTemplate(String displayName, String defaultBaseUrl, String defaultApiKey,
                     String defaultModel, double defaultTemperature,
                     int defaultMaxTokens, int defaultTimeoutSeconds, boolean local,
                     String region, boolean recommended, int sortOrder) {
        this.displayName = displayName;
        this.defaultBaseUrl = defaultBaseUrl;
        this.defaultApiKey = defaultApiKey;
        this.defaultModel = defaultModel;
        this.defaultTemperature = defaultTemperature;
        this.defaultMaxTokens = defaultMaxTokens;
        this.defaultTimeoutSeconds = defaultTimeoutSeconds;
        this.local = local;
        this.region = region;
        this.recommended = recommended;
        this.sortOrder = sortOrder;
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

    /**
     * 是否为本地部署的 Provider（不需要 API Key）。
     */
    public boolean isLocal() {
        return local;
    }

    public String getRegion() {
        return region;
    }

    public boolean isRecommended() {
        return recommended;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public boolean requiresApiKey() {
        return !local;
    }

    public static List<ProviderTemplate> orderedValues() {
        return Arrays.stream(values())
                .sorted(Comparator.comparingInt(ProviderTemplate::getSortOrder))
                .toList();
    }

    /**
     * 根据 baseUrl 判断是否为本地 Provider。
     * 本地地址包括: localhost, 127.0.0.1, 以及已知的本地 Provider 类型。
     */
    public static boolean isLocalUrl(String baseUrl) {
        if (baseUrl == null) return false;
        String lower = baseUrl.toLowerCase();
        return lower.contains("localhost") || lower.contains("127.0.0.1")
                || lower.contains("0.0.0.0") || lower.contains("host.docker.internal");
    }

    /**
     * 判断 Provider 配置是否有效（本地 Provider 不需要 apiKey）。
     */
    public static boolean isConfigValid(String providerType, String baseUrl, String apiKey) {
        if (baseUrl == null || baseUrl.isBlank()) return false;
        // 本地 Provider 不需要 API Key
        if (isLocalUrl(baseUrl)) return true;
        if ("ollama".equalsIgnoreCase(providerType) || "vllm".equalsIgnoreCase(providerType)
                || "lmstudio".equalsIgnoreCase(providerType) || "local".equalsIgnoreCase(providerType)) {
            return true;
        }
        // 公有云 Provider 需要 API Key
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * 对 API Key 脱敏，仅显示前 4 位和后 4 位。
     */
    public static String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() <= 8) return "******";
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }
}
