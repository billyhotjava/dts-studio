package com.yuzhi.dts.copilot.ai.service.safety;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validates SQL statements to ensure they are safe to execute.
 * Only allows read-only operations; blocks any data modification or DDL statements.
 */
@Component
public class SqlSandbox {

    private static final Logger log = LoggerFactory.getLogger(SqlSandbox.class);

    /**
     * SQL keywords/statements that are allowed.
     */
    private static final Set<String> ALLOWED_STATEMENTS = Set.of(
            "SELECT", "WITH", "EXPLAIN", "SHOW"
    );

    /**
     * SQL keywords/statements that are explicitly blocked.
     */
    private static final Set<String> BLOCKED_KEYWORDS = Set.of(
            "INSERT", "UPDATE", "DELETE", "DROP", "TRUNCATE",
            "ALTER", "CREATE", "GRANT", "REVOKE", "EXECUTE",
            "EXEC", "CALL", "COPY", "LOAD", "MERGE"
    );

    /**
     * Pattern to extract the first SQL keyword from a statement.
     */
    private static final Pattern FIRST_KEYWORD_PATTERN = Pattern.compile(
            "^\\s*(?:/\\*.*?\\*/\\s*)*([A-Za-z]+)", Pattern.DOTALL
    );

    /**
     * Pattern to detect blocked keywords anywhere in the SQL (case-insensitive).
     * Uses word boundaries to avoid false positives (e.g. "selection" matching "SELECT").
     */
    private static final Pattern BLOCKED_PATTERN;

    static {
        String keywords = String.join("|", BLOCKED_KEYWORDS);
        BLOCKED_PATTERN = Pattern.compile(
                "\\b(" + keywords + ")\\b", Pattern.CASE_INSENSITIVE
        );
    }

    /**
     * Validate whether the given SQL is safe to execute.
     *
     * @param sql the SQL statement to validate
     * @return a {@link SafetyResult} indicating whether it is safe
     */
    public SafetyResult validate(String sql) {
        if (sql == null || sql.isBlank()) {
            return new SafetyResult(false, "Empty SQL statement");
        }

        String trimmed = sql.trim();

        // Remove comments for analysis
        String cleaned = removeComments(trimmed);

        // Check first keyword is in the allowed set
        Matcher firstKeyword = FIRST_KEYWORD_PATTERN.matcher(cleaned);
        if (!firstKeyword.find()) {
            return new SafetyResult(false, "Cannot determine SQL statement type");
        }

        String keyword = firstKeyword.group(1).toUpperCase();
        if (!ALLOWED_STATEMENTS.contains(keyword)) {
            return new SafetyResult(false,
                    "Statement type '" + keyword + "' is not allowed. Only SELECT, WITH, EXPLAIN, and SHOW are permitted.");
        }

        // Scan the entire SQL for blocked keywords (catches subqueries, CTEs with mutations, etc.)
        Matcher blocked = BLOCKED_PATTERN.matcher(cleaned);
        if (blocked.find()) {
            String found = blocked.group(1).toUpperCase();
            return new SafetyResult(false,
                    "Blocked keyword '" + found + "' detected. Data modification is not allowed.");
        }

        // Check for multiple statements (semicolon-separated)
        String withoutStrings = removeStringLiterals(cleaned);
        if (withoutStrings.contains(";")) {
            // Allow trailing semicolon but block multiple statements
            String afterSemicolon = withoutStrings.substring(withoutStrings.indexOf(';') + 1).trim();
            if (!afterSemicolon.isEmpty()) {
                return new SafetyResult(false,
                        "Multiple statements are not allowed. Please submit one query at a time.");
            }
        }

        return new SafetyResult(true, null);
    }

    /**
     * Remove SQL line comments (--) and block comments.
     */
    private String removeComments(String sql) {
        // Remove block comments
        String result = sql.replaceAll("/\\*.*?\\*/", " ");
        // Remove line comments
        result = result.replaceAll("--[^\n]*", " ");
        return result;
    }

    /**
     * Remove string literals to prevent false positives when scanning for keywords.
     */
    private String removeStringLiterals(String sql) {
        return sql.replaceAll("'[^']*'", "''");
    }

    /**
     * Result of SQL safety validation.
     *
     * @param safe   whether the SQL is safe to execute
     * @param reason explanation if unsafe, {@code null} if safe
     */
    public record SafetyResult(boolean safe, String reason) {}
}
