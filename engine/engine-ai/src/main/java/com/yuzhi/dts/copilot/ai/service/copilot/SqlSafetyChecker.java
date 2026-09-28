package com.yuzhi.dts.copilot.ai.service.copilot;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validates generated SQL for safety - only allows SELECT and WITH (CTE) statements.
 */
@Component
public class SqlSafetyChecker {

    private static final Set<String> BLOCKED_KEYWORDS = Set.of(
            "INSERT", "UPDATE", "DELETE", "DROP", "TRUNCATE", "ALTER", "CREATE",
            "GRANT", "REVOKE", "EXEC", "EXECUTE", "MERGE", "REPLACE"
    );

    private static final Pattern MARKDOWN_FENCE_PATTERN = Pattern.compile(
            "^```(?:sql)?\\s*\\n?|\\n?```\\s*$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

    private final CaliberRuleRegistry caliberRuleRegistry;

    public SqlSafetyChecker(CaliberRuleRegistry caliberRuleRegistry) {
        this.caliberRuleRegistry = caliberRuleRegistry;
    }

    /**
     * Check if the SQL is safe (only SELECT/WITH statements allowed).
     */
    public boolean isSafe(String sql) {
        return validate(null, sql).safe();
    }

    public boolean isSafe(String domain, String sql) {
        return validate(domain, sql).safe();
    }

    public SqlSafetyValidation validate(String domain, String sql) {
        if (sql == null || sql.isBlank()) {
            return SqlSafetyValidation.blocked("SQL is blank. Only SELECT queries are allowed.");
        }
        String cleaned = sanitize(sql).trim().toUpperCase();
        // Remove comments
        cleaned = cleaned.replaceAll("--[^\\n]*", "").replaceAll("/\\*.*?\\*/", "").trim();

        if (cleaned.isEmpty()) {
            return SqlSafetyValidation.blocked("SQL is blank after sanitization. Only SELECT queries are allowed.");
        }

        // Must start with SELECT or WITH
        if (!cleaned.startsWith("SELECT") && !cleaned.startsWith("WITH")) {
            return SqlSafetyValidation.blocked("Only SELECT or WITH queries are allowed.");
        }

        // Check for blocked keywords at statement boundaries
        for (String keyword : BLOCKED_KEYWORDS) {
            Pattern pattern = Pattern.compile(
                    "(?:^|;|\\s)" + keyword + "\\s", Pattern.CASE_INSENSITIVE);
            if (pattern.matcher(cleaned).find()) {
                return SqlSafetyValidation.blocked("Only SELECT queries are allowed; blocked keyword: " + keyword + ".");
            }
        }

        String caliberDomain = normalizeCaliberDomain(domain);
        if (!caliberDomain.isBlank()) {
            CaliberRuleRegistry.CaliberValidation caliberValidation =
                    caliberRuleRegistry.validateSql(caliberDomain, sql);
            if (!caliberValidation.allowed()) {
                List<String> reasons = new ArrayList<>();
                for (CaliberRuleRegistry.CaliberViolation violation : caliberValidation.violations()) {
                    reasons.add(violation.ruleId() + ": " + violation.reason());
                }
                return new SqlSafetyValidation(false, reasons);
            }
        }

        return SqlSafetyValidation.allowed();
    }

    /**
     * Remove markdown code fences and trim whitespace.
     */
    public String sanitize(String sql) {
        if (sql == null) {
            return "";
        }
        return MARKDOWN_FENCE_PATTERN.matcher(sql).replaceAll("").trim();
    }

    private static String normalizeCaliberDomain(String domain) {
        if (domain == null || domain.isBlank()) {
            return "";
        }
        return switch (domain.trim().toLowerCase(Locale.ROOT)) {
            case "finance", "financial", "finace", "settlement", "财务", "结算" -> "finance";
            default -> "";
        };
    }

    public record SqlSafetyValidation(boolean safe, List<String> reasons) {
        public SqlSafetyValidation {
            reasons = reasons == null ? List.of() : List.copyOf(reasons);
        }

        static SqlSafetyValidation allowed() {
            return new SqlSafetyValidation(true, List.of());
        }

        static SqlSafetyValidation blocked(String reason) {
            return new SqlSafetyValidation(false, List.of(reason));
        }
    }
}
