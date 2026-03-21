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
import java.util.Comparator;
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

    private record FixedReportIntent(
            String templateCode,
            String domain,
            String targetObject,
            String description,
            List<String> questionSamples,
            List<String> patterns
    ) {}

    private static final List<FixedReportIntent> FIXED_REPORT_INTENTS = List.of(
            new FixedReportIntent(
                    "FIN-AR-OVERVIEW",
                    "财务",
                    "authority.finance.receivable_overview",
                    "财务结算汇总",
                    List.of("财务结算汇总", "应收总览", "应收已收未收总览"),
                    List.of(".*(财务结算汇总|应收.*(总览|概览|看板)|已收.*未收|未收.*已收).*")),
            new FixedReportIntent(
                    "FIN-CUSTOMER-AR-RANK",
                    "财务",
                    "mart.finance.customer_ar_rank_daily",
                    "财务结算汇总-客户欠款排行",
                    List.of("客户欠款排行", "客户应收排行", "财务结算汇总客户欠款排行"),
                    List.of(".*(财务结算汇总.*客户.*(欠款|应收)|客户.*(欠款|应收).*(排行|排名)).*")),
            new FixedReportIntent(
                    "FIN-PROJECT-COLLECTION-PROGRESS",
                    "财务",
                    "authority.finance.project_collection_progress",
                    "财务结算列表-项目回款进度",
                    List.of("项目回款进度", "项目回款完成率", "财务结算列表项目回款进度"),
                    List.of(".*(财务结算列表.*项目.*回款|项目.*回款.*(进度|完成率)).*")),
            new FixedReportIntent(
                    "FIN-PENDING-RECEIPTS-DETAIL",
                    "财务",
                    "authority.finance.pending_receipts_detail",
                    "财务结算列表-待收款明细",
                    List.of("待收款明细", "待收款清单", "财务结算列表待收款明细"),
                    List.of(".*(财务结算列表.*待收款|待收款.*(明细|清单|列表)).*")),
            new FixedReportIntent(
                    "PROC-PURCHASE-REQUEST-TODO",
                    "采购",
                    "authority.procurement.request_todo",
                    "采购计划明细-待处理",
                    List.of("采购申请待办", "待处理采购申请", "采购计划明细待处理"),
                    List.of(".*(采购计划明细.*待处理|采购申请.*(待办|待处理)).*")),
            new FixedReportIntent(
                    "PROC-SUPPLIER-AMOUNT-RANK",
                    "采购",
                    "fact.procurement.order_event",
                    "采购汇总",
                    List.of("采购汇总", "供应商采购金额排行"),
                    List.of(".*(采购汇总|供应商.*采购金额.*(排行|排名)).*")),
            new FixedReportIntent(
                    "PROC-ARRIVAL-ONTIME-RATE",
                    "采购",
                    "fact.procurement.order_event",
                    "配送记录-到货及时率",
                    List.of("采购到货及时率", "配送记录到货及时率"),
                    List.of(".*(配送记录.*到货.*及时率|采购.*到货.*及时率).*")),
            new FixedReportIntent(
                    "PROC-PENDING-INBOUND-LIST",
                    "采购",
                    "authority.procurement.pending_inbound_list",
                    "入库管理-待入库清单",
                    List.of("待入库采购清单", "入库管理待入库清单"),
                    List.of(".*(入库管理.*待入库|待入库.*采购.*(清单|列表)).*")),
            new FixedReportIntent(
                    "PROC-INTRANSIT-BOARD",
                    "采购",
                    "authority.procurement.intransit_board",
                    "配送记录-在途采购",
                    List.of("采购在途看板", "配送记录在途采购"),
                    List.of(".*(配送记录.*在途采购|采购.*在途.*(看板|情况)).*")),
            new FixedReportIntent(
                    "WH-STOCK-OVERVIEW",
                    "仓库",
                    "authority.inventory.stock_overview",
                    "库存现量",
                    List.of("库存现量", "库存现量看板", "当前库存总览"),
                    List.of(".*(库存现量|当前库存|库存总览|库存看板).*")),
            new FixedReportIntent(
                    "WH-LOW-STOCK-ALERT",
                    "仓库",
                    "authority.inventory.low_stock_alert",
                    "库存现量-低库存预警",
                    List.of("低库存预警", "低库存清单", "库存现量低库存预警"),
                    List.of(".*(库存现量.*低库存|低库存|缺货).*(预警|告警|清单|列表).*"))
    );

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

        TemplateMatchResult fixedReportMatch = matchFixedReportIntent(userQuestion);
        if (fixedReportMatch.matched()) {
            return fixedReportMatch;
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
        List<SuggestedQuestion> suggestions = new ArrayList<>(buildFixedReportSuggestedQuestions(limit));
        if (suggestions.size() >= limit) {
            return suggestions.subList(0, limit);
        }

        List<Nl2SqlQueryTemplate> templates = loadActiveTemplates();
        if (templates.isEmpty()) {
            return suggestions;
        }

        // Group templates by domain
        Map<String, List<Nl2SqlQueryTemplate>> byDomain = new LinkedHashMap<>();
        for (Nl2SqlQueryTemplate t : templates) {
            byDomain.computeIfAbsent(t.getDomain(), k -> new ArrayList<>()).add(t);
        }

        // Pick 1-2 questions from each domain, round-robin
        int perDomain = Math.max(1, limit / Math.max(1, byDomain.size()));
        List<String> existingQuestions = suggestions.stream()
                .map(SuggestedQuestion::question)
                .toList();

        for (Map.Entry<String, List<Nl2SqlQueryTemplate>> entry : byDomain.entrySet()) {
            int picked = 0;
            for (Nl2SqlQueryTemplate t : entry.getValue()) {
                if (picked >= perDomain) break;

                List<String> samples = parseJsonArray(t.getQuestionSamples());
                if (!samples.isEmpty()) {
                    SuggestedQuestion suggestion = new SuggestedQuestion(
                            t.getTemplateCode(),
                            t.getDomain(),
                            t.getRoleHint(),
                            samples.get(0),
                            t.getDescription()
                    );
                    if (existingQuestions.contains(suggestion.question())
                            || suggestions.stream().anyMatch(item -> item.templateCode().equals(suggestion.templateCode()))) {
                        continue;
                    }
                    suggestions.add(suggestion);
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

    public List<SuggestedQuestion> getFixedReportSuggestionsByDomain(String domain, int limit) {
        if (limit <= 0) {
            return Collections.emptyList();
        }
        String normalizedDomain = normalizeFixedReportDomain(domain);
        if (normalizedDomain.isBlank()) {
            return Collections.emptyList();
        }
        return FIXED_REPORT_INTENTS.stream()
                .filter(intent -> normalizeFixedReportDomain(intent.domain()).equals(normalizedDomain))
                .limit(limit)
                .map(intent -> new SuggestedQuestion(
                        intent.templateCode(),
                        intent.domain(),
                        intent.domain(),
                        buildPageAlignedFixedReportQuestion(intent),
                        intent.description()
                ))
                .toList();
    }

    private List<SuggestedQuestion> buildFixedReportSuggestedQuestions(int limit) {
        if (limit <= 0) {
            return Collections.emptyList();
        }
        Map<String, Integer> perDomainCount = new HashMap<>();
        List<SuggestedQuestion> suggestions = new ArrayList<>();
        for (FixedReportIntent intent : FIXED_REPORT_INTENTS) {
            if (suggestions.size() >= limit) {
                break;
            }
            int count = perDomainCount.getOrDefault(intent.domain(), 0);
            if (count >= 2 || intent.questionSamples().isEmpty()) {
                continue;
            }
            suggestions.add(new SuggestedQuestion(
                    intent.templateCode(),
                    intent.domain(),
                    intent.domain(),
                    intent.questionSamples().get(0),
                    intent.description()
            ));
            perDomainCount.put(intent.domain(), count + 1);
        }
        return suggestions;
    }

    private String buildPageAlignedFixedReportQuestion(FixedReportIntent intent) {
        if (intent == null) {
            return "";
        }
        String description = intent.description();
        if (description != null && !description.isBlank()) {
            return description.replace("-", "").trim();
        }
        if (intent.questionSamples() != null && !intent.questionSamples().isEmpty()) {
            return intent.questionSamples().get(0);
        }
        return intent.templateCode();
    }

    private String normalizeFixedReportDomain(String domain) {
        if (domain == null || domain.isBlank()) {
            return "";
        }
        return switch (domain.trim().toLowerCase()) {
            case "settlement", "finance", "financial", "finace", "财务" -> "财务";
            case "procurement", "purchase", "采购" -> "采购";
            case "warehouse", "inventory", "stock", "仓库", "库存" -> "仓库";
            default -> domain.trim();
        };
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
                case "good_name" -> value = extractProductName(question);
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

        // Check for explicit 中文年月 pattern, e.g. 2025年2月
        Matcher zhMonth = Pattern.compile("(\\d{4})年(\\d{1,2})月").matcher(question);
        if (zhMonth.find()) {
            int month = Integer.parseInt(zhMonth.group(2));
            return zhMonth.group(1) + "-" + String.format("%02d", month);
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

    private String extractProductName(String question) {
        if (question == null || question.isBlank()) {
            return null;
        }

        String normalized = question
                .replaceAll("\\d{4}年\\d{1,2}月", " ")
                .replaceAll("\\d{4}-\\d{2}", " ")
                .replace('，', ' ')
                .replace(',', ' ');

        Pattern aroundKeyword = Pattern.compile("([\\u4e00-\\u9fa5A-Za-z0-9]{1,20})(?:这个)?(产品|物品|商品)");
        Matcher aroundMatcher = aroundKeyword.matcher(normalized);
        if (aroundMatcher.find()) {
            String candidate = normalizeProductCandidate(aroundMatcher.group(1));
            if (candidate != null) {
                return candidate;
            }
        }

        Pattern beforePurchase = Pattern.compile("([\\u4e00-\\u9fa5A-Za-z0-9]{1,20})采购(明细|详细情况|情况)");
        Matcher purchaseMatcher = beforePurchase.matcher(normalized);
        if (purchaseMatcher.find()) {
            String candidate = normalizeProductCandidate(purchaseMatcher.group(1));
            if (candidate != null) {
                return candidate;
            }
        }

        return null;
    }

    private String normalizeProductCandidate(String candidate) {
        if (candidate == null) {
            return null;
        }
        String normalized = candidate
                .replaceAll("^(查询|查看|统计|帮我看下|帮我看看|帮我查询)", "")
                .replaceAll("(这个|该|的)$", "")
                .trim();
        if (normalized.isBlank() || isStopWord(normalized)) {
            return null;
        }
        return normalized;
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

    private TemplateMatchResult matchFixedReportIntent(String userQuestion) {
        for (FixedReportIntent intent : FIXED_REPORT_INTENTS) {
            for (String patternStr : intent.patterns()) {
                try {
                    Pattern pattern = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE);
                    if (pattern.matcher(userQuestion).find()) {
                        log.debug("Matched fixed report {} with pattern {}", intent.templateCode(), patternStr);
                        return new TemplateMatchResult(
                                true,
                                buildSyntheticFixedReportTemplate(intent),
                                Collections.emptyMap(),
                                null
                        );
                    }
                } catch (Exception e) {
                    log.warn("Invalid fixed report pattern '{}' in {}: {}",
                            patternStr, intent.templateCode(), e.getMessage());
                }
            }
        }
        return new TemplateMatchResult(false, null, null, null);
    }

    // ========== Cache and JSON helpers ==========

    private List<Nl2SqlQueryTemplate> loadActiveTemplates() {
        long now = System.currentTimeMillis();
        if (cachedTemplates == null || (now - cacheTimestamp) > CACHE_TTL_MS) {
            List<Nl2SqlQueryTemplate> loaded = new ArrayList<>(templateRepository.findByIsActiveTrueOrderByPriorityDesc());
            loaded.sort(Comparator.comparingInt(template -> template.getPriority() == null ? 0 : template.getPriority()).reversed());
            cachedTemplates = List.copyOf(loaded);
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

    private Nl2SqlQueryTemplate buildSyntheticFixedReportTemplate(FixedReportIntent intent) {
        Nl2SqlQueryTemplate template = new Nl2SqlQueryTemplate();
        template.setTemplateCode(intent.templateCode());
        template.setDomain(intent.domain());
        template.setRoleHint(null);
        template.setIntentPatterns("[]");
        try {
            template.setQuestionSamples(objectMapper.writeValueAsString(intent.questionSamples()));
        } catch (Exception ignored) {
            template.setQuestionSamples("[]");
        }
        template.setSqlTemplate("-- fixed report fast path --");
        template.setParameters("{}");
        template.setTargetView(intent.targetObject());
        template.setDescription(intent.description());
        template.setPriority(1000);
        template.setIsActive(true);
        return template;
    }
}
