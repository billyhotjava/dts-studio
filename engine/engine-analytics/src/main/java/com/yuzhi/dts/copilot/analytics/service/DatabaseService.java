package com.yuzhi.dts.copilot.analytics.service;

import com.yuzhi.dts.copilot.analytics.domain.AnalyticsDatabase;
import com.yuzhi.dts.copilot.analytics.repository.AnalyticsDatabaseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for managing analytics database registrations.
 * Delegates metadata operations to copilot-ai when needed.
 */
@Service
@Transactional
public class DatabaseService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseService.class);

    private final AnalyticsDatabaseRepository databaseRepository;
    private final CopilotAiClient copilotAiClient;

    public DatabaseService(AnalyticsDatabaseRepository databaseRepository,
                           CopilotAiClient copilotAiClient) {
        this.databaseRepository = databaseRepository;
        this.copilotAiClient = copilotAiClient;
    }

    @Transactional(readOnly = true)
    public List<AnalyticsDatabase> findAll() {
        return databaseRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<AnalyticsDatabase> findById(Long id) {
        return databaseRepository.findById(id);
    }

    public AnalyticsDatabase create(AnalyticsDatabase database) {
        database.setCreatedAt(Instant.now());
        database.setUpdatedAt(Instant.now());
        AnalyticsDatabase saved = databaseRepository.save(database);
        log.info("Created analytics database id={} name={}", saved.getId(), saved.getName());
        return saved;
    }

    public AnalyticsDatabase update(Long id, AnalyticsDatabase updated) {
        AnalyticsDatabase existing = databaseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Database not found: " + id));

        existing.setName(updated.getName());
        existing.setEngine(updated.getEngine());
        existing.setDetails(updated.getDetails());
        existing.setUpdatedAt(Instant.now());

        return databaseRepository.save(existing);
    }

    public void delete(Long id) {
        databaseRepository.deleteById(id);
        log.info("Deleted analytics database id={}", id);
    }

    /**
     * Sync metadata from copilot-ai for a given database.
     */
    public List<Map<String, Object>> syncMetadata(String apiKey) {
        return copilotAiClient.getDataSources(apiKey);
    }
}
