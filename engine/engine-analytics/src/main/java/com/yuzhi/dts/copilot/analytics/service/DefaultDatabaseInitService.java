package com.yuzhi.dts.copilot.analytics.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuzhi.dts.copilot.analytics.config.DefaultDatabaseProperties;
import com.yuzhi.dts.copilot.analytics.config.DefaultDatabaseProperties.DatabaseEntry;
import com.yuzhi.dts.copilot.analytics.domain.AnalyticsDatabase;
import com.yuzhi.dts.copilot.analytics.repository.AnalyticsDatabaseRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultDatabaseInitService implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultDatabaseInitService.class);

    private final DefaultDatabaseProperties properties;
    private final AnalyticsDatabaseRepository databaseRepository;
    private final MetadataSyncService metadataSyncService;
    private final ObjectMapper objectMapper;

    public DefaultDatabaseInitService(
            DefaultDatabaseProperties properties,
            AnalyticsDatabaseRepository databaseRepository,
            MetadataSyncService metadataSyncService,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.databaseRepository = databaseRepository;
        this.metadataSyncService = metadataSyncService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<DatabaseEntry> entries = properties.defaultDatabases();
        if (entries.isEmpty()) {
            LOG.debug("No default databases configured");
            return;
        }

        LOG.info("Checking {} default database(s) for auto-registration", entries.size());

        Set<String> existingNames = databaseRepository.findAll().stream()
                .map(AnalyticsDatabase::getName)
                .collect(Collectors.toSet());

        for (DatabaseEntry entry : entries) {
            if (entry.name() == null || entry.name().isBlank()) {
                LOG.warn("Skipping default database entry with empty name");
                continue;
            }
            if (existingNames.contains(entry.name())) {
                LOG.info("Default database '{}' already exists, skipping", entry.name());
                continue;
            }

            try {
                registerDatabase(entry);
            } catch (Exception e) {
                LOG.error("Failed to register default database '{}': {}", entry.name(), e.getMessage(), e);
            }
        }
    }

    @Transactional
    protected void registerDatabase(DatabaseEntry entry) {
        String detailsJson = buildDetailsJson(entry);

        AnalyticsDatabase database = new AnalyticsDatabase();
        database.setName(entry.name());
        database.setEngine(mapEngine(entry.engine()));
        database.setDetailsJson(detailsJson);
        database.setSample(false);
        database.setAutoRunQueries(true);
        database.setFullSync(true);
        database.setOnDemand(false);

        database = databaseRepository.save(database);
        LOG.info("Registered default database '{}' with id={}", entry.name(), database.getId());

        if (entry.autoSyncMetadata()) {
            triggerMetadataSync(database);
        }
    }

    private String buildDetailsJson(DatabaseEntry entry) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("host", entry.host());
        details.put("port", entry.port());
        details.put("dbname", entry.db());
        details.put("user", entry.user());
        if (entry.password() != null && !entry.password().isBlank()) {
            details.put("password", entry.password());
        }
        try {
            return objectMapper.writeValueAsString(details);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize database details for " + entry.name(), e);
        }
    }

    private void triggerMetadataSync(AnalyticsDatabase database) {
        try {
            MetadataSyncService.SyncSummary summary = metadataSyncService.syncDatabaseSchema(database.getId());
            LOG.info(
                    "Metadata sync completed for '{}': tables(created={}, updated={}, disabled={}), fields(created={}, updated={}, disabled={})",
                    database.getName(),
                    summary.createdTables(),
                    summary.updatedTables(),
                    summary.disabledTables(),
                    summary.createdFields(),
                    summary.updatedFields(),
                    summary.disabledFields());
        } catch (Exception e) {
            LOG.warn("Metadata sync failed for default database '{}' (id={}). Will retry on next restart or manual trigger: {}",
                    database.getName(), database.getId(), e.getMessage());
        }
    }

    private static String mapEngine(String engine) {
        if (engine == null) {
            return "postgres";
        }
        return switch (engine.toLowerCase()) {
            case "postgresql", "pg" -> "postgres";
            default -> engine.toLowerCase();
        };
    }
}
