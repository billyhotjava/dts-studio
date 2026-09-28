package com.yuzhi.dts.copilot.ai.service.auth;

import com.yuzhi.dts.copilot.ai.domain.ApiKey;
import com.yuzhi.dts.copilot.ai.repository.ApiKeyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing API keys: generation, validation, revocation, and rotation.
 */
@Service
@Transactional
public class ApiKeyService {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyService.class);
    private static final String KEY_PREFIX = "cpk_";
    private static final int KEY_RANDOM_LENGTH = 32;
    private static final String ALPHANUMERIC = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final ApiKeyRepository apiKeyRepository;

    public ApiKeyService(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    /**
     * Result object returned when a new API key is created.
     */
    public record ApiKeyCreateResult(Long id, String rawKey, String prefix) {}

    /**
     * Generate a new API key.
     */
    public ApiKeyCreateResult generateKey(String name, String description, String createdBy, Integer expiresInDays) {
        String randomPart = generateRandomString(KEY_RANDOM_LENGTH);
        String rawKey = KEY_PREFIX + randomPart;
        String keyHash = sha256(rawKey);
        String prefix = rawKey.substring(0, Math.min(rawKey.length(), 8));

        ApiKey apiKey = new ApiKey();
        apiKey.setKeyHash(keyHash);
        apiKey.setKeyPrefix(prefix);
        apiKey.setName(name);
        apiKey.setDescription(description);
        apiKey.setCreatedBy(createdBy);
        apiKey.setStatus("ACTIVE");

        if (expiresInDays != null && expiresInDays > 0) {
            apiKey.setExpiresAt(Instant.now().plus(expiresInDays, ChronoUnit.DAYS));
        }

        apiKey = apiKeyRepository.save(apiKey);
        log.info("Generated new API key id={} prefix={} for user={}", apiKey.getId(), prefix, createdBy);

        return new ApiKeyCreateResult(apiKey.getId(), rawKey, prefix);
    }

    /**
     * Validate a raw API key and return the corresponding entity if valid.
     */
    @Transactional(readOnly = true)
    public Optional<ApiKey> validateKey(String rawKey) {
        if (rawKey == null || !rawKey.startsWith(KEY_PREFIX)) {
            return Optional.empty();
        }

        String keyHash = sha256(rawKey);
        Optional<ApiKey> optKey = apiKeyRepository.findByKeyHash(keyHash);

        if (optKey.isEmpty()) {
            return Optional.empty();
        }

        ApiKey apiKey = optKey.get();

        if (!"ACTIVE".equals(apiKey.getStatus())) {
            log.debug("API key id={} has status={}, rejecting", apiKey.getId(), apiKey.getStatus());
            return Optional.empty();
        }

        if (apiKey.getExpiresAt() != null && Instant.now().isAfter(apiKey.getExpiresAt())) {
            log.debug("API key id={} has expired at {}", apiKey.getId(), apiKey.getExpiresAt());
            return Optional.empty();
        }

        return Optional.of(apiKey);
    }

    /**
     * Revoke an API key by setting its status to REVOKED.
     */
    public void revokeKey(Long id) {
        ApiKey apiKey = apiKeyRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("API key not found: " + id));
        apiKey.setStatus("REVOKED");
        apiKeyRepository.save(apiKey);
        log.info("Revoked API key id={}", id);
    }

    /**
     * Rotate an API key: revoke the old one and create a new one with the same metadata.
     */
    public ApiKeyCreateResult rotateKey(Long id) {
        ApiKey oldKey = apiKeyRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("API key not found: " + id));
        oldKey.setStatus("REVOKED");
        apiKeyRepository.save(oldKey);

        Integer expiresInDays = null;
        if (oldKey.getExpiresAt() != null) {
            long remaining = Instant.now().until(oldKey.getExpiresAt(), ChronoUnit.DAYS);
            expiresInDays = (int) Math.max(remaining, 1);
        }

        log.info("Rotating API key id={}, creating replacement", id);
        return generateKey(oldKey.getName(), oldKey.getDescription(), oldKey.getCreatedBy(), expiresInDays);
    }

    /**
     * List all active API keys.
     */
    @Transactional(readOnly = true)
    public List<ApiKey> listKeys() {
        return apiKeyRepository.findAllByStatus("ACTIVE");
    }

    /**
     * Increment usage counter and update last-used timestamp.
     */
    public void incrementUsage(Long id) {
        apiKeyRepository.findById(id).ifPresent(apiKey -> {
            apiKey.setUsageCount(apiKey.getUsageCount() + 1);
            apiKey.setLastUsedAt(Instant.now());
            apiKeyRepository.save(apiKey);
        });
    }

    private String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHANUMERIC.charAt(SECURE_RANDOM.nextInt(ALPHANUMERIC.length())));
        }
        return sb.toString();
    }

    static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
