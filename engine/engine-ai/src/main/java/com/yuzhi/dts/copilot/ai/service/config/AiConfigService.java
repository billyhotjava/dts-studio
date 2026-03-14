package com.yuzhi.dts.copilot.ai.service.config;

import com.yuzhi.dts.copilot.ai.domain.AiProviderConfig;
import com.yuzhi.dts.copilot.ai.repository.AiProviderConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Manages AI provider configurations (CRUD + defaults).
 */
@Service
@Transactional
public class AiConfigService {

    private static final Logger log = LoggerFactory.getLogger(AiConfigService.class);

    private final AiProviderConfigRepository repository;

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

    private void clearDefaultFlag() {
        repository.findByIsDefaultTrue().ifPresent(existing -> {
            existing.setIsDefault(false);
            repository.save(existing);
        });
    }
}
