package com.yuzhi.dts.copilot.ai.service.tool;

import com.yuzhi.dts.copilot.ai.domain.AiDataSource;
import com.yuzhi.dts.copilot.ai.repository.AiDataSourceRepository;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PreDestroy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ManagedToolConnectionProvider implements ToolConnectionProvider {

    private final AiDataSourceRepository dataSourceRepository;
    private final Environment environment;
    private final Map<Long, CachedDataSource> cache = new ConcurrentHashMap<>();

    public ManagedToolConnectionProvider(AiDataSourceRepository dataSourceRepository, Environment environment) {
        this.dataSourceRepository = dataSourceRepository;
        this.environment = environment;
    }

    @Override
    public Connection openConnection(ToolContext context) throws SQLException {
        Long dataSourceId = context.dataSourceId();
        if (dataSourceId == null) {
            throw new IllegalArgumentException("Please select a datasource before using Copilot database tools.");
        }

        AiDataSource entity = dataSourceRepository.findById(dataSourceId)
                .orElseThrow(() -> new IllegalArgumentException("Selected datasource %d was not found.".formatted(dataSourceId)));
        if (!StringUtils.hasText(entity.getJdbcUrl())) {
            throw new IllegalArgumentException("Selected datasource %d does not have a JDBC URL.".formatted(dataSourceId));
        }

        return resolveDataSource(entity).getConnection();
    }

    @PreDestroy
    void closeAll() {
        cache.values().forEach(entry -> entry.dataSource().close());
        cache.clear();
    }

    private HikariDataSource resolveDataSource(AiDataSource entity) {
        CachedDataSource cached = cache.compute(entity.getId(), (id, existing) -> {
            String configKey = configKey(entity);
            if (existing != null && existing.configKey().equals(configKey)) {
                return existing;
            }
            HikariDataSource replacement = buildDataSource(entity);
            if (existing != null) {
                existing.dataSource().close();
            }
            return new CachedDataSource(configKey, replacement);
        });
        return cached.dataSource();
    }

    private HikariDataSource buildDataSource(AiDataSource entity) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(resolveConfiguredText(entity.getJdbcUrl()));
        config.setUsername(resolveConfiguredText(entity.getUsername()));
        config.setPassword(resolveConfiguredText(entity.getPassword()));
        config.setPoolName("copilot-tool-" + entity.getId());
        config.setMaximumPoolSize(3);
        config.setMinimumIdle(0);
        config.setConnectionTimeout(5000);
        config.setValidationTimeout(3000);
        config.setInitializationFailTimeout(5000);
        return new HikariDataSource(config);
    }

    private String configKey(AiDataSource entity) {
        return String.join("|",
                safe(resolveConfiguredText(entity.getJdbcUrl())),
                safe(resolveConfiguredText(entity.getUsername())),
                safe(resolveConfiguredText(entity.getPassword())),
                safe(entity.getDbType()));
    }

    private String resolveConfiguredText(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        String resolved = environment.resolvePlaceholders(value);
        return StringUtils.hasText(resolved) ? resolved.trim() : resolved;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private record CachedDataSource(String configKey, HikariDataSource dataSource) {}
}
