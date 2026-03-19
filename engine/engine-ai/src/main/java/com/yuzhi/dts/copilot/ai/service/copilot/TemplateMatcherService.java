package com.yuzhi.dts.copilot.ai.service.copilot;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuzhi.dts.copilot.ai.domain.Nl2SqlQueryTemplate;
import com.yuzhi.dts.copilot.ai.repository.Nl2SqlQueryTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Template matching engine for NL2SQL.
 * Matches user questions against pre-built query templates
 * using regex intent patterns and extracts parameters.
 */
@Service
public class TemplateMatcherService {

    private static final Logger log = LoggerFactory.getLogger(TemplateMatcherService.class);

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private final Nl2SqlQueryTemplateRepository templateRepository;
    private final ObjectMapper objectMapper;

    /** Simple field cache for active templates. */
    private volatile List<Nl2SqlQueryTemplate> cachedTemplates;
    private volatile long cacheTimestamp;
    private static final long CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutes

    public TemplateMatcherService(Nl2SqlQueryTemplateRepository templateRepository,
                                  ObjectMapper objectMapper) {
        this.templateRepository = templateRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Result of a template match attempt.
     */
    public record TemplateMatchResult(
            boolean matched,
            Nl2SqlQueryTemplate template,
            Map<String, String> extractedParams,
            String resolvedSql
    ) {}

    /**
     * Suggested question for welcome card display.
     */
    public record SuggestedQuestion(
            String templateCode,
            String domain,
            String roleHint,
            String question,
            String description
    ) {}

    /**
     * Match a user question against all active templates.
     *
     * @param userQuestion the natural language question from the user
     * @return match result with template, extracted params, and resolved SQL
     */
    public TemplateMatchResult match(String userQuestion) {
        if (userQuestion == null || userQuestion.isBlank()) {
            return new TemplateMatchResult(false, null, null, null);
        }

        List<Nl2SqlQueryTemplate> templates = loadActiveTemplates();
        if (templates.isEmpty()) {
            log.warn("No active query templates found");
            return new TemplateMatchResult(false, null, null, null);
        }

        // Try matching each template's intent patterns (highest priority first)
        for (Nl2SqlQueryTemplate template : templates) {
            List<String> patterns = parseJsonArray(template.getIntentPatterns());
            for (String patternStr : patterns) {
                try {
                    Pattern pattern = Pattern.compile(patternStr);
                    Matcher matcher = pattern.matcher(userQuestion);
                    if (matcher.find()) {
                        log.debug("Matched template {} with pattern: {}", template.getTemplateCode(), patternStr);

                        Map<String, String> paramDefs = parseParamDefinitions(template.getParameters());
                        Map<String, String> extractedParams = extractParameters(userQuestion, paramDefs);
                        String resolvedSql = resolveSql(template.getSqlTemplate(), extractedParams);

                        return new TemplateMatchResult(true, template, extractedParams, resolvedSql);
                    }
                } catch (Exception e) {
                    log.warn("Invalid regex pattern '{}' in template {}: {}",
                            patternStr, template.getTemplateCode(), e.getMessage());
                }
            }
        }

        log.debug("No template matched for question: {}", userQuestion);
        return new TemplateMatchResult(false, null, null, null);
    }

    /**
     * Get suggested questions for the welcome card.
     * Returns a sampling from each domain's question_samples.
     *
     * @param limit maximum number of suggestions to return
     * @return list of suggested questions
     */
    public List<SuggestedQuestion> getSuggestedQuestions(int limit) {
        List<Nl2SqlQueryTemplate> templates = loadActiveTemplates();
        if (templates.isEmpty()) {
            return Collections.emptyList();
        }

        // Group templates by domain
        Map<String, List<Nl2SqlQueryTemplate>> byDomain = new LinkedHashMap<>();
        for (Nl2SqlQueryTemplate t : templates) {
            byDomain.computeIfAbsent(t.getDomain(), k -> new ArrayList<>()).add(t);
        }

        // Pick 1-2 questions from each domain, round-robin
        List<SuggestedQuestion> suggestions = new ArrayList<>();
        int perDomain = Math.max(1, limit / Math.max(1, byDomain.size()));

        for (Map.Entry<String, List<Nl2SqlQueryTemplate>> entry : byDomain.entrySet()) {
            int picked = 0;
            for (Nl2SqlQueryTemplate t : entry.getValue()) {
                if (picked >= perDomain) break;

                List<String> samples = parseJsonArray(t.getQuestionSamples());
                if (!samples.isEmpty()) {
                    suggestions.add(new SuggestedQuestion(
                            t.getTemplateCode(),
                            t.getDomain(),
                            t.getRoleHint(),
                            samples.get(0),
                            t.getDescription()
                    ));
                    picked++;
                }
            }
        }

        // Trim to limit
        if (suggestions.size() > limit) {
            return suggestions.subList(0, limit);
        }
        return suggestions;
    }

    // ========== Parameter extraction ==========

    /**
     * Extract parameter values from a user question based on parameter definitions.
     */
    private Map<String, String> extractParameters(String question, Map<String, String> paramDefs) {
        Map<String, String> params = new HashMap<>();

        for (Map.Entry<String, String> entry : paramDefs.entrySet()) {
            String paramName = entry.getKey();
            String defJson = entry.getValue();

            String value = null;

            switch (paramName) {
                case "month" -> value = extractMonth(question, defJson);
                case "project_name" -> value = extractNameBeforeKeyword(question, "项目");
                case "customer_name" -> value = extractNameBeforeKeyword(question, "客户");
                case "curing_user" -> value = extractNameBeforeKeyword(question, "养护人");
                default -> value = resolveDefault(defJson);
            }

            if (value != null) {
                params.put(paramName, value);
            }
        }

        return params;
    }

    /**
     * Extract month reference from question text.
     * "本月"/"这个月" -> current YYYY-MM
     * "上月"/"上个月" -> previous YYYY-MM
     * Falls back to default from param definition.
     */
    private String extractMonth(String question, String defJson) {
        LocalDate today = LocalDate.now();

        if (question.contains("上月") || question.contains("上个月")) {
            return today.minusMonths(1).format(MONTH_FORMATTER);
        }
        if (question.contains("本月") || question.contains("这个月")) {
            return today.format(MONTH_FORMATTER);
        }

        // Check for explicit YYYY-MM pattern in question
        Matcher m = Pattern.compile("(\\d{4}-\\d{2})").matcher(question);
        if (m.find()) {
            return m.group(1);
        }

        // Use default
        String defaultVal = extractDefaultFromDef(defJson);
        if ("CURRENT_MONTH".equals(defaultVal)) {
            return today.format(MONTH_FORMATTER);
        }
        if ("LAST_MONTH".equals(defaultVal)) {
            return today.minusMonths(1).format(MONTH_FORMATTER);
        }
        return defaultVal;
    }

    /**
     * Extract a name that appears before a keyword in the question.
     * e.g., "万科项目" -> extracts "万科" before "项目".
     * Also handles "XX客户" or names after keywords like "客户XX".
     */
    private String extractNameBeforeKeyword(String question, String keyword) {
        // Pattern: Chinese characters before the keyword
        Pattern beforePattern = Pattern.compile("([\\u4e00-\\u9fa5A-Za-z0-9]{2,20})" + keyword);
        Matcher beforeMatcher = beforePattern.matcher(question);
        if (beforeMatcher.find()) {
            String candidate = beforeMatcher.group(1);
            // Filter out common question words that aren't entity names
            if (!isStopWord(candidate)) {
                return candidate;
            }
        }

        // Pattern: keyword followed by Chinese characters
        Pattern afterPattern = Pattern.compile(keyword + "([\\u4e00-\\u9fa5A-Za-z0-9]{2,20})");
        Matcher afterMatcher = afterPattern.matcher(question);
        if (afterMatcher.find()) {
            String candidate = afterMatcher.group(1);
            if (!isStopWord(candidate)) {
                return candidate;
            }
        }

        return null;
    }

    /**
     * Check if a candidate name is a common stop word / question word.
     */
    private boolean isStopWord(String word) {
        return word.matches("(的|有|在|了|是|这个|那个|什么|多少|几个|哪些|目前|一共|当前|所有|各个|各)");
    }

    /**
     * Resolve default value from a param definition JSON string.
     * For numeric defaults, returns the default value.
     */
    private String resolveDefault(String defJson) {
        return extractDefaultFromDef(defJson);
    }

    /**
     * Extract the "default" field from a single parameter definition JSON string.
     */
    private String extractDefaultFromDef(String defJson) {
        if (defJson == null || defJson.isBlank()) {
            return null;
        }
        try {
            Map<String, Object> def = objectMapper.readValue(defJson, new TypeReference<>() {});
            Object defaultVal = def.get("default");
            if (defaultVal == null) {
                return null;
            }
            String val = defaultVal.toString();
            LocalDate today = LocalDate.now();
            if ("CURRENT_MONTH".equals(val)) {
                return today.format(MONTH_FORMATTER);
            }
            if ("LAST_MONTH".equals(val)) {
                return today.minusMonths(1).format(MONTH_FORMATTER);
            }
            return val;
        } catch (Exception e) {
            return null;
        }
    }

    // ========== SQL resolution ==========

    /**
     * Replace :param_name placeholders in SQL with actual values.
     * For null optional params, keeps them as NULL (the SQL uses :param IS NULL OR ... pattern).
     */
    private String resolveSql(String sqlTemplate, Map<String, String> params) {
        String sql = sqlTemplate;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String placeholder = ":" + entry.getKey();
            String value = entry.getValue();
            if (value == null) {
                sql = sql.replace(placeholder, "NULL");
            } else {
                // For string values, wrap in quotes; for numeric values, use as-is
                if (isNumeric(value)) {
                    sql = sql.replace(placeholder, value);
                } else {
                    sql = sql.replace(placeholder, "'" + escapeSql(value) + "'");
                }
            }
        }
        return sql;
    }

    private boolean isNumeric(String s) {
        try {
            Double.parseDouble(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String escapeSql(String value) {
        if (value == null) return "";
        return value.replace("'", "''");
    }

    // ========== Parameter definition parsing ==========

    /**
     * Parse the parameters JSON object into a map of paramName -> individual param def JSON.
     */
    private Map<String, String> parseParamDefinitions(String parametersJson) {
        if (parametersJson == null || parametersJson.isBlank() || "{}".equals(parametersJson.trim())) {
            return Collections.emptyMap();
        }
        try {
            Map<String, Object> defs = objectMapper.readValue(parametersJson, new TypeReference<>() {});
            Map<String, String> result = new HashMap<>();
            for (Map.Entry<String, Object> entry : defs.entrySet()) {
                result.put(entry.getKey(), objectMapper.writeValueAsString(entry.getValue()));
            }
            return result;
        } catch (Exception e) {
            log.warn("Failed to parse parameters JSON: {}", parametersJson, e);
            return Collections.emptyMap();
        }
    }

    // ========== Cache and JSON helpers ==========

    private List<Nl2SqlQueryTemplate> loadActiveTemplates() {
        long now = System.currentTimeMillis();
        if (cachedTemplates == null || (now - cacheTimestamp) > CACHE_TTL_MS) {
            cachedTemplates = templateRepository.findByIsActiveTrueOrderByPriorityDesc();
            cacheTimestamp = now;
            log.debug("Refreshed query template cache, loaded {} templates", cachedTemplates.size());
        }
        return cachedTemplates;
    }

    private List<String> parseJsonArray(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse JSON array: {}", json, e);
            return Collections.emptyList();
        }
    }
}
