package com.yuzhi.dts.copilot.analytics.service;

import com.yuzhi.dts.copilot.analytics.domain.AnalyticsDatabase;
import com.yuzhi.dts.copilot.analytics.repository.AnalyticsDatabaseRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for executing SQL queries against registered data sources via JDBC.
 */
@Service
public class QueryExecutionService {

    private static final Logger log = LoggerFactory.getLogger(QueryExecutionService.class);
    private static final int MAX_ROWS = 10_000;
    private static final int QUERY_TIMEOUT_SECONDS = 120;

    private final AnalyticsDatabaseRepository databaseRepository;
    private final ObjectMapper objectMapper;

    public QueryExecutionService(AnalyticsDatabaseRepository databaseRepository,
                                 ObjectMapper objectMapper) {
        this.databaseRepository = databaseRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Execute a SQL query against the specified database.
     *
     * @param databaseId the analytics database ID
     * @param queryJson  the dataset query as a JSON string containing a "query" field with the SQL
     * @return the query result
     */
    public QueryResult executeQuery(Long databaseId, String queryJson) {
        if (databaseId == null) {
            throw new IllegalArgumentException("Database ID is required");
        }

        AnalyticsDatabase database = databaseRepository.findById(databaseId)
                .orElseThrow(() -> new IllegalArgumentException("Database not found: " + databaseId));

        String sql = extractSql(queryJson);
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("No SQL query provided");
        }

        Map<String, String> connDetails = parseConnectionDetails(database.getDetails());
        String jdbcUrl = buildJdbcUrl(database.getEngine(), connDetails);
        String username = connDetails.getOrDefault("user", "");
        String password = connDetails.getOrDefault("password", "");

        log.info("Executing query on database id={} engine={}", databaseId, database.getEngine());

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             Statement stmt = conn.createStatement()) {

            stmt.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            stmt.setMaxRows(MAX_ROWS);

            boolean hasResultSet = stmt.execute(sql);
            if (!hasResultSet) {
                int updateCount = stmt.getUpdateCount();
                return new QueryResult(List.of(), List.of(), updateCount);
            }

            try (ResultSet rs = stmt.getResultSet()) {
                return mapResultSet(rs);
            }
        } catch (SQLException e) {
            log.error("Query execution failed on database id={}: {}", databaseId, e.getMessage());
            throw new RuntimeException("Query execution failed: " + e.getMessage(), e);
        }
    }

    private String extractSql(String queryJson) {
        if (queryJson == null) {
            return null;
        }
        // Try to parse as JSON with a "query" field
        try {
            Map<String, Object> queryMap = objectMapper.readValue(queryJson, new TypeReference<>() {});
            Object query = queryMap.get("query");
            if (query != null) {
                return query.toString();
            }
            // Fallback: try "native" -> "query"
            Object nativeObj = queryMap.get("native");
            if (nativeObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nativeMap = (Map<String, Object>) nativeObj;
                Object nativeQuery = nativeMap.get("query");
                if (nativeQuery != null) {
                    return nativeQuery.toString();
                }
            }
        } catch (JsonProcessingException e) {
            // Not valid JSON - treat as raw SQL
            return queryJson;
        }
        return queryJson;
    }

    private Map<String, String> parseConnectionDetails(String detailsJson) {
        if (detailsJson == null || detailsJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(detailsJson, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse database connection details: {}", e.getMessage());
            return Map.of();
        }
    }

    private String buildJdbcUrl(String engine, Map<String, String> details) {
        String host = details.getOrDefault("host", "localhost");
        String port = details.getOrDefault("port", "5432");
        String dbName = details.getOrDefault("dbname", details.getOrDefault("db", ""));

        return switch (engine.toLowerCase()) {
            case "postgres", "postgresql" ->
                    String.format("jdbc:postgresql://%s:%s/%s", host, port, dbName);
            case "mysql", "mariadb" ->
                    String.format("jdbc:mysql://%s:%s/%s", host, port, dbName);
            case "h2" ->
                    String.format("jdbc:h2:%s", details.getOrDefault("url", "mem:test"));
            default ->
                    throw new IllegalArgumentException("Unsupported database engine: " + engine);
        };
    }

    private QueryResult mapResultSet(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int columnCount = meta.getColumnCount();

        List<String> columns = new ArrayList<>(columnCount);
        for (int i = 1; i <= columnCount; i++) {
            columns.add(meta.getColumnLabel(i));
        }

        List<List<Object>> rows = new ArrayList<>();
        int rowCount = 0;
        while (rs.next() && rowCount < MAX_ROWS) {
            List<Object> row = new ArrayList<>(columnCount);
            for (int i = 1; i <= columnCount; i++) {
                row.add(rs.getObject(i));
            }
            rows.add(row);
            rowCount++;
        }

        return new QueryResult(columns, rows, rowCount);
    }

    /**
     * Result of a query execution.
     */
    public record QueryResult(
            List<String> columns,
            List<List<Object>> rows,
            int rowCount
    ) {}
}
