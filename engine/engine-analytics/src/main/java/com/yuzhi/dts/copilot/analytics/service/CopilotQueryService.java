package com.yuzhi.dts.copilot.analytics.service;

import java.sql.SQLException;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CopilotQueryService {

    private static final Logger log = LoggerFactory.getLogger(CopilotQueryService.class);

    // View whitelist - only these views are allowed for copilot queries
    private static final Set<String> ALLOWED_VIEWS = Set.of(
            "v_project_overview",
            "v_flower_biz_detail",
            "v_project_green_current",
            "v_monthly_settlement",
            "v_task_progress",
            "v_curing_coverage",
            "v_pendulum_progress");

    // Role-restricted views
    private static final Set<String> FINANCE_RESTRICTED_VIEWS = Set.of(
            "v_monthly_settlement");

    private static final Pattern FROM_JOIN_PATTERN =
            Pattern.compile("(?:from|join)\\s+(\\w+)", Pattern.CASE_INSENSITIVE);

    private final DatasetQueryService datasetQueryService;

    public CopilotQueryService(DatasetQueryService datasetQueryService) {
        this.datasetQueryService = datasetQueryService;
    }

    /**
     * Validate that the SQL only references allowed views.
     * Simple approach: extract all table/view references from SQL
     * and check against whitelist.
     */
    public void validateSqlSources(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new ForbiddenQueryException("SQL 不能为空。");
        }

        String normalized = sql.toLowerCase();
        Matcher matcher = FROM_JOIN_PATTERN.matcher(normalized);

        while (matcher.find()) {
            String tableName = matcher.group(1);
            if (!ALLOWED_VIEWS.contains(tableName) && !isSubqueryAlias(tableName)) {
                throw new ForbiddenQueryException(
                        "不允许查询表: " + tableName + "。Copilot 只能查询业务视图。");
            }
        }
    }

    /**
     * Check if a view is finance-restricted.
     */
    public boolean isFinanceRestricted(String sql) {
        if (sql == null || sql.isBlank()) {
            return false;
        }
        String normalized = sql.toLowerCase();
        Matcher matcher = FROM_JOIN_PATTERN.matcher(normalized);
        while (matcher.find()) {
            String tableName = matcher.group(1);
            if (FINANCE_RESTRICTED_VIEWS.contains(tableName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Helper: check if a name might be a subquery alias (heuristic).
     * Common aliases that aren't real tables: fb, po, t, etc.
     * Keep it simple - allow single/double/triple letter aliases.
     */
    private boolean isSubqueryAlias(String name) {
        return name.length() <= 3;
    }

    /**
     * Execute a validated copilot query.
     * Delegates to existing query execution infrastructure.
     */
    public DatasetQueryService.DatasetResult executeCopilotQuery(
            String sql, long datasourceId, String userId) throws SQLException {
        validateSqlSources(sql);
        log.info("[copilot-query] user={} datasource={} sql={}", userId, datasourceId, sql);
        return datasetQueryService.runNative(
                datasourceId, sql, DatasetQueryService.DatasetConstraints.defaults());
    }
}
