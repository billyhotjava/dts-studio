package com.yuzhi.dts.copilot.ai.service.copilot;

import org.springframework.stereotype.Component;

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

    /**
     * Check if the SQL is safe (only SELECT/WITH statements allowed).
     */
    public boolean isSafe(String sql) {
        if (sql == null || sql.isBlank()) {
            return false;
        }
        String cleaned = sanitize(sql).trim().toUpperCase();
        // Remove comments
        cleaned = cleaned.replaceAll("--[^\\n]*", "").replaceAll("/\\*.*?\\*/", "").trim();

        if (cleaned.isEmpty()) {
            return false;
        }

        // Must start with SELECT or WITH
        if (!cleaned.startsWith("SELECT") && !cleaned.startsWith("WITH")) {
            return false;
        }

        // Check for blocked keywords at statement boundaries
        for (String keyword : BLOCKED_KEYWORDS) {
            Pattern pattern = Pattern.compile(
                    "(?:^|;|\\s)" + keyword + "\\s", Pattern.CASE_INSENSITIVE);
            if (pattern.matcher(cleaned).find()) {
                return false;
            }
        }

        return true;
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
}
