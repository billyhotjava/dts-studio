package com.yuzhi.dts.copilot.ai.web.rest.dto;

import com.yuzhi.dts.copilot.ai.domain.AiProviderConfig;

import java.time.Instant;

public record AiProviderConfigResponse(
        Long id,
        String name,
        String baseUrl,
        String model,
        Double temperature,
        Integer maxTokens,
        Integer timeoutSeconds,
        Boolean isDefault,
        Boolean enabled,
        Integer priority,
        String providerType,
        Instant createdAt,
        Instant updatedAt,
        boolean hasApiKey,
        String apiKeyMasked
) {

    public static AiProviderConfigResponse from(AiProviderConfig config) {
        String apiKey = config.getApiKey();
        return new AiProviderConfigResponse(
                config.getId(),
                config.getName(),
                config.getBaseUrl(),
                config.getModel(),
                config.getTemperature(),
                config.getMaxTokens(),
                config.getTimeoutSeconds(),
                config.getIsDefault(),
                config.getEnabled(),
                config.getPriority(),
                config.getProviderType(),
                config.getCreatedAt(),
                config.getUpdatedAt(),
                apiKey != null && !apiKey.isBlank(),
                mask(apiKey)
        );
    }

    private static String mask(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }

        int length = apiKey.length();
        if (length <= 4) {
            return "*".repeat(length);
        }

        int suffixLength = Math.min(4, length - 1);
        int prefixLength = Math.min(3, Math.max(1, length - suffixLength));
        int maskedLength = Math.max(4, length - prefixLength - suffixLength);
        return apiKey.substring(0, prefixLength)
                + "*".repeat(maskedLength)
                + apiKey.substring(length - suffixLength);
    }
}
