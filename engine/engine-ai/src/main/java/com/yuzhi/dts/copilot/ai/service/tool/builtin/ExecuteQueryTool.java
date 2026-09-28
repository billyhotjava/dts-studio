package com.yuzhi.dts.copilot.ai.service.tool.builtin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yuzhi.dts.copilot.ai.service.safety.SqlSandbox;
import com.yuzhi.dts.copilot.ai.service.tool.CopilotTool;
import com.yuzhi.dts.copilot.ai.service.tool.ToolConnectionProvider;
import com.yuzhi.dts.copilot.ai.service.tool.ToolContext;
import com.yuzhi.dts.copilot.ai.service.tool.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;

/**
 * Tool that executes SELECT SQL queries against a registered data source.
 * All queries are validated through the {@link SqlSandbox} before execution.
 */
@Component
public class ExecuteQueryTool implements CopilotTool {

    private static final Logger log = LoggerFactory.getLogger(ExecuteQueryTool.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final int MAX_ROWS = 100;

    private final SqlSandbox sqlSandbox;
    private final ToolConnectionProvider connectionProvider;

    public ExecuteQueryTool(SqlSandbox sqlSandbox, ToolConnectionProvider connectionProvider) {
        this.sqlSandbox = sqlSandbox;
        this.connectionProvider = connectionProvider;
    }

    @Override
    public String name() {
        return "execute_query";
    }

    @Override
    public String description() {
        return "Execute a SELECT SQL query against the connected database and return the results. " +
               "Only read-only queries are allowed.";
    }

    @Override
    public JsonNode parameterSchema() {
        ObjectNode schema = mapper.createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = schema.putObject("properties");

        ObjectNode sqlProp = properties.putObject("sql");
        sqlProp.put("type", "string");
        sqlProp.put("description", "The SELECT SQL query to execute");

        ArrayNode required = schema.putArray("required");
        required.add("sql");

        return schema;
    }

    @Override
    public ToolResult execute(ToolContext context, JsonNode arguments) {
        String sql = arguments.has("sql") ? arguments.get("sql").asText() : null;
        if (sql == null || sql.isBlank()) {
            return ToolResult.failure("Missing required parameter: sql");
        }

        // Safety check
        SqlSandbox.SafetyResult safety = sqlSandbox.validate(sql);
        if (!safety.safe()) {
            log.warn("SQL blocked by sandbox: {} - reason: {}", sql, safety.reason());
            return ToolResult.failure("Query blocked: " + safety.reason());
        }

        try (Connection conn = connectionProvider.openConnection(context);
             Statement stmt = conn.createStatement()) {

            stmt.setMaxRows(MAX_ROWS);
            stmt.setQueryTimeout(30);

            boolean hasResultSet = stmt.execute(sql);
            if (!hasResultSet) {
                return ToolResult.failure("Query did not return a result set");
            }

            try (ResultSet rs = stmt.getResultSet()) {
                ResultSetMetaData meta = rs.getMetaData();
                int columnCount = meta.getColumnCount();

                ArrayNode rows = mapper.createArrayNode();
                int rowCount = 0;

                while (rs.next() && rowCount < MAX_ROWS) {
                    ObjectNode row = mapper.createObjectNode();
                    for (int i = 1; i <= columnCount; i++) {
                        String colName = meta.getColumnLabel(i);
                        Object value = rs.getObject(i);
                        if (value == null) {
                            row.putNull(colName);
                        } else if (value instanceof Number) {
                            if (value instanceof Long) {
                                row.put(colName, (Long) value);
                            } else if (value instanceof Double) {
                                row.put(colName, (Double) value);
                            } else if (value instanceof Integer) {
                                row.put(colName, (Integer) value);
                            } else {
                                row.put(colName, value.toString());
                            }
                        } else if (value instanceof Boolean) {
                            row.put(colName, (Boolean) value);
                        } else {
                            row.put(colName, value.toString());
                        }
                    }
                    rows.add(row);
                    rowCount++;
                }

                // Build column info
                StringBuilder columns = new StringBuilder();
                for (int i = 1; i <= columnCount; i++) {
                    if (i > 1) columns.append(", ");
                    columns.append(meta.getColumnLabel(i));
                }

                String output = String.format("Query returned %d row(s). Columns: %s",
                        rowCount, columns);
                return ToolResult.success(output, rows);
            }
        } catch (IllegalArgumentException e) {
            return ToolResult.failure(e.getMessage());
        } catch (Exception e) {
            log.error("Query execution failed: {}", e.getMessage());
            return ToolResult.failure("Query execution error: " + e.getMessage());
        }
    }
}
