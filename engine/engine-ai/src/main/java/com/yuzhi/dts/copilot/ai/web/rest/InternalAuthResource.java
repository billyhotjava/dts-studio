package com.yuzhi.dts.copilot.ai.web.rest;

import com.yuzhi.dts.copilot.ai.domain.ApiKey;
import com.yuzhi.dts.copilot.ai.service.auth.ApiKeyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Internal endpoint for cross-service API key verification.
 * Used by copilot-analytics to verify API keys against copilot-ai.
 */
@RestController
@RequestMapping("/internal/auth")
public class InternalAuthResource {

    private final ApiKeyService apiKeyService;

    public InternalAuthResource(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    /**
     * Verify an API key and return the associated user context.
     */
    @PostMapping("/verify")
    public ResponseEntity<?> verifyApiKey(@RequestBody VerifyRequest request) {
        Optional<ApiKey> optKey = apiKeyService.validateKey(request.apiKey());

        if (optKey.isEmpty()) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("valid", false);
            body.put("error", "Invalid, expired, or revoked API key");
            return ResponseEntity.ok(body);
        }

        ApiKey apiKey = optKey.get();

        Map<String, Object> user = new LinkedHashMap<>();
        user.put("userId", apiKey.getName());
        user.put("userName", apiKey.getName());
        user.put("roles", List.of());
        user.put("apiKeyId", apiKey.getId());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("valid", true);
        body.put("user", user);

        return ResponseEntity.ok(body);
    }

    public record VerifyRequest(String apiKey) {}
}
