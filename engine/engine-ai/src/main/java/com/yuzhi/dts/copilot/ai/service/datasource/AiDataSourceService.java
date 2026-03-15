package com.yuzhi.dts.copilot.ai.service.datasource;

import com.yuzhi.dts.copilot.ai.domain.AiDataSource;
import com.yuzhi.dts.copilot.ai.repository.AiDataSourceRepository;
import com.yuzhi.dts.copilot.ai.web.rest.dto.AiDataSourceUpsertRequest;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class AiDataSourceService {

    private final AiDataSourceRepository repository;

    public AiDataSourceService(AiDataSourceRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<AiDataSource> listDataSources() {
        return repository.findAllByOrderByUpdatedAtDescIdDesc();
    }

    @Transactional(readOnly = true)
    public Optional<AiDataSource> getDataSource(Long id) {
        return repository.findById(id);
    }

    public AiDataSource createDataSource(AiDataSourceUpsertRequest request) {
        AiDataSource entity = new AiDataSource();
        apply(entity, request);
        return repository.save(entity);
    }

    public AiDataSource updateDataSource(Long id, AiDataSourceUpsertRequest request) {
        AiDataSource existing = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Data source not found: " + id));
        apply(existing, request);
        return repository.save(existing);
    }

    public void deleteDataSource(Long id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("Data source not found: " + id);
        }
        repository.deleteById(id);
    }

    private void apply(AiDataSource entity, AiDataSourceUpsertRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        String name = trimToNull(request.name());
        String type = normalizeType(request.type());
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("name is required");
        }
        if (!StringUtils.hasText(type)) {
            throw new IllegalArgumentException("type is required");
        }
        entity.setName(name);
        entity.setDbType(type);
        entity.setJdbcUrl(resolveJdbcUrl(type, request));
        entity.setUsername(trimToNull(request.username()));
        entity.setPassword(trimToNull(request.password()));
        entity.setDescription(trimToNull(request.description()));
        entity.setStatus("ACTIVE");
    }

    static String resolveJdbcUrl(String type, AiDataSourceUpsertRequest request) {
        String jdbcUrl = trimToNull(request.jdbcUrl());
        if (StringUtils.hasText(jdbcUrl)) {
            if (jdbcUrl.startsWith("dbc:")) {
                return "jdbc:" + jdbcUrl.substring(4);
            }
            return jdbcUrl;
        }

        String host = trimToNull(request.host());
        Integer port = request.port();
        String database = trimToNull(request.database());

        if ("postgres".equals(type)) {
            if (!StringUtils.hasText(host) || !StringUtils.hasText(database)) {
                throw new IllegalArgumentException("Postgres data source requires host and database");
            }
            return "jdbc:postgresql://%s:%d/%s".formatted(host, port != null ? port : 5432, database);
        }
        if ("mysql".equals(type)) {
            if (!StringUtils.hasText(host) || !StringUtils.hasText(database)) {
                throw new IllegalArgumentException("MySQL data source requires host and database");
            }
            return "jdbc:mysql://%s:%d/%s".formatted(host, port != null ? port : 3306, database);
        }
        if ("oracle".equals(type)) {
            if (!StringUtils.hasText(host)) {
                throw new IllegalArgumentException("Oracle data source requires host");
            }
            String serviceName = trimToNull(request.serviceName());
            String sid = trimToNull(request.sid());
            int resolvedPort = port != null ? port : 1521;
            if (StringUtils.hasText(serviceName)) {
                return "jdbc:oracle:thin:@//%s:%d/%s".formatted(host, resolvedPort, serviceName);
            }
            if (StringUtils.hasText(sid)) {
                return "jdbc:oracle:thin:@%s:%d:%s".formatted(host, resolvedPort, sid);
            }
            throw new IllegalArgumentException("Oracle data source requires serviceName or sid");
        }

        throw new IllegalArgumentException("jdbcUrl is required for type=" + type);
    }

    static String normalizeType(String type) {
        String normalized = trimToNull(type);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        return switch (normalized.toLowerCase(Locale.ROOT)) {
            case "postgresql", "postgres", "pg" -> "postgres";
            case "mysql", "mariadb" -> "mysql";
            case "oracle" -> "oracle";
            case "dm", "dameng" -> "dm";
            default -> normalized.toLowerCase(Locale.ROOT);
        };
    }

    private static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
