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

/**
 * 花卉租赁统计工具。
 * 统计花卉租赁数据：在租数量、品种分布、养护状态。
 */
@Component
public class FlowerStatsTool implements CopilotTool {

    private static final Logger log = LoggerFactory.getLogger(FlowerStatsTool.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final SqlSandbox sqlSandbox;
    private final DataSource dataSource;

    public FlowerStatsTool(SqlSandbox sqlSandbox, DataSource dataSource) {
        this.sqlSandbox = sqlSandbox;
        this.dataSource = dataSource;
    }

    @Override
    public String name() {
        return "flower_statistics";
    }

    @Override
    public String description() {
        return "统计花卉租赁数据：在租数量、品种分布、养护状态。" +
               "支持按统计维度查看概览、品种分布或养护状态分布。";
    }

    @Override
    public JsonNode parameterSchema() {
        ObjectNode schema = mapper.createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = schema.putObject("properties");

        ObjectNode dimensionProp = properties.putObject("dimension");
        dimensionProp.put("type", "string");
        dimensionProp.put("description",
                "统计维度：overview（概览）、variety（品种分布）、maintenance（养护状态）。默认 overview");

        ObjectNode projectIdProp = properties.putObject("project_id");
        projectIdProp.put("type", "integer");
        projectIdProp.put("description", "按项目 ID 筛选，不传则查全部");

        schema.putArray("required");

        return schema;
    }

    @Override
    public ToolResult execute(ToolContext context, JsonNode arguments) {
        String dimension = arguments.has("dimension")
                ? arguments.get("dimension").asText("overview") : "overview";
        String projectFilter = "";

        if (arguments.has("project_id")) {
            int projectId = arguments.get("project_id").asInt();
            projectFilter = " AND f.project_id = " + projectId;
        }

        String sql;
        switch (dimension) {
            case "variety" -> sql =
                    "SELECT f.variety_name AS 品种, COUNT(*) AS 数量, " +
                    "SUM(CASE WHEN f.rental_status = '在租' THEN 1 ELSE 0 END) AS 在租数量, " +
                    "SUM(CASE WHEN f.rental_status = '待租' THEN 1 ELSE 0 END) AS 待租数量 " +
                    "FROM f_flower f WHERE 1=1" + projectFilter +
                    " GROUP BY f.variety_name ORDER BY 数量 DESC LIMIT 50";

            case "maintenance" -> sql =
                    "SELECT f.maintenance_status AS 养护状态, COUNT(*) AS 数量, " +
                    "ROUND(COUNT(*) * 100.0 / (SELECT COUNT(*) FROM f_flower), 1) AS 占比 " +
                    "FROM f_flower f WHERE 1=1" + projectFilter +
                    " GROUP BY f.maintenance_status ORDER BY 数量 DESC";

            default -> sql =
                    "SELECT " +
                    "COUNT(*) AS 总数量, " +
                    "SUM(CASE WHEN f.rental_status = '在租' THEN 1 ELSE 0 END) AS 在租数量, " +
                    "SUM(CASE WHEN f.rental_status = '待租' THEN 1 ELSE 0 END) AS 待租数量, " +
                    "COUNT(DISTINCT f.variety_name) AS 品种数, " +
                    "SUM(CASE WHEN f.maintenance_status = '正常' THEN 1 ELSE 0 END) AS 养护正常, " +
                    "SUM(CASE WHEN f.maintenance_status != '正常' THEN 1 ELSE 0 END) AS 需关注 " +
                    "FROM f_flower f WHERE 1=1" + projectFilter;
        }

        // Safety check through SqlSandbox
        SqlSandbox.SafetyResult safety = sqlSandbox.validate(sql);
        if (!safety.safe()) {
            log.warn("SQL blocked by sandbox: {} - reason: {}", sql, safety.reason());
            return ToolResult.failure("Query blocked: " + safety.reason());
        }

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.setMaxRows(100);
            stmt.setQueryTimeout(30);

            try (ResultSet rs = stmt.executeQuery(sql)) {
                ResultSetMetaData meta = rs.getMetaData();
                int columnCount = meta.getColumnCount();
                ArrayNode rows = mapper.createArrayNode();
                int rowCount = 0;

                while (rs.next()) {
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

                String label = switch (dimension) {
                    case "variety" -> "品种分布";
                    case "maintenance" -> "养护状态分布";
                    default -> "花卉租赁概览";
                };
                return ToolResult.success(
                        String.format("花卉统计（%s）：%d 条记录", label, rowCount), rows);
            }
        } catch (Exception e) {
            log.error("Flower statistics query failed: {}", e.getMessage());
            return ToolResult.failure("统计查询失败: " + e.getMessage());
        }
    }
}
