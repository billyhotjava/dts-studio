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
 * 财务摘要查询工具。
 * 查询财务摘要：收款情况、应收账款、月度营收趋势。
 */
@Component
public class FinanceSummaryTool implements CopilotTool {

    private static final Logger log = LoggerFactory.getLogger(FinanceSummaryTool.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final SqlSandbox sqlSandbox;
    private final DataSource dataSource;

    public FinanceSummaryTool(SqlSandbox sqlSandbox, DataSource dataSource) {
        this.sqlSandbox = sqlSandbox;
        this.dataSource = dataSource;
    }

    @Override
    public String name() {
        return "finance_summary";
    }

    @Override
    public String description() {
        return "查询财务摘要：收款情况、应收账款、月度营收趋势。" +
               "支持按报表类型查看概览、月度趋势或应收明细。";
    }

    @Override
    public JsonNode parameterSchema() {
        ObjectNode schema = mapper.createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = schema.putObject("properties");

        ObjectNode reportTypeProp = properties.putObject("report_type");
        reportTypeProp.put("type", "string");
        reportTypeProp.put("description",
                "报表类型：overview（财务概览）、monthly_trend（月度营收趋势）、receivables（应收账款明细）。默认 overview");

        ObjectNode yearProp = properties.putObject("year");
        yearProp.put("type", "integer");
        yearProp.put("description", "查询年份，默认当前年");

        ObjectNode monthProp = properties.putObject("month");
        monthProp.put("type", "integer");
        monthProp.put("description", "查询月份（1-12），仅 overview 类型使用");

        schema.putArray("required");

        return schema;
    }

    @Override
    public ToolResult execute(ToolContext context, JsonNode arguments) {
        String reportType = arguments.has("report_type")
                ? arguments.get("report_type").asText("overview") : "overview";
        int year = arguments.has("year")
                ? arguments.get("year").asInt() : java.time.Year.now().getValue();

        String sql;
        switch (reportType) {
            case "monthly_trend" -> sql =
                    "SELECT DATE_FORMAT(f.payment_date, '%Y-%m') AS 月份, " +
                    "ROUND(SUM(f.amount), 2) AS 收款金额, " +
                    "COUNT(*) AS 收款笔数, " +
                    "ROUND(SUM(f.amount) / COUNT(*), 2) AS 笔均金额 " +
                    "FROM f_finance f " +
                    "WHERE YEAR(f.payment_date) = " + year + " " +
                    "AND f.type = '收款' " +
                    "GROUP BY DATE_FORMAT(f.payment_date, '%Y-%m') " +
                    "ORDER BY 月份";

            case "receivables" -> sql =
                    "SELECT c.customer_name AS 客户, p.project_name AS 项目, " +
                    "ROUND(f.contract_amount, 2) AS 合同金额, " +
                    "ROUND(f.received_amount, 2) AS 已收金额, " +
                    "ROUND(f.contract_amount - f.received_amount, 2) AS 应收余额, " +
                    "f.due_date AS 到期日 " +
                    "FROM f_receivable f " +
                    "JOIN p_project p ON f.project_id = p.id " +
                    "JOIN c_customer c ON p.customer_id = c.id " +
                    "WHERE f.contract_amount > f.received_amount " +
                    "ORDER BY 应收余额 DESC LIMIT 50";

            default -> {
                String monthFilter = "";
                if (arguments.has("month")) {
                    int month = arguments.get("month").asInt();
                    monthFilter = " AND MONTH(f.payment_date) = " + month;
                }
                sql = "SELECT " +
                      "ROUND(SUM(CASE WHEN f.type = '收款' THEN f.amount ELSE 0 END), 2) AS 总收款, " +
                      "ROUND(SUM(CASE WHEN f.type = '支出' THEN f.amount ELSE 0 END), 2) AS 总支出, " +
                      "ROUND(SUM(CASE WHEN f.type = '收款' THEN f.amount ELSE 0 END) - " +
                      "SUM(CASE WHEN f.type = '支出' THEN f.amount ELSE 0 END), 2) AS 净收入, " +
                      "COUNT(DISTINCT CASE WHEN f.type = '收款' THEN f.id END) AS 收款笔数, " +
                      "COUNT(DISTINCT CASE WHEN f.type = '支出' THEN f.id END) AS 支出笔数 " +
                      "FROM f_finance f " +
                      "WHERE YEAR(f.payment_date) = " + year + monthFilter;
            }
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

                String label = switch (reportType) {
                    case "monthly_trend" -> year + "年月度营收趋势";
                    case "receivables" -> "应收账款明细";
                    default -> year + "年财务概览";
                };
                return ToolResult.success(
                        String.format("财务摘要（%s）：%d 条记录", label, rowCount), rows);
            }
        } catch (Exception e) {
            log.error("Finance summary query failed: {}", e.getMessage());
            return ToolResult.failure("财务查询失败: " + e.getMessage());
        }
    }
}
