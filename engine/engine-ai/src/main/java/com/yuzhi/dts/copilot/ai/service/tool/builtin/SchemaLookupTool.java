package com.yuzhi.dts.copilot.ai.service.tool.builtin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yuzhi.dts.copilot.ai.service.tool.CopilotTool;
import com.yuzhi.dts.copilot.ai.service.tool.ToolContext;
import com.yuzhi.dts.copilot.ai.service.tool.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Tool that retrieves table and column metadata for a data source.
 * Useful for the LLM to understand the database schema before writing queries.
 */
@Component
public class SchemaLookupTool implements CopilotTool {

    private static final Logger log = LoggerFactory.getLogger(SchemaLookupTool.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final DataSource dataSource;

    public SchemaLookupTool(DataSource dataSource) {
        this.dataSource = dataSource;
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
        schemaProp.put("description", "Optional schema name (default: public)");

        return schema;
    }

    @Override
    public ToolResult execute(ToolContext context, JsonNode arguments) {
        String tableName = arguments.has("table_name") ? arguments.get("table_name").asText(null) : null;
        String schemaName = arguments.has("schema_name") ? arguments.get("schema_name").asText("public") : "public";

        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData dbMeta = conn.getMetaData();

            if (tableName != null && !tableName.isBlank()) {
                return getTableDetails(dbMeta, schemaName, tableName);
            } else {
                return listTables(dbMeta, schemaName);
            }
        } catch (Exception e) {
            log.error("Schema lookup failed: {}", e.getMessage());
            return ToolResult.failure("Schema lookup error: " + e.getMessage());
        }
    }

    private ToolResult listTables(DatabaseMetaData dbMeta, String schemaName) throws Exception {
        List<String> tableNames = new ArrayList<>();
        try (ResultSet rs = dbMeta.getTables(null, schemaName, "%",
                new String[]{"TABLE", "VIEW"})) {
            while (rs.next()) {
                String name = rs.getString("TABLE_NAME");
                String type = rs.getString("TABLE_TYPE");
                tableNames.add(name + " (" + type + ")");
            }
        }

        if (tableNames.isEmpty()) {
            return ToolResult.success("No tables found in schema '" + schemaName + "'.");
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Found %d tables/views in schema '%s':\n", tableNames.size(), schemaName));
        for (String name : tableNames) {
            sb.append("  - ").append(name).append('\n');
        }

        return ToolResult.success(sb.toString());
    }

    private ToolResult getTableDetails(DatabaseMetaData dbMeta, String schemaName,
                                       String tableName) throws Exception {
        // Get primary keys
        List<String> primaryKeys = new ArrayList<>();
        try (ResultSet rs = dbMeta.getPrimaryKeys(null, schemaName, tableName)) {
            while (rs.next()) {
                primaryKeys.add(rs.getString("COLUMN_NAME"));
            }
        }

        // Get columns
        ArrayNode columns = mapper.createArrayNode();
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Table: %s.%s\n", schemaName, tableName));
        sb.append("Columns:\n");

        try (ResultSet rs = dbMeta.getColumns(null, schemaName, tableName, "%")) {
            while (rs.next()) {
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

                sb.append(String.format("  - %s %s(%d)%s%s\n",
                        colName, colType, colSize,
                        nullable ? "" : " NOT NULL",
                        isPk ? " [PK]" : ""));
            }
        }

        if (columns.isEmpty()) {
            return ToolResult.failure("Table '" + tableName + "' not found in schema '" + schemaName + "'.");
        }

        if (!primaryKeys.isEmpty()) {
            sb.append("Primary key: ").append(String.join(", ", primaryKeys)).append('\n');
        }

        return ToolResult.success(sb.toString(), columns);
    }
}
