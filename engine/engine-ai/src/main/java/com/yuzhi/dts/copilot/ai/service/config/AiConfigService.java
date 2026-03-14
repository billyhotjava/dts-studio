package com.yuzhi.dts.copilot.ai.service.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuzhi.dts.copilot.ai.domain.AiProviderConfig;
import com.yuzhi.dts.copilot.ai.repository.AiProviderConfigRepository;
import com.yuzhi.dts.copilot.ai.service.llm.OpenAiCompatibleClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Manages AI provider configurations (CRUD + defaults).
 */
@Service
@Transactional
public class AiConfigService {

    private static final Logger log = LoggerFactory.getLogger(AiConfigService.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final AiProviderConfigRepository repository;

    @Value("${dts.copilot.ai.config-path:/opt/dts/upload/ai-copilot-config.json}")
    private String configFilePath;

    @Value("${dts.copilot.llm.provider:deepseek}")
    private String envLlmProvider;

    @Value("${dts.copilot.llm.base-url:https://api.deepseek.com/v1}")
    private String envLlmBaseUrl;

    @Value("${dts.copilot.llm.model:deepseek-chat}")
    private String envLlmModel;

    @Value("${dts.copilot.llm.api-key:}")
    private String envLlmApiKey;

    @Value("${dts.copilot.llm.max-tokens:4096}")
    private int envLlmMaxTokens;

    @Value("${dts.copilot.llm.temperature:0.3}")
    private double envLlmTemperature;

    @Value("${dts.copilot.llm.timeout-seconds:60}")
    private int envLlmTimeout;

    public AiConfigService(AiProviderConfigRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<AiProviderConfig> getAllProviders() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public List<AiProviderConfig> getEnabledProviders() {
        return repository.findByEnabledTrueOrderByPriorityAsc();
    }

    @Transactional(readOnly = true)
    public Optional<AiProviderConfig> getProvider(Long id) {
        return repository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<AiProviderConfig> getDefaultProvider() {
        return repository.findByIsDefaultTrue();
    }

    public AiProviderConfig createProvider(AiProviderConfig config) {
        if (Boolean.TRUE.equals(config.getIsDefault())) {
            clearDefaultFlag();
        }
        return repository.save(config);
    }

    public AiProviderConfig updateProvider(Long id, AiProviderConfig updated) {
        AiProviderConfig existing = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Provider not found: " + id));

        existing.setName(updated.getName());
        existing.setBaseUrl(updated.getBaseUrl());
        existing.setApiKey(updated.getApiKey());
        existing.setModel(updated.getModel());
        existing.setTemperature(updated.getTemperature());
        existing.setMaxTokens(updated.getMaxTokens());
        existing.setTimeoutSeconds(updated.getTimeoutSeconds());
        existing.setEnabled(updated.getEnabled());
        existing.setPriority(updated.getPriority());
        existing.setProviderType(updated.getProviderType());

        if (Boolean.TRUE.equals(updated.getIsDefault())) {
            clearDefaultFlag();
        }
        existing.setIsDefault(updated.getIsDefault());

        return repository.save(existing);
    }

    public void deleteProvider(Long id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("Provider not found: " + id);
        }
        repository.deleteById(id);
    }

    /**
     * Create a provider config from a built-in template.
     */
    public AiProviderConfig createFromTemplate(ProviderTemplate template, String apiKey) {
        AiProviderConfig config = new AiProviderConfig();
        config.setName(template.getDisplayName());
        config.setBaseUrl(template.getDefaultBaseUrl());
        config.setApiKey(apiKey);
        config.setModel(template.getDefaultModel());
        config.setTemperature(template.getDefaultTemperature());
        config.setMaxTokens(template.getDefaultMaxTokens());
        config.setTimeoutSeconds(template.getDefaultTimeoutSeconds());
        config.setProviderType(template.name());
        config.setEnabled(true);
        config.setIsDefault(false);
        return repository.save(config);
    }

    /**
     * 获取有效的默认 Provider 配置，按降级链查找：
     * 1. 数据库中标记为 default 的配置
     * 2. 数据库中第一个 enabled 的配置
     * 3. 环境变量 LLM_* 配置（默认公有云）
     * 4. 本地 JSON 配置文件
     * 5. 内置 Ollama 兜底
     */
    @Transactional(readOnly = true)
    public AiProviderConfig resolveEffectiveProvider() {
        // 1. 数据库 default
        Optional<AiProviderConfig> defaultProvider = getDefaultProvider();
        if (defaultProvider.isPresent() && isProviderUsable(defaultProvider.get())) {
            return defaultProvider.get();
        }

        // 2. 数据库中第一个 enabled
        List<AiProviderConfig> enabled = getEnabledProviders();
        for (AiProviderConfig p : enabled) {
            if (isProviderUsable(p)) return p;
        }

        // 3. 环境变量 LLM_* 配置
        AiProviderConfig envConfig = loadFromEnvConfig();
        if (envConfig != null) {
            log.info("使用环境变量 LLM 配置: provider={}, model={}", envLlmProvider, envLlmModel);
            return envConfig;
        }

        // 4. 本地 JSON 文件
        AiProviderConfig fromFile = loadFromConfigFile();
        if (fromFile != null) {
            log.info("使用本地配置文件: {}", configFilePath);
            return fromFile;
        }

        // 5. 内置 Ollama 兜底
        log.warn("未找到任何 AI Provider 配置，使用内置 Ollama 默认配置");
        AiProviderConfig fallback = new AiProviderConfig();
        fallback.setName("Ollama (默认)");
        fallback.setProviderType("OLLAMA");
        fallback.setBaseUrl(ProviderTemplate.OLLAMA.getDefaultBaseUrl());
        fallback.setModel(ProviderTemplate.OLLAMA.getDefaultModel());
        fallback.setTemperature(ProviderTemplate.OLLAMA.getDefaultTemperature());
        fallback.setMaxTokens(ProviderTemplate.OLLAMA.getDefaultMaxTokens());
        fallback.setTimeoutSeconds(ProviderTemplate.OLLAMA.getDefaultTimeoutSeconds());
        fallback.setEnabled(true);
        fallback.setIsDefault(true);
        return fallback;
    }

    /**
     * 从环境变量 LLM_* 加载 Provider 配置。
     * 当 LLM_BASE_URL 和 LLM_MODEL 有效时返回配置，否则返回 null。
     */
    private AiProviderConfig loadFromEnvConfig() {
        if (envLlmBaseUrl == null || envLlmBaseUrl.isBlank()) return null;
        if (envLlmModel == null || envLlmModel.isBlank()) return null;
        // 公有云 Provider 必须有 API Key
        if (!ProviderTemplate.isLocalUrl(envLlmBaseUrl)
                && (envLlmApiKey == null || envLlmApiKey.isBlank())) {
            log.debug("环境变量 LLM 配置缺少 API Key，跳过（provider={}）", envLlmProvider);
            return null;
        }

        AiProviderConfig config = new AiProviderConfig();
        config.setName(envLlmProvider + " (环境变量)");
        config.setProviderType(envLlmProvider.toUpperCase());
        config.setBaseUrl(envLlmBaseUrl);
        config.setModel(envLlmModel);
        config.setApiKey(envLlmApiKey);
        config.setMaxTokens(envLlmMaxTokens);
        config.setTemperature(envLlmTemperature);
        config.setTimeoutSeconds(envLlmTimeout);
        config.setEnabled(true);
        config.setIsDefault(true);
        return config;
    }

    /**
     * 测试 Provider 连通性。
     */
    public Map<String, Object> testProvider(Long id) {
        AiProviderConfig config = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Provider not found: " + id));
        return testProviderConfig(config);
    }

    /**
     * 测试指定配置的连通性（不需要持久化）。
     */
    public Map<String, Object> testProviderConfig(AiProviderConfig config) {
        try {
            OpenAiCompatibleClient client = new OpenAiCompatibleClient(
                    config.getBaseUrl(), config.getApiKey(),
                    config.getTimeoutSeconds() != null ? config.getTimeoutSeconds() : 30);
            JsonNode models = client.listModels();

            boolean modelFound = false;
            if (models.has("data") && models.get("data").isArray()) {
                for (JsonNode m : models.get("data")) {
                    if (m.has("id") && m.get("id").asText().contains(config.getModel())) {
                        modelFound = true;
                        break;
                    }
                }
            }

            return Map.of(
                    "success", true,
                    "provider", config.getName(),
                    "baseUrl", config.getBaseUrl(),
                    "model", config.getModel(),
                    "modelAvailable", modelFound,
                    "modelsCount", models.has("data") ? models.get("data").size() : 0
            );
        } catch (Exception e) {
            log.warn("Provider 连通性测试失败: {} - {}", config.getName(), e.getMessage());
            return Map.of(
                    "success", false,
                    "provider", config.getName(),
                    "baseUrl", config.getBaseUrl(),
                    "error", e.getMessage()
            );
        }
    }

    /**
     * 判断 Provider 配置是否可用。
     */
    private boolean isProviderUsable(AiProviderConfig config) {
        if (!Boolean.TRUE.equals(config.getEnabled())) return false;
        return ProviderTemplate.isConfigValid(
                config.getProviderType(), config.getBaseUrl(), config.getApiKey());
    }

    /**
     * 从本地 JSON 配置文件加载 Provider 配置。
     * 文件格式: {"provider":"ollama","baseUrl":"...","model":"...","apiKey":"","maxTokens":4096,"temperature":0.3,"timeout":60}
     */
    private AiProviderConfig loadFromConfigFile() {
        try {
            Path path = Path.of(configFilePath);
            if (!Files.exists(path)) return null;

            JsonNode json = mapper.readTree(Files.readString(path));
            AiProviderConfig config = new AiProviderConfig();
            config.setName(json.has("provider") ? json.get("provider").asText() : "local-file");
            config.setProviderType(json.has("provider") ? json.get("provider").asText().toUpperCase() : "OLLAMA");
            config.setBaseUrl(json.has("baseUrl") ? json.get("baseUrl").asText() : null);
            config.setModel(json.has("model") ? json.get("model").asText() : null);
            config.setApiKey(json.has("apiKey") ? json.get("apiKey").asText() : null);
            config.setMaxTokens(json.has("maxTokens") ? json.get("maxTokens").asInt(4096) : 4096);
            config.setTemperature(json.has("temperature") ? json.get("temperature").asDouble(0.3) : 0.3);
            config.setTimeoutSeconds(json.has("timeout") ? json.get("timeout").asInt(60) : 60);
            config.setEnabled(true);
            config.setIsDefault(true);

            if (config.getBaseUrl() == null || config.getBaseUrl().isBlank()) return null;
            return config;
        } catch (Exception e) {
            log.warn("读取本地 AI 配置文件失败: {} - {}", configFilePath, e.getMessage());
            return null;
        }
    }

    private void clearDefaultFlag() {
        repository.findByIsDefaultTrue().ifPresent(existing -> {
            existing.setIsDefault(false);
            repository.save(existing);
        });
    }
}
