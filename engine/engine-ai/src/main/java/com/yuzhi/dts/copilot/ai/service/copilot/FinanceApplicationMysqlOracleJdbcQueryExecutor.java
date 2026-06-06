package com.yuzhi.dts.copilot.ai.service.copilot;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.util.StringUtils;

public class FinanceApplicationMysqlOracleJdbcQueryExecutor
        implements FinanceApplicationMysqlOracleProofService.QueryExecutor {

    private static final Pattern UNSAFE_SQL = Pattern.compile(
            "\\b(insert|update|delete|drop|alter|truncate|merge|create|grant|revoke|call)\\b",
            Pattern.CASE_INSENSITIVE);

    private final JdbcOperations jdbcOperations;
    private final String expectedDatabase;

    public FinanceApplicationMysqlOracleJdbcQueryExecutor(JdbcOperations jdbcOperations, String expectedDatabase) {
        this.jdbcOperations = jdbcOperations;
        this.expectedDatabase = textOrEmpty(expectedDatabase);
    }

    @Override
    public List<Map<String, Object>> query(String database, String nativeSql) {
        String requestedDatabase = textOrEmpty(database);
        if (StringUtils.hasText(expectedDatabase) && !expectedDatabase.equals(requestedDatabase)) {
            throw new IllegalArgumentException("Unexpected application MySQL oracle database: expected="
                    + expectedDatabase + ", actual=" + requestedDatabase);
        }
        assertReadOnlyApplicationMysqlSql(nativeSql);
        return jdbcOperations.queryForList(nativeSql).stream()
                .map(FinanceApplicationMysqlOracleJdbcQueryExecutor::normalizeRow)
                .toList();
    }

    private static void assertReadOnlyApplicationMysqlSql(String nativeSql) {
        String sql = textOrEmpty(nativeSql);
        String trimmed = sql.stripLeading();
        String lowered = trimmed.toLowerCase(Locale.ROOT);
        if (!(lowered.startsWith("select ") || lowered.startsWith("with "))) {
            throw new IllegalArgumentException("Application MySQL oracle SQL must be read-only");
        }
        if (lowered.contains(";") || UNSAFE_SQL.matcher(lowered).find()) {
            throw new IllegalArgumentException("Application MySQL oracle SQL must be read-only");
        }
        if (lowered.contains("public.ods_") || lowered.contains("mysql.rs_cloud_flower")
                || lowered.contains("jdbc:mysql") || lowered.contains("password")) {
            throw new IllegalArgumentException("Application MySQL oracle SQL must query application tables directly");
        }
    }

    private static Map<String, Object> normalizeRow(Map<String, Object> row) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        if (row == null) {
            return normalized;
        }
        row.forEach((key, value) -> {
            String canonicalKey = canonicalKey(key);
            normalized.put(canonicalKey, "amount".equals(canonicalKey) ? amount(value) : value);
        });
        return normalized;
    }

    private static String canonicalKey(String key) {
        String safeKey = textOrEmpty(key);
        return switch (safeKey.toLowerCase(Locale.ROOT)) {
            case "accountperiod" -> "accountPeriod";
            case "metricid" -> "metricId";
            default -> safeKey;
        };
    }

    private static BigDecimal amount(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        String text = textOrEmpty(value);
        return text.isEmpty() ? BigDecimal.ZERO : new BigDecimal(text);
    }

    private static String textOrEmpty(Object value) {
        return value == null ? "" : value.toString();
    }
}
