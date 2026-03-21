package com.yuzhi.dts.copilot.analytics.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class LocalAiDataSourceLookup {

    private final JdbcTemplate jdbcTemplate;

    public LocalAiDataSourceLookup(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<PlatformInfraClient.DataSourceDetail> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return jdbcTemplate.query(
                """
                SELECT
                    id,
                    name,
                    db_type,
                    jdbc_url,
                    username,
                    password,
                    description,
                    status,
                    updated_at
                FROM copilot_ai.data_source
                WHERE id = ?
                """,
                ps -> ps.setLong(1, id),
                rs -> rs.next() ? Optional.of(mapDetail(rs)) : Optional.empty());
    }

    private PlatformInfraClient.DataSourceDetail mapDetail(ResultSet rs) throws SQLException {
        String password = rs.getString("password");
        return new PlatformInfraClient.DataSourceDetail(
                String.valueOf(rs.getLong("id")),
                rs.getString("name"),
                rs.getString("db_type"),
                rs.getString("jdbc_url"),
                rs.getString("username"),
                rs.getString("description"),
                null,
                Map.of(),
                password == null || password.isBlank() ? Map.of() : Map.of("password", password),
                rs.getString("status"),
                rs.getTimestamp("updated_at") == null ? null : rs.getTimestamp("updated_at").toInstant().toString());
    }
}
