package com.yuzhi.dts.copilot.ai.service.tool.builtin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yuzhi.dts.copilot.ai.service.tool.CopilotTool;
import com.yuzhi.dts.copilot.ai.service.tool.ToolConnectionProvider;
import com.yuzhi.dts.copilot.ai.service.tool.ToolContext;
import com.yuzhi.dts.copilot.ai.service.tool.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Tool that retrieves table and column metadata for a data source.
 * Useful for the LLM to understand the database schema before writing queries.
 */
@Component
public class SchemaLookupTool implements CopilotTool {

    private static final Logger log = LoggerFactory.getLogger(SchemaLookupTool.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final ToolConnectionProvider connectionProvider;

    public SchemaLookupTool(ToolConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    @Override
    public String name() {
        return "schema_lookup";
    }

    @Override
    public String description() {
        return "Look up database schema information including table names, column names, " +
               "column types, and primary keys. Use this to understand the database structure " +
               "before writing queries.";
    }

    @Override
    public JsonNode parameterSchema() {
        ObjectNode schema = mapper.createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = schema.putObject("properties");

        ObjectNode tableProp = properties.putObject("table_name");
        tableProp.put("type", "string");
        tableProp.put("description",
                "Optional table name to get detailed column info. " +
                "If omitted, returns list of all tables.");

        ObjectNode schemaProp = properties.putObject("schema_name");
        schemaProp.put("type", "string");
        schemaProp.put("description", "Optional schema name. Leave empty for the datasource default scope.");

        return schema;
    }

    @Override
    public ToolResult execute(ToolContext context, JsonNode arguments) {
        String tableName = arguments.has("table_name") ? arguments.get("table_name").asText(null) : null;
        String schemaName = arguments.has("schema_name") ? arguments.get("schema_name").asText(null) : null;

        try (Connection conn = connectionProvider.openConnection(context)) {
            DatabaseMetaData dbMeta = conn.getMetaData();

            if (tableName != null && !tableName.isBlank()) {
                return getTableDetails(conn, dbMeta, schemaName, tableName);
            } else {
                return listTables(conn, dbMeta, schemaName);
            }
        } catch (IllegalArgumentException e) {
            return ToolResult.failure(e.getMessage());
        } catch (Exception e) {
            log.error("Schema lookup failed: {}", e.getMessage());
            return ToolResult.failure("Schema lookup error: " + e.getMessage());
        }
    }

    private ToolResult listTables(Connection conn, DatabaseMetaData dbMeta, String schemaName) throws Exception {
        List<String> tableNames = new ArrayList<>();
        String schemaFilter = normalize(schemaName);
        try (ResultSet rs = dbMeta.getTables(conn.getCatalog(), schemaFilter, "%",
                new String[]{"TABLE", "VIEW"})) {
            while (rs.next()) {
                String namespace = resolveNamespace(rs.getString("TABLE_SCHEM"), rs.getString("TABLE_CAT"));
                if (isSystemNamespace(namespace)) {
                    continue;
                }
                String name = rs.getString("TABLE_NAME");
                String type = rs.getString("TABLE_TYPE");
                String label = (namespace == null ? name : namespace + "." + name) + " (" + type + ")";
                tableNames.add(label);
            }
        }

        if (tableNames.isEmpty()) {
            if (schemaFilter != null) {
                return ToolResult.success("No tables found in schema '" + schemaFilter + "'.");
            }
            return ToolResult.success("No tables found for the selected datasource.");
        }

        StringBuilder sb = new StringBuilder();
        if (schemaFilter != null) {
            sb.append(String.format("Found %d tables/views in schema '%s':\n", tableNames.size(), schemaFilter));
        } else {
            sb.append(String.format("Found %d tables/views in the selected datasource:\n", tableNames.size()));
        }
        for (String name : tableNames) {
            sb.append("  - ").append(name).append('\n');
        }

        return ToolResult.success(sb.toString());
    }

    private ToolResult getTableDetails(Connection conn, DatabaseMetaData dbMeta, String schemaName,
                                       String tableName) throws Exception {
        String schemaFilter = normalize(schemaName);
        // Get primary keys
        List<String> primaryKeys = new ArrayList<>();
        try (ResultSet rs = dbMeta.getPrimaryKeys(conn.getCatalog(), schemaFilter, tableName)) {
            while (rs.next()) {
                primaryKeys.add(rs.getString("COLUMN_NAME"));
            }
        }

        // Get columns
        ArrayNode columns = mapper.createArrayNode();
        StringBuilder sb = new StringBuilder();

        String namespace = schemaFilter;
        try (ResultSet rs = dbMeta.getColumns(conn.getCatalog(), schemaFilter, tableName, "%")) {
            while (rs.next()) {
                if (namespace == null) {
                    namespace = resolveNamespace(rs.getString("TABLE_SCHEM"), rs.getString("TABLE_CAT"));
                }
                String colName = rs.getString("COLUMN_NAME");
                String colType = rs.getString("TYPE_NAME");
                int colSize = rs.getInt("COLUMN_SIZE");
                boolean nullable = rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable;
                boolean isPk = primaryKeys.contains(colName);

                ObjectNode col = mapper.createObjectNode();
                col.put("name", colName);
                col.put("type", colType);
                col.put("size", colSize);
                col.put("nullable", nullable);
                col.put("primary_key", isPk);
                columns.add(col);

                if (sb.isEmpty()) {
                    String qualifiedName = namespace == null ? tableName : namespace + "." + tableName;
                    sb.append(String.format("Table: %s\n", qualifiedName));
                    sb.append("Columns:\n");
                }
                sb.append(String.format("  - %s %s(%d)%s%s\n",
                        colName, colType, colSize,
                        nullable ? "" : " NOT NULL",
                        isPk ? " [PK]" : ""));
            }
        }

        if (columns.isEmpty()) {
            if (schemaFilter != null) {
                return ToolResult.failure("Table '" + tableName + "' not found in schema '" + schemaFilter + "'.");
            }
            return ToolResult.failure("Table '" + tableName + "' was not found in the selected datasource.");
        }

        if (!primaryKeys.isEmpty()) {
            sb.append("Primary key: ").append(String.join(", ", primaryKeys)).append('\n');
        }

        return ToolResult.success(sb.toString(), columns);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String resolveNamespace(String schema, String catalog) {
        String normalizedSchema = normalize(schema);
        if (normalizedSchema != null) {
            return normalizedSchema;
        }
        return normalize(catalog);
    }

    private boolean isSystemNamespace(String namespace) {
        if (namespace == null) {
            return false;
        }
        String lower = namespace.toLowerCase(Locale.ROOT);
        return "information_schema".equals(lower)
                || lower.startsWith("pg_")
                || "sys".equals(lower)
                || "mysql".equals(lower)
                || "performance_schema".equals(lower);
    }
}
