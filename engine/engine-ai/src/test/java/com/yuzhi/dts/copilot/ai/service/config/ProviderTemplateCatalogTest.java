package com.yuzhi.dts.copilot.ai.service.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

class ProviderTemplateCatalogTest {

    @Test
    void catalogIncludesMainstreamInternationalChinaAndLocalProviders() {
        assertDoesNotThrow(() -> ProviderTemplate.valueOf("OPENAI"));
        assertDoesNotThrow(() -> ProviderTemplate.valueOf("ANTHROPIC"));
        assertDoesNotThrow(() -> ProviderTemplate.valueOf("GEMINI"));
        assertDoesNotThrow(() -> ProviderTemplate.valueOf("GROQ"));
        assertDoesNotThrow(() -> ProviderTemplate.valueOf("DEEPSEEK"));
        assertDoesNotThrow(() -> ProviderTemplate.valueOf("QWEN"));
        assertDoesNotThrow(() -> ProviderTemplate.valueOf("ZHIPU"));
        assertDoesNotThrow(() -> ProviderTemplate.valueOf("MOONSHOT"));
        assertDoesNotThrow(() -> ProviderTemplate.valueOf("BAIDU_QIANFAN"));
        assertDoesNotThrow(() -> ProviderTemplate.valueOf("DOUBAO"));
        assertDoesNotThrow(() -> ProviderTemplate.valueOf("SILICONFLOW"));
        assertDoesNotThrow(() -> ProviderTemplate.valueOf("OLLAMA"));
    }
}
