package com.yuzhi.dts.copilot.ai.web.rest;

import com.yuzhi.dts.copilot.ai.domain.AiProviderConfig;
import com.yuzhi.dts.copilot.ai.service.config.AiConfigService;
import com.yuzhi.dts.copilot.ai.service.config.ProviderTemplate;
import com.yuzhi.dts.copilot.ai.service.llm.OpenAiCompatibleClient;
import com.yuzhi.dts.copilot.ai.web.rest.dto.ApiResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for AI provider configuration management.
 */
@RestController
@RequestMapping("/api/ai/config")
public class AiConfigResource {

    private static final Logger log = LoggerFactory.getLogger(AiConfigResource.class);

    private final AiConfigService configService;

    public AiConfigResource(AiConfigService configService) {
        this.configService = configService;
    }

    @GetMapping("/providers")
    public ResponseEntity<ApiResponse<List<AiProviderConfig>>> listProviders() {
        List<AiProviderConfig> providers = configService.getAllProviders();
        return ResponseEntity.ok(ApiResponse.ok(providers));
    }

    @PostMapping("/providers")
    public ResponseEntity<ApiResponse<AiProviderConfig>> createProvider(
            @Valid @RequestBody AiProviderConfig config) {
        try {
            AiProviderConfig created = configService.createProvider(config);
            return ResponseEntity.ok(ApiResponse.ok(created));
        } catch (Exception e) {
            log.error("Failed to create provider", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to create provider: " + e.getMessage()));
        }
    }

    @PutMapping("/providers/{id}")
    public ResponseEntity<ApiResponse<AiProviderConfig>> updateProvider(
            @PathVariable Long id,
            @Valid @RequestBody AiProviderConfig config) {
        try {
            AiProviderConfig updated = configService.updateProvider(id, config);
            return ResponseEntity.ok(ApiResponse.ok(updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to update provider", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to update provider: " + e.getMessage()));
        }
    }

    @DeleteMapping("/providers/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProvider(@PathVariable Long id) {
        try {
            configService.deleteProvider(id);
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/providers/templates")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listTemplates() {
        List<Map<String, Object>> templates = Arrays.stream(ProviderTemplate.values())
                .map(t -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("name", t.name());
                    map.put("displayName", t.getDisplayName());
                    map.put("defaultBaseUrl", t.getDefaultBaseUrl());
                    map.put("defaultModel", t.getDefaultModel());
                    map.put("defaultTemperature", t.getDefaultTemperature());
                    map.put("defaultMaxTokens", t.getDefaultMaxTokens());
                    map.put("defaultTimeoutSeconds", t.getDefaultTimeoutSeconds());
                    return map;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(templates));
    }

    @PostMapping("/providers/{id}/test")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testProvider(@PathVariable Long id) {
        return configService.getProvider(id)
                .map(config -> {
                    OpenAiCompatibleClient client = new OpenAiCompatibleClient(
                            config.getBaseUrl(),
                            config.getApiKey(),
                            config.getTimeoutSeconds() != null ? config.getTimeoutSeconds() : 10
                    );
                    boolean available = client.isAvailable();
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("reachable", available);
                    result.put("provider", config.getName());
                    result.put("baseUrl", config.getBaseUrl());
                    if (available) {
                        try {
                            result.put("models", client.listModels());
                        } catch (Exception e) {
                            result.put("modelsError", e.getMessage());
                        }
                    }
                    return ResponseEntity.ok(ApiResponse.ok(result));
                })
                .orElseGet(() -> ResponseEntity.badRequest()
                        .body(ApiResponse.error("Provider not found: " + id)));
    }
}
