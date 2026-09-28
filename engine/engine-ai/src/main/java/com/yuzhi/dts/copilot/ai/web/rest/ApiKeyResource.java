package com.yuzhi.dts.copilot.ai.web.rest;

import com.yuzhi.dts.copilot.ai.domain.ApiKey;
import com.yuzhi.dts.copilot.ai.service.auth.ApiKeyService;
import com.yuzhi.dts.copilot.ai.service.auth.ApiKeyService.ApiKeyCreateResult;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for API key management.
 * All endpoints require the X-Admin-Secret header matching the COPILOT_ADMIN_SECRET env variable.
 */
@RestController
@RequestMapping("/api/auth/keys")
public class ApiKeyResource {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyResource.class);

    private final ApiKeyService apiKeyService;
    private final String adminSecret;

    public ApiKeyResource(ApiKeyService apiKeyService,
                          @Value("${copilot.admin-secret:}") String adminSecret) {
        this.apiKeyService = apiKeyService;
        this.adminSecret = adminSecret;
    }

    /**
     * Generate a new API key.
     */
    @PostMapping
    public ResponseEntity<?> generateKey(@RequestHeader("X-Admin-Secret") String secret,
                                         @RequestBody CreateKeyRequest request) {
        ResponseEntity<?> authCheck = checkAdminSecret(secret);
        if (authCheck != null) {
            return authCheck;
        }

        ApiKeyCreateResult result = apiKeyService.generateKey(
            request.name(), request.description(), request.createdBy(), request.expiresInDays()
        );

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", result.id());
        body.put("rawKey", result.rawKey());
        body.put("prefix", result.prefix());
        body.put("message", "Store this key securely. It will not be shown again.");

        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    /**
     * List all active API keys (with masked key info).
     */
    @GetMapping
    public ResponseEntity<?> listKeys(@RequestHeader("X-Admin-Secret") String secret) {
        ResponseEntity<?> authCheck = checkAdminSecret(secret);
        if (authCheck != null) {
            return authCheck;
        }

        List<Map<String, Object>> keys = apiKeyService.listKeys().stream()
            .map(this::toMaskedResponse)
            .toList();

        return ResponseEntity.ok(keys);
    }

    /**
     * Get details of a specific API key.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getKey(@RequestHeader("X-Admin-Secret") String secret,
                                    @PathVariable Long id) {
        ResponseEntity<?> authCheck = checkAdminSecret(secret);
        if (authCheck != null) {
            return authCheck;
        }

        return apiKeyService.listKeys().stream()
            .filter(k -> k.getId().equals(id))
            .findFirst()
            .map(k -> ResponseEntity.ok(toMaskedResponse(k)))
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Revoke an API key.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> revokeKey(@RequestHeader("X-Admin-Secret") String secret,
                                       @PathVariable Long id) {
        ResponseEntity<?> authCheck = checkAdminSecret(secret);
        if (authCheck != null) {
            return authCheck;
        }

        apiKeyService.revokeKey(id);
        return ResponseEntity.ok(Map.of("message", "API key revoked", "id", id));
    }

    /**
     * Rotate an API key (revoke old, generate new).
     */
    @PutMapping("/{id}/rotate")
    public ResponseEntity<?> rotateKey(@RequestHeader("X-Admin-Secret") String secret,
                                       @PathVariable Long id) {
        ResponseEntity<?> authCheck = checkAdminSecret(secret);
        if (authCheck != null) {
            return authCheck;
        }

        ApiKeyCreateResult result = apiKeyService.rotateKey(id);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", result.id());
        body.put("rawKey", result.rawKey());
        body.put("prefix", result.prefix());
        body.put("message", "Old key revoked. Store this new key securely.");

        return ResponseEntity.ok(body);
    }

    private ResponseEntity<?> checkAdminSecret(String secret) {
        if (adminSecret == null || adminSecret.isBlank()) {
            log.error("COPILOT_ADMIN_SECRET is not configured");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Admin secret not configured"));
        }
        if (!adminSecret.equals(secret)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "Invalid admin secret"));
        }
        return null;
    }

    private Map<String, Object> toMaskedResponse(ApiKey apiKey) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", apiKey.getId());
        map.put("prefix", apiKey.getKeyPrefix());
        map.put("name", apiKey.getName());
        map.put("description", apiKey.getDescription());
        map.put("status", apiKey.getStatus());
        map.put("rateLimit", apiKey.getRateLimit());
        map.put("createdBy", apiKey.getCreatedBy());
        map.put("createdAt", apiKey.getCreatedAt());
        map.put("expiresAt", apiKey.getExpiresAt());
        map.put("lastUsedAt", apiKey.getLastUsedAt());
        map.put("usageCount", apiKey.getUsageCount());
        return map;
    }

    public record CreateKeyRequest(
        String name,
        String description,
        String createdBy,
        Integer expiresInDays
    ) {}
}
