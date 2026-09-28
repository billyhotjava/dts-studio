package com.yuzhi.dts.copilot.ai.web.rest;

import com.yuzhi.dts.copilot.ai.domain.AiProviderConfig;
import com.yuzhi.dts.copilot.ai.service.config.AiConfigService;
import com.yuzhi.dts.copilot.ai.service.config.ProviderTemplate;
import com.yuzhi.dts.copilot.ai.web.rest.dto.ApiResponse;
import com.yuzhi.dts.copilot.ai.web.rest.dto.AiProviderConfigRequest;
import com.yuzhi.dts.copilot.ai.web.rest.dto.AiProviderConfigResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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
    private final String adminSecret;

    public AiConfigResource(AiConfigService configService,
                            @Value("${copilot.admin-secret:}") String adminSecret) {
        this.configService = configService;
        this.adminSecret = adminSecret;
    }

    @GetMapping("/providers")
    public ResponseEntity<ApiResponse<List<AiProviderConfigResponse>>> listProviders(
            @RequestHeader("X-Admin-Secret") String secret) {
        ResponseEntity<ApiResponse<List<AiProviderConfigResponse>>> authCheck = checkAdminSecret(secret);
        if (authCheck != null) {
            return authCheck;
        }
        List<AiProviderConfigResponse> providers = configService.getAllProviders()
                .stream()
                .map(AiProviderConfigResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(providers));
    }

    @PostMapping("/providers")
    public ResponseEntity<ApiResponse<AiProviderConfigResponse>> createProvider(
            @RequestHeader("X-Admin-Secret") String secret,
            @Valid @RequestBody AiProviderConfigRequest request) {
        ResponseEntity<ApiResponse<AiProviderConfigResponse>> authCheck = checkAdminSecret(secret);
        if (authCheck != null) {
            return authCheck;
        }
        try {
            AiProviderConfig created = configService.createProvider(request.toEntity());
            return ResponseEntity.ok(ApiResponse.ok(AiProviderConfigResponse.from(created)));
        } catch (Exception e) {
            log.error("Failed to create provider", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to create provider: " + e.getMessage()));
        }
    }

    @PutMapping("/providers/{id}")
    public ResponseEntity<ApiResponse<AiProviderConfigResponse>> updateProvider(
            @RequestHeader("X-Admin-Secret") String secret,
            @PathVariable Long id,
            @Valid @RequestBody AiProviderConfigRequest request) {
        ResponseEntity<ApiResponse<AiProviderConfigResponse>> authCheck = checkAdminSecret(secret);
        if (authCheck != null) {
            return authCheck;
        }
        try {
            AiProviderConfig updated = configService.updateProvider(id, request.toEntity());
            return ResponseEntity.ok(ApiResponse.ok(AiProviderConfigResponse.from(updated)));
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
    public ResponseEntity<ApiResponse<Void>> deleteProvider(
            @RequestHeader("X-Admin-Secret") String secret,
            @PathVariable Long id) {
        ResponseEntity<ApiResponse<Void>> authCheck = checkAdminSecret(secret);
        if (authCheck != null) {
            return authCheck;
        }
        try {
            configService.deleteProvider(id);
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/providers/templates")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listTemplates(
            @RequestHeader("X-Admin-Secret") String secret) {
        ResponseEntity<ApiResponse<List<Map<String, Object>>>> authCheck = checkAdminSecret(secret);
        if (authCheck != null) {
            return authCheck;
        }
        List<Map<String, Object>> templates = ProviderTemplate.orderedValues().stream()
                .map(t -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("name", t.name());
                    map.put("displayName", t.getDisplayName());
                    map.put("defaultBaseUrl", t.getDefaultBaseUrl());
                    map.put("defaultModel", t.getDefaultModel());
                    map.put("defaultTemperature", t.getDefaultTemperature());
                    map.put("defaultMaxTokens", t.getDefaultMaxTokens());
                    map.put("defaultTimeoutSeconds", t.getDefaultTimeoutSeconds());
                    map.put("region", t.getRegion());
                    map.put("recommended", t.isRecommended());
                    map.put("sortOrder", t.getSortOrder());
                    map.put("requiresApiKey", t.requiresApiKey());
                    return map;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(templates));
    }

    @PostMapping("/providers/{id}/test")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testProvider(
            @RequestHeader("X-Admin-Secret") String secret,
            @PathVariable Long id) {
        ResponseEntity<ApiResponse<Map<String, Object>>> authCheck = checkAdminSecret(secret);
        if (authCheck != null) {
            return authCheck;
        }
        return configService.getProvider(id)
                .map(config -> ResponseEntity.ok(ApiResponse.ok(configService.testProvider(id))))
                .orElseGet(() -> ResponseEntity.badRequest()
                        .body(ApiResponse.error("Provider not found: " + id)));
    }

    private <T> ResponseEntity<ApiResponse<T>> checkAdminSecret(String secret) {
        if (adminSecret == null || adminSecret.isBlank()) {
            log.error("COPILOT_ADMIN_SECRET is not configured");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Admin secret not configured"));
        }
        if (!adminSecret.equals(secret)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Invalid admin secret"));
        }
        return null;
    }
}
