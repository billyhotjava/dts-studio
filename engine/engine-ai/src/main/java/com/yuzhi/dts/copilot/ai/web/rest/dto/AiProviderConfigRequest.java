package com.yuzhi.dts.copilot.ai.web.rest.dto;

import com.yuzhi.dts.copilot.ai.domain.AiProviderConfig;
import jakarta.validation.constraints.NotBlank;

public class AiProviderConfigRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String baseUrl;

    private String apiKey;

    @NotBlank
    private String model;

    private Double temperature;
    private Integer maxTokens;
    private Integer timeoutSeconds;
    private Boolean isDefault;
    private Boolean enabled;
    private Integer priority;
    private String providerType;

    public AiProviderConfig toEntity() {
        AiProviderConfig config = new AiProviderConfig();
        config.setName(name);
        config.setBaseUrl(baseUrl);
        config.setApiKey(apiKey);
        config.setModel(model);
        config.setTemperature(temperature);
        config.setMaxTokens(maxTokens);
        config.setTimeoutSeconds(timeoutSeconds);
        config.setIsDefault(isDefault != null ? isDefault : Boolean.FALSE);
        config.setEnabled(enabled != null ? enabled : Boolean.TRUE);
        config.setPriority(priority != null ? priority : 0);
        config.setProviderType(providerType);
        return config;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }

    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(Integer timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getProviderType() {
        return providerType;
    }

    public void setProviderType(String providerType) {
        this.providerType = providerType;
    }
}
