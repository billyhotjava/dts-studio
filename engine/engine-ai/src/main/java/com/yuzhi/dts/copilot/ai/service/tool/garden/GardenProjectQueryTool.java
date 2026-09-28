package com.yuzhi.dts.copilot.ai.service.tool.garden;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yuzhi.dts.copilot.ai.service.safety.SqlSandbox;
import com.yuzhi.dts.copilot.ai.service.tool.CopilotTool;
import com.yuzhi.dts.copilot.ai.service.tool.ToolContext;
import com.yuzhi.dts.copilot.ai.service.tool.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * 园林项目查询工具。
 * 查询园林项目列表，支持按状态、区域、客户筛选。
 */
@Component
public class GardenProjectQueryTool implements CopilotTool {

    private static final Logger log = LoggerFactory.getLogger(GardenProjectQueryTool.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final int MAX_ROWS = 100;

    private final SqlSandbox sqlSandbox;
    private final DataSource dataSource;

    public GardenProjectQueryTool(SqlSandbox sqlSandbox, DataSource dataSource) {
        this.sqlSandbox = sqlSandbox;
        this.dataSource = dataSource;
    }

    @Override
    public String name() {
        return "query_garden_projects";
    }

    @Override
    public String description() {
        return "查询园林项目列表，支持按状态、区域、客户筛选。" +
               "返回项目名称、状态、区域、客户、起止日期等信息。";
    }

    @Override
    public JsonNode parameterSchema() {
        ObjectNode schema = mapper.createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = schema.putObject("properties");

        ObjectNode statusProp = properties.putObject("status");
        statusProp.put("type", "string");
        statusProp.put("description", "项目状态筛选，如：进行中、已完成、待启动");

        ObjectNode areaProp = properties.putObject("area");
        areaProp.put("type", "string");
        areaProp.put("description", "区域筛选，如：浦东新区、徐汇区");

        ObjectNode customerProp = properties.putObject("customer_name");
        customerProp.put("type", "string");
        customerProp.put("description", "客户名称模糊匹配");

        ObjectNode limitProp = properties.putObject("limit");
        limitProp.put("type", "integer");
        limitProp.put("description", "返回记录数上限，默认 50");

        // No required parameters - all filters are optional
        schema.putArray("required");

        return schema;
    }

    @Override
    public ToolResult execute(ToolContext context, JsonNode arguments) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT p.id, p.project_name, p.status, p.area, ");
        sql.append("p.customer_name, p.start_date, p.end_date, p.contract_amount ");
        sql.append("FROM p_project p WHERE 1=1");

        List<String> conditions = new ArrayList<>();

        if (arguments.has("status") && !arguments.get("status").asText().isBlank()) {
            String status = arguments.get("status").asText().replace("'", "''");
            conditions.add(" AND p.status = '" + status + "'");
        }

        if (arguments.has("area") && !arguments.get("area").asText().isBlank()) {
            String area = arguments.get("area").asText().replace("'", "''");
            conditions.add(" AND p.area = '" + area + "'");
        }

        if (arguments.has("customer_name") && !arguments.get("customer_name").asText().isBlank()) {
            String customer = arguments.get("customer_name").asText().replace("'", "''");
            conditions.add(" AND p.customer_name LIKE '%" + customer + "%'");
        }

        for (String condition : conditions) {
            sql.append(condition);
        }

        sql.append(" ORDER BY p.start_date DESC");

        int limit = arguments.has("limit") ? arguments.get("limit").asInt(50) : 50;
        if (limit > MAX_ROWS) limit = MAX_ROWS;
        sql.append(" LIMIT ").append(limit);

        String query = sql.toString();

        // Safety check through SqlSandbox
        SqlSandbox.SafetyResult safety = sqlSandbox.validate(query);
        if (!safety.safe()) {
            log.warn("SQL blocked by sandbox: {} - reason: {}", query, safety.reason());
            return ToolResult.failure("Query blocked: " + safety.reason());
        }

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.setMaxRows(MAX_ROWS);
            stmt.setQueryTimeout(30);

            try (ResultSet rs = stmt.executeQuery(query)) {
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
                        } else if (value instanceof Number num) {
                            row.put(colName, num.doubleValue());
                        } else {
                            row.put(colName, value.toString());
                        }
                    }
                    rows.add(row);
                    rowCount++;
                }

                return ToolResult.success(
                        String.format("查询到 %d 个园林项目", rowCount), rows);
            }
        } catch (Exception e) {
            log.error("Garden project query failed: {}", e.getMessage());
            return ToolResult.failure("查询失败: " + e.getMessage());
        }
    }
}
