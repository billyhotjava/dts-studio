package com.yuzhi.dts.copilot.ai.service.copilot;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CaliberRuleRegistry {

    private static final Logger log = LoggerFactory.getLogger(CaliberRuleRegistry.class);
    private static final String RULE_RESOURCE = "governance/caliber-rules.v1.json";
    private static final String STATIC_SQL = "SQL_STATIC";

    private static final Pattern COMMENT_PATTERN = Pattern.compile("--[^\\n]*|/\\*.*?\\*/", Pattern.DOTALL);
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
    private static final Pattern UNQUALIFIED_BIZ_TYPE_PATTERN = Pattern.compile(
            "(?:where|and|or|on)\\s+biz_type\\s*(?:=|<>|!=|in\\s*\\()",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern DIRECT_JSON_JOIN_PATTERN = Pattern.compile(
            "(?:\\bbiz_ids_json\\b\\s*=|=\\s*(?:\\w+\\.)?\\bbiz_ids_json\\b)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern RAW_RENT_AGGREGATE_PATTERN = Pattern.compile(
            "\\b(?:sum|avg|min|max)\\s*\\(\\s*(?:\\w+\\.)?rent\\s*\\)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SOURCE_TYPE_EIGHT_PATTERN = Pattern.compile(
            "\\bsource_type\\s*=\\s*8\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SALE_ACCOUNT_AMOUNT_AGGREGATE_PATTERN = Pattern.compile(
            "\\b(?:sum|avg)\\s*\\(\\s*(?:\\w+\\.)?(?:receivable_amount|biz_amount)\\s*\\)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern BAD_DEBT_EXCLUSION_PATTERN = Pattern.compile(
            "\\bbiz_type\\s*(?:<>|!=)\\s*6\\b"
                    + "|\\bbiz_type\\s+not\\s+in\\s*\\([^)]*\\b6\\b[^)]*\\)"
                    + "|\\bbiz_type\\s*=\\s*(?:7|8)\\b"
                    + "|\\bbiz_type\\s+in\\s*\\(\\s*(?:7\\s*,\\s*8|8\\s*,\\s*7|7|8)\\s*\\)",
            Pattern.CASE_INSENSITIVE);

    private final ObjectMapper objectMapper;
    private List<CaliberRule> rules = List.of();

    public CaliberRuleRegistry(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(RULE_RESOURCE)) {
            if (is == null) {
                log.warn("Caliber rule resource not found: {}", RULE_RESOURCE);
                this.rules = List.of();
                return;
            }
            CaliberRuleDocument document = objectMapper.readValue(is, CaliberRuleDocument.class);
            this.rules = List.copyOf(document.rules());
            log.info("Loaded {} caliber rule(s) from {}", rules.size(), RULE_RESOURCE);
        } catch (Exception e) {
            log.warn("Failed to load caliber rules from {}: {}", RULE_RESOURCE, e.getMessage());
            this.rules = List.of();
        }
    }

    public List<CaliberRule> rules() {
        return rules;
    }

    public List<String> guardrailsForDomain(String domain) {
        String normalizedDomain = normalizeDomain(domain);
        List<String> guardrails = new ArrayList<>();
        for (CaliberRule rule : rules) {
            if (rule.appliesToDomain(normalizedDomain)) {
                guardrails.add("[" + rule.id() + "] " + rule.guardrailText());
            }
        }
        return List.copyOf(guardrails);
    }

    public CaliberValidation validateSql(String domain, String sql) {
        if (sql == null || sql.isBlank()) {
            return new CaliberValidation(true, List.of());
        }
        String normalizedDomain = normalizeDomain(domain);
        String normalizedSql = normalizeSql(sql);
        List<CaliberViolation> violations = new ArrayList<>();
        for (CaliberRule rule : rules) {
            if (rule.appliesToDomain(normalizedDomain)
                    && STATIC_SQL.equalsIgnoreCase(rule.check().type())
                    && violates(rule.id(), normalizedSql)) {
                violations.add(new CaliberViolation(rule.id(), rule.severity(), rule.check().reason()));
            }
        }
        return new CaliberValidation(violations.isEmpty(), violations);
    }

    private boolean violates(String ruleId, String sql) {
        return switch (ruleId) {
            case "CAL-BIZTYPE-SCOPE" -> UNQUALIFIED_BIZ_TYPE_PATTERN.matcher(sql).find();
            case "CAL-SETTLEMENT-CHAIN" -> (containsAll(sql, "a_month_accounting", "a_sale_account")
                    && sql.contains("sum(")) || saleAccountIncomeMayIncludeBadDebt(sql);
            case "CAL-SALE-IN-RENT" -> containsAll(sql, "a_month_accounting", "a_green_accounting",
                    "a_sale_account") && SOURCE_TYPE_EIGHT_PATTERN.matcher(sql).find() && sql.contains("sum(");
            case "CAL-JSON-EXPAND" -> sql.contains("biz_ids_json")
                    && DIRECT_JSON_JOIN_PATTERN.matcher(sql).find()
                    && !sql.contains("jsonb_array_elements_text")
                    && !sql.contains("json_table");
            case "CAL-VARCHAR-AMOUNT-CAST" -> sql.contains("a_sale_account_rent_item")
                    && RAW_RENT_AGGREGATE_PATTERN.matcher(sql).find();
            default -> false;
        };
    }

    private static boolean containsAll(String value, String... tokens) {
        for (String token : tokens) {
            if (!value.contains(token)) {
                return false;
            }
        }
        return true;
    }

    private static boolean saleAccountIncomeMayIncludeBadDebt(String sql) {
        return sql.contains("a_sale_account")
                && SALE_ACCOUNT_AMOUNT_AGGREGATE_PATTERN.matcher(sql).find()
                && !BAD_DEBT_EXCLUSION_PATTERN.matcher(sql).find();
    }

    private static String normalizeSql(String sql) {
        String withoutComments = COMMENT_PATTERN.matcher(sql).replaceAll(" ");
        return WHITESPACE_PATTERN.matcher(withoutComments)
                .replaceAll(" ")
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private static String normalizeDomain(String domain) {
        return domain == null ? "" : domain.trim().toLowerCase(Locale.ROOT);
    }

    private static <T> List<T> copyOrEmpty(List<T> values) {
        return values == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(values));
    }

    private record CaliberRuleDocument(String version, List<CaliberRule> rules) {
        private CaliberRuleDocument {
            rules = copyOrEmpty(rules);
        }
    }

    public record CaliberRule(
            String id,
            String description,
            List<String> domains,
            List<String> appliesTo,
            String severity,
            CaliberCheck check,
            String guardrailText) {

        public CaliberRule {
            domains = copyOrEmpty(domains);
            appliesTo = copyOrEmpty(appliesTo);
            severity = severity == null ? "warning" : severity;
            check = check == null ? new CaliberCheck("METADATA", "") : check;
            guardrailText = guardrailText == null ? description : guardrailText;
        }

        private boolean appliesToDomain(String domain) {
            return domains.isEmpty() || domains.stream()
                    .map(CaliberRuleRegistry::normalizeDomain)
                    .anyMatch(domain::equals);
        }
    }

    public record CaliberCheck(String type, String reason) {
        public CaliberCheck {
            type = type == null || type.isBlank() ? "METADATA" : type;
            reason = reason == null ? "" : reason;
        }
    }

    public record CaliberViolation(String ruleId, String severity, String reason) {
    }

    public record CaliberValidation(boolean allowed, List<CaliberViolation> violations) {
        public CaliberValidation {
            violations = copyOrEmpty(violations);
        }
    }
}
