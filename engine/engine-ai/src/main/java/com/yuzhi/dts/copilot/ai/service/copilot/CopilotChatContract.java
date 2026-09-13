package com.yuzhi.dts.copilot.ai.service.copilot;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yuzhi.dts.copilot.ai.domain.AiChatMessage;
import com.yuzhi.dts.copilot.ai.service.copilot.ConversationPlannerService.ConversationPlan;
import com.yuzhi.dts.copilot.ai.service.copilot.ConversationPlannerService.ResponseKind;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public final class CopilotChatContract {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern SENSITIVE_COMMENT_PATTERN = Pattern.compile(
            "/\\*[^*]*(?:\\*(?!/)[^*]*)*\\*/|--[^\\n]*",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern JDBC_PATTERN = Pattern.compile("(?i)jdbc:[^\\s,;)]*");
    private static final Pattern SECRET_ASSIGNMENT_PATTERN = Pattern.compile(
            "(?i)\\b(password|passwd|token|secret|authorization|access_key)\\s*=\\s*('([^']*)'|\"([^\"]*)\"|[^\\s,;)]*)");

    private CopilotChatContract() {
    }

    public static void applyToMessage(AiChatMessage message, ConversationPlan plan, String generatedSql) {
        applyToMessage(message, plan, generatedSql, CopilotChatRequestContext.empty());
    }

    public static void applyToMessage(
            AiChatMessage message,
            ConversationPlan plan,
            String generatedSql,
            CopilotChatRequestContext requestContext) {
        applyToMessage(message, plan, generatedSql, requestContext, null);
    }

    public static void applyToMessage(
            AiChatMessage message,
            ConversationPlan plan,
            String generatedSql,
            CopilotChatRequestContext requestContext,
            FinanceAnswerAuditTrailService.AuditTrailReport financeAuditTrail) {
        if (message == null) {
            return;
        }
        message.setAssumptions(writeJson(buildAssumptions(plan, requestContext)));
        message.setConfidence(resolveConfidence(plan));
        message.setClarifications(writeJson(buildClarifications(plan, requestContext)));
        message.setTrace(writeJson(buildTrace(plan, generatedSql, requestContext, financeAuditTrail)));
    }

    public static void attachFinanceAuditTrail(
            AiChatMessage message,
            FinanceAnswerAuditTrailService.AuditTrailReport financeAuditTrail) {
        if (message == null || financeAuditTrail == null) {
            return;
        }
        try {
            ObjectNode trace = StringUtils.hasText(message.getTrace())
                    ? (ObjectNode) MAPPER.readTree(message.getTrace())
                    : MAPPER.createObjectNode();
            trace.set("financeAudit", MAPPER.valueToTree(financeAuditTrail));
            message.setTrace(writeJson(trace));
        } catch (Exception ignored) {
            // Keep the answer visible even if optional finance audit serialization fails.
        }
    }

    public static void putMessageFields(AiChatMessage message, Map<String, Object> target) {
        if (message == null || target == null) {
            return;
        }
        putStringField(target, "generatedSql", message.getGeneratedSql());
        putStringField(target, "responseKind", message.getResponseKind());
        putStringField(target, "routedDomain", message.getRoutedDomain());
        putStringField(target, "targetView", message.getTargetView());
        putStringField(target, "templateCode", message.getTemplateCode());
        putStringField(target, "dataSurface", message.getDataSurface());
        putStringField(target, "qualityLevel", message.getQualityLevel());
        putStringField(target, "qualityNotes", message.getQualityNotes());
        putStringField(target, "suggestedDisplay", message.getSuggestedDisplay());
        putStringField(target, "reportCode", message.getReportCode());
        putStringField(target, "sourceRefs", message.getSourceRefs());
        putJsonField(target, "assumptions", message.getAssumptions());
        if (message.getConfidence() != null) {
            target.put("confidence", message.getConfidence());
        }
        putJsonField(target, "clarifications", message.getClarifications());
        putAccuracyEvidenceField(target, message.getTrace());
        putJsonField(target, "trace", message.getTrace());
    }

    private static void putStringField(Map<String, Object> target, String key, String value) {
        if (StringUtils.hasText(value)) {
            target.put(key, value);
        }
    }

    public static void putDoneFields(ObjectNode done, ConversationPlan plan, String generatedSql) {
        putDoneFields(done, plan, generatedSql, CopilotChatRequestContext.empty());
    }

    public static void putDoneFields(
            ObjectNode done,
            ConversationPlan plan,
            String generatedSql,
            CopilotChatRequestContext requestContext) {
        putDoneFields(done, plan, generatedSql, requestContext, null);
    }

    public static void putDoneFields(
            ObjectNode done,
            ConversationPlan plan,
            String generatedSql,
            CopilotChatRequestContext requestContext,
            FinanceAnswerAuditTrailService.AuditTrailReport financeAuditTrail) {
        if (done == null) {
            return;
        }
        List<Map<String, Object>> assumptions = buildAssumptions(plan, requestContext);
        if (!assumptions.isEmpty()) {
            done.set("assumptions", MAPPER.valueToTree(assumptions));
        }
        Double confidence = resolveConfidence(plan);
        if (confidence != null) {
            done.put("confidence", confidence);
        }
        List<Map<String, Object>> clarifications = buildClarifications(plan, requestContext);
        if (!clarifications.isEmpty()) {
            done.set("clarifications", MAPPER.valueToTree(clarifications));
        }
        Map<String, Object> accuracyEvidence = buildAccuracyEvidence(plan, generatedSql, requestContext, financeAuditTrail);
        done.set("accuracyEvidence", MAPPER.valueToTree(accuracyEvidence));
        Map<String, Object> trace = buildTrace(plan, generatedSql, requestContext, financeAuditTrail);
        if (!trace.isEmpty()) {
            done.set("trace", MAPPER.valueToTree(trace));
        }
    }

    private static List<Map<String, Object>> buildAssumptions(ConversationPlan plan) {
        return buildAssumptions(plan, CopilotChatRequestContext.empty());
    }

    private static List<Map<String, Object>> buildAssumptions(
            ConversationPlan plan,
            CopilotChatRequestContext requestContext) {
        List<Map<String, Object>> assumptions = new ArrayList<>();
        if (plan != null && plan.responseKind() == ResponseKind.PUBLISHED_INDICATOR && plan.metricCaliber() != null) {
            assumptions.add(metricAssumption(plan));
        }
        if (plan != null && StringUtils.hasText(plan.dataSurface())) {
            assumptions.add(assumption("dataSurface", "数据层", plan.dataSurface(), false, "planner"));
        }
        if (plan != null && StringUtils.hasText(plan.qualityLevel())) {
            assumptions.add(assumption("qualityLevel", "可信度", plan.qualityLevel(), false, "planner"));
        }
        if (plan != null && StringUtils.hasText(plan.routedDomain())) {
            assumptions.add(assumption("domain", "业务域", plan.routedDomain(), false, "planner"));
        }
        if (requestContext != null) {
            requestContext.assumptionOverrides().forEach((key, value) -> {
                if (StringUtils.hasText(key) && StringUtils.hasText(value)) {
                    assumptions.add(assumption(key, key, value, true, "user_override"));
                }
            });
            requestContext.clarificationAnswers().forEach((key, value) -> {
                if (StringUtils.hasText(key) && StringUtils.hasText(value)) {
                    assumptions.add(assumption("clarification." + key, key, value, true, "user_clarification"));
                }
            });
        }
        return assumptions;
    }

    private static Map<String, Object> metricAssumption(ConversationPlan plan) {
        Map<String, Object> assumption = assumption(
                "metric",
                "指标",
                plan.metricCaliber().name(),
                true,
                "platform_indicator");
        List<Map<String, Object>> options = new ArrayList<>();
        plan.secondaryTargets().stream()
                .filter(StringUtils::hasText)
                .distinct()
                .forEach(name -> {
                    Map<String, Object> option = new LinkedHashMap<>();
                    option.put("value", name);
                    option.put("label", name);
                    options.add(option);
                });
        Map<String, Object> fallback = new LinkedHashMap<>();
        fallback.put("value", "__fallback_generated__");
        fallback.put("label", "退回 AI 现生成");
        options.add(fallback);
        assumption.put("options", options);
        if (StringUtils.hasText(plan.metricCaliber().formula())) {
            assumption.put("sourceHint", "平台指标口径: " + plan.metricCaliber().formula());
        }
        return assumption;
    }

    private static Map<String, Object> assumption(
            String key,
            String label,
            String value,
            boolean editable,
            String sourceHint) {
        Map<String, Object> assumption = new LinkedHashMap<>();
        assumption.put("key", key);
        assumption.put("label", label);
        assumption.put("value", value);
        assumption.put("editable", editable);
        assumption.put("sourceHint", sourceHint);
        return assumption;
    }

    private static Double resolveConfidence(ConversationPlan plan) {
        if (plan == null) {
            return null;
        }
        String qualityLevel = String.valueOf(plan.qualityLevel()).trim().toUpperCase();
        return switch (qualityLevel) {
            case "HIGH" -> 0.86d;
            case "MEDIUM" -> 0.72d;
            case "LOW" -> 0.45d;
            default -> plan.responseKind() == ResponseKind.BUSINESS_CLARIFICATION ? 0.42d : 0.64d;
        };
    }

    private static List<Map<String, Object>> buildClarifications(ConversationPlan plan) {
        return buildClarifications(plan, CopilotChatRequestContext.empty());
    }

    private static List<Map<String, Object>> buildClarifications(
            ConversationPlan plan,
            CopilotChatRequestContext requestContext) {
        if (requestContext != null && requestContext.hasClarificationAnswers()) {
            return List.of();
        }
        if (plan == null || plan.responseKind() != ResponseKind.BUSINESS_CLARIFICATION
                || plan.secondaryTargets().isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> options = plan.secondaryTargets().stream()
                .filter(StringUtils::hasText)
                .map(target -> {
                    Map<String, Object> option = new LinkedHashMap<>();
                    option.put("value", target);
                    option.put("label", target);
                    return option;
                })
                .toList();
        if (options.isEmpty()) {
            return List.of();
        }
        Map<String, Object> clarification = new LinkedHashMap<>();
        clarification.put("key", "target");
        clarification.put("question", StringUtils.hasText(plan.directResponse())
                ? plan.directResponse()
                : "请确认本次要分析的业务对象。");
        clarification.put("options", options);
        return List.of(clarification);
    }

    private static Map<String, Object> buildTrace(ConversationPlan plan, String generatedSql) {
        return buildTrace(plan, generatedSql, CopilotChatRequestContext.empty(), null);
    }

    private static Map<String, Object> buildTrace(
            ConversationPlan plan,
            String generatedSql,
            CopilotChatRequestContext requestContext,
            FinanceAnswerAuditTrailService.AuditTrailReport financeAuditTrail) {
        Map<String, Object> trace = new LinkedHashMap<>();
        if (plan == null) {
            if (StringUtils.hasText(generatedSql)) {
                trace.put("sql", generatedSql);
            }
            trace.put("accuracyEvidence", buildAccuracyEvidence(null, generatedSql, requestContext, financeAuditTrail));
            return trace;
        }
        Map<String, Object> caliber = new LinkedHashMap<>();
        if (plan.metricCaliber() != null) {
            if (StringUtils.hasText(plan.metricCaliber().name())) {
                caliber.put("name", plan.metricCaliber().name());
            }
            if (StringUtils.hasText(plan.metricCaliber().formula())) {
                caliber.put("formula", plan.metricCaliber().formula());
            }
            if (StringUtils.hasText(plan.metricCaliber().domain())) {
                caliber.put("domain", plan.metricCaliber().domain());
            }
            if (StringUtils.hasText(plan.metricCaliber().version())) {
                caliber.put("version", plan.metricCaliber().version());
            }
            if (StringUtils.hasText(plan.metricCaliber().ontologyRef())) {
                caliber.put("ontologyRef", plan.metricCaliber().ontologyRef());
            }
        } else if (StringUtils.hasText(plan.reportCode())) {
            caliber.put("name", plan.reportCode());
            caliber.put("ontologyRef", plan.reportCode());
        } else if (StringUtils.hasText(plan.primaryTarget())) {
            caliber.put("name", plan.primaryTarget());
        }
        if (plan.metricCaliber() == null && StringUtils.hasText(plan.routedDomain())) {
            caliber.put("domain", plan.routedDomain());
        }
        if (plan.metricCaliber() == null && StringUtils.hasText(plan.dataSurface())) {
            caliber.put("version", plan.dataSurface());
        }
        if (!caliber.isEmpty()) {
            trace.put("metricCaliber", caliber);
        }

        List<Map<String, Object>> sources = buildTraceSources(plan);
        if (!sources.isEmpty()) {
            trace.put("sources", sources);
        }

        List<Map<String, Object>> routeTrace = buildRouteTrace(plan);
        if (!routeTrace.isEmpty()) {
            trace.put("routeTrace", routeTrace);
        }

        String sql = StringUtils.hasText(generatedSql) ? generatedSql : plan.resolvedSql();
        if (StringUtils.hasText(sql)) {
            trace.put("sql", sql);
        }
        if (financeAuditTrail != null) {
            trace.put("financeAudit", MAPPER.valueToTree(financeAuditTrail));
        }
        trace.put("accuracyEvidence", buildAccuracyEvidence(plan, generatedSql, requestContext, financeAuditTrail));
        return trace;
    }

    private static Map<String, Object> buildAccuracyEvidence(
            ConversationPlan plan,
            String generatedSql,
            CopilotChatRequestContext requestContext,
            FinanceAnswerAuditTrailService.AuditTrailReport financeAuditTrail) {
        String sql = StringUtils.hasText(generatedSql) ? generatedSql : plan == null ? null : plan.resolvedSql();
        String safeSql = sanitizeSql(sql);
        List<String> targetRelations = targetRelations(plan, sql);
        String freshness = resolveFreshness(requestContext, targetRelations);
        List<String> reasons = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> staticChecks = new ArrayList<>();

        boolean hasSql = StringUtils.hasText(sql);
        boolean readOnly = !hasSql || isReadOnlySql(sql);
        boolean l0Profile = plan != null && "L0_BUSINESS_OBJECT_PROFILE".equalsIgnoreCase(text(plan.dataSurface()));
        boolean directApplicationMysql = isDirectApplicationMysql(plan, sql, targetRelations);
        boolean warehouseSurface = isWarehouseSurface(plan, targetRelations);
        boolean templateHit = plan != null && StringUtils.hasText(plan.templateCode());
        boolean auditPassed = isAuditPassed(financeAuditTrail);
        boolean auditFailed = financeAuditTrail != null && !auditPassed;

        if (hasSql && readOnly) {
            staticChecks.add("READ_ONLY");
        }
        if (warehouseSurface && !directApplicationMysql && !l0Profile) {
            staticChecks.add("ALLOWED_RELATION");
        }
        if (templateHit) {
            staticChecks.add("PARAM_BOUND");
        }
        if (auditPassed) {
            staticChecks.add("FINANCE_AUDIT_PASS");
        }

        String grade;
        double score;
        if (hasSql && !readOnly) {
            grade = "UNTRUSTED";
            score = 0.18d;
            reasons.add("SQL 静态检查未通过，非只读查询不能采信");
        } else if ("MISSING".equals(freshness)) {
            grade = "UNTRUSTED";
            score = 0.30d;
            reasons.add("目标 ADS/DWS 未入湖或未构建");
            warnings.add("ODS/dbt 新鲜度为 MISSING，数据未入湖或未构建");
        } else if (l0Profile) {
            grade = "UNTRUSTED";
            score = 0.28d;
            reasons.add("数据层 L0_BUSINESS_OBJECT_PROFILE 仅用于字段画像");
            warnings.add("L0 profile 不能作为统计结论");
        } else if (directApplicationMysql) {
            grade = "UNTRUSTED";
            score = 0.32d;
            reasons.add("命中应用 MySQL 直连路径");
            warnings.add("应用 MySQL 直连只能作为只读证明基准，不能作为仓库统计结论");
        } else if (auditFailed) {
            grade = "UNTRUSTED";
            score = 0.35d;
            reasons.add("财务对账未通过");
            warnings.add(firstAuditFailure(financeAuditTrail));
        } else if (warehouseSurface && auditPassed) {
            if ("STALE".equals(freshness)) {
                grade = "MEDIUM";
                score = 0.74d;
                reasons.add(templateHit ? "命中受控模板 " + plan.templateCode() : "命中受控财务路径");
                reasons.add("目标数据层为 ADS/DWS");
                reasons.add("财务对账通过");
                warnings.add("ODS/dbt 新鲜度已过期，最高 MEDIUM");
            } else {
                grade = "HIGH";
                score = 0.92d;
                reasons.add(templateHit ? "命中受控模板 " + plan.templateCode() : "命中受控财务路径");
                reasons.add("目标数据层为 ADS/DWS");
                reasons.add("财务对账通过");
            }
        } else if (warehouseSurface) {
            grade = "MEDIUM";
            score = 0.78d;
            reasons.add(templateHit ? "命中受控模板 " + plan.templateCode() : "命中治理数据层");
            reasons.add("目标数据层为 ADS/DWS");
            if (!staticChecks.isEmpty()) {
                reasons.add("SQL 静态检查通过");
            }
            warnings.add("缺少对账证据，最高 MEDIUM");
        } else {
            grade = "LOW";
            score = 0.48d;
            reasons.add("仅有路由或 schema 级证据");
            warnings.add("缺少 ADS/DWS 或对账证据，不能作为高可信统计结论");
        }

        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("grade", grade);
        evidence.put("score", score);
        evidence.put("reasons", reasons);
        evidence.put("warnings", warnings);
        evidence.put("intent", buildAccuracyIntent(plan));
        evidence.put("route", buildAccuracyRoute(plan, targetRelations));
        evidence.put("sql", buildAccuracySql(safeSql, staticChecks));
        evidence.put("data", buildAccuracyData(freshness, financeAuditTrail));
        evidence.put("tieout", buildAccuracyTieout(financeAuditTrail));
        return evidence;
    }

    private static Map<String, Object> buildAccuracyIntent(ConversationPlan plan) {
        Map<String, Object> intent = new LinkedHashMap<>();
        putIfPresent(intent, "domain", plan == null ? null : plan.routedDomain());
        putIfPresent(intent, "intentType", plan == null || plan.responseKind() == null ? null : plan.responseKind().name());
        intent.put("params", Map.of());
        String matchedBy = plan != null && StringUtils.hasText(plan.templateCode())
                ? "TEMPLATE"
                : plan != null && plan.metricCaliber() != null ? "METRIC" : "PLANNER";
        intent.put("matchedBy", matchedBy);
        return intent;
    }

    private static Map<String, Object> buildAccuracyRoute(ConversationPlan plan, List<String> targetRelations) {
        Map<String, Object> route = new LinkedHashMap<>();
        ConversationPlan.RouteStep routeStep = plan == null || plan.routeTrace().isEmpty() ? null : plan.routeTrace().get(0);
        putIfPresent(route, "routeTier", routeStep == null ? null : routeStep.tier());
        putIfPresent(route, "templateCode", plan == null ? null : plan.templateCode());
        putIfPresent(route, "metricCode", plan == null || plan.metricCaliber() == null ? null : plan.metricCaliber().ontologyRef());
        putIfPresent(route, "dataSurface", plan == null ? null : plan.dataSurface());
        route.put("targetRelations", targetRelations);
        return route;
    }

    private static Map<String, Object> buildAccuracySql(String safeSql, List<String> staticChecks) {
        Map<String, Object> sql = new LinkedHashMap<>();
        putIfPresent(sql, "sqlHash", StringUtils.hasText(safeSql) ? "sha256:" + sha256(safeSql) : null);
        putIfPresent(sql, "safeSql", safeSql);
        sql.put("staticChecks", staticChecks);
        return sql;
    }

    private static Map<String, Object> buildAccuracyData(
            String freshness,
            FinanceAnswerAuditTrailService.AuditTrailReport financeAuditTrail) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("freshness", StringUtils.hasText(freshness) ? freshness : "UNKNOWN");
        List<String> lineage = financeAuditTrail == null
                ? List.of()
                : financeAuditTrail.lineage().stream()
                        .map(node -> node.level() + ":" + node.name())
                        .filter(StringUtils::hasText)
                        .toList();
        data.put("lineage", lineage);
        return data;
    }

    private static String resolveFreshness(
            CopilotChatRequestContext requestContext,
            List<String> targetRelations) {
        if (requestContext == null || targetRelations == null || targetRelations.isEmpty()) {
            return "UNKNOWN";
        }
        for (String relation : targetRelations) {
            String status = lookupFreshness(requestContext.freshnessSnapshot(), relation);
            if (StringUtils.hasText(status)) {
                return status;
            }
        }
        for (String relation : targetRelations) {
            Boolean healthy = lookupHealth(requestContext.martHealthSnapshot(), relation);
            if (healthy != null) {
                return healthy ? "FRESH" : "MISSING";
            }
        }
        return "UNKNOWN";
    }

    private static String lookupFreshness(Map<String, String> snapshot, String relation) {
        if (snapshot == null || snapshot.isEmpty() || !StringUtils.hasText(relation)) {
            return null;
        }
        String normalizedRelation = normalizeRelationKey(relation);
        for (Map.Entry<String, String> entry : snapshot.entrySet()) {
            if (matchesRelationKey(normalizedRelation, entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static Boolean lookupHealth(Map<String, Boolean> snapshot, String relation) {
        if (snapshot == null || snapshot.isEmpty() || !StringUtils.hasText(relation)) {
            return null;
        }
        String normalizedRelation = normalizeRelationKey(relation);
        for (Map.Entry<String, Boolean> entry : snapshot.entrySet()) {
            if (matchesRelationKey(normalizedRelation, entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static boolean matchesRelationKey(String normalizedRelation, String candidate) {
        String normalizedCandidate = normalizeRelationKey(candidate);
        if (!StringUtils.hasText(normalizedCandidate)) {
            return false;
        }
        return normalizedRelation.equals(normalizedCandidate)
                || simpleRelationName(normalizedRelation).equals(normalizedCandidate)
                || normalizedRelation.equals(simpleRelationName(normalizedCandidate));
    }

    private static String normalizeRelationKey(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String simpleRelationName(String value) {
        int dot = value == null ? -1 : value.lastIndexOf('.');
        return dot >= 0 ? value.substring(dot + 1) : text(value);
    }

    private static Map<String, Object> buildAccuracyTieout(
            FinanceAnswerAuditTrailService.AuditTrailReport financeAuditTrail) {
        Map<String, Object> tieout = new LinkedHashMap<>();
        if (financeAuditTrail == null) {
            tieout.put("status", "MISSING");
            tieout.put("checks", List.of());
            return tieout;
        }
        String status = isAuditPassed(financeAuditTrail) ? "PASS" : "FAIL";
        tieout.put("status", status);
        FinanceAnswerAuditTrailService.OracleAuditStatus oracle = financeAuditTrail.oracleStatus();
        Map<String, Object> check = new LinkedHashMap<>();
        check.put("name", StringUtils.hasText(oracle.bindingId()) ? oracle.bindingId() : "finance_oracle");
        check.put("expected", "PASS");
        check.put("actual", oracle.healthStatus());
        BigDecimal diff = oracle.maxDifference();
        check.put("diff", diff == null ? BigDecimal.ZERO : diff);
        tieout.put("checks", List.of(check));
        return tieout;
    }

    private static boolean isAuditPassed(FinanceAnswerAuditTrailService.AuditTrailReport financeAuditTrail) {
        if (financeAuditTrail == null || !financeAuditTrail.passed()) {
            return false;
        }
        FinanceAnswerAuditTrailService.OracleAuditStatus status = financeAuditTrail.oracleStatus();
        return status.covered() && "PASS".equalsIgnoreCase(status.healthStatus());
    }

    private static String firstAuditFailure(FinanceAnswerAuditTrailService.AuditTrailReport financeAuditTrail) {
        if (financeAuditTrail == null || !StringUtils.hasText(financeAuditTrail.failureMessage())) {
            return "财务审计证据缺失或未通过";
        }
        return financeAuditTrail.failureMessage();
    }

    private static boolean isWarehouseSurface(ConversationPlan plan, List<String> targetRelations) {
        String surface = plan == null ? "" : text(plan.dataSurface()).toUpperCase(Locale.ROOT);
        if (surface.contains("ADS") || surface.contains("DWS") || surface.contains("DBT_MART")
                || surface.contains("PUBLISHED_INDICATOR")) {
            return true;
        }
        return targetRelations.stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(value -> value.contains("xycyl_ads_") || value.contains("xycyl_dws_"));
    }

    private static boolean isDirectApplicationMysql(
            ConversationPlan plan,
            String sql,
            List<String> targetRelations) {
        StringBuilder text = new StringBuilder();
        if (plan != null) {
            text.append(' ').append(text(plan.dataSurface()));
            text.append(' ').append(text(plan.primaryTarget()));
            text.append(' ').append(text(plan.martTable()));
            plan.sourceRefs().forEach(ref -> text.append(' ').append(ref));
        }
        text.append(' ').append(text(sql));
        targetRelations.forEach(value -> text.append(' ').append(value));
        String normalized = text.toString().toLowerCase(Locale.ROOT);
        return normalized.contains("mysql.rs_cloud_flower.")
                || normalized.contains("jdbc:mysql:")
                || normalized.contains("application-mysql")
                || normalized.contains("应用 mysql")
                || normalized.contains("应用mysql");
    }

    private static boolean isReadOnlySql(String sql) {
        if (!StringUtils.hasText(sql)) {
            return true;
        }
        String normalized = stripLeadingSqlComments(sql).toLowerCase(Locale.ROOT);
        return (normalized.startsWith("select ") || normalized.startsWith("with "))
                && !normalized.matches("(?s).*;\\s*\\S+.*")
                && !normalized.matches("(?s).*\\b(insert|update|delete|drop|alter|truncate|create|grant|revoke|merge|call|execute)\\b.*");
    }

    private static String stripLeadingSqlComments(String sql) {
        String trimmed = sql == null ? "" : sql.trim();
        boolean changed = true;
        while (changed && StringUtils.hasText(trimmed)) {
            changed = false;
            if (trimmed.startsWith("--")) {
                int newline = trimmed.indexOf('\n');
                trimmed = newline >= 0 ? trimmed.substring(newline + 1).trim() : "";
                changed = true;
            } else if (trimmed.startsWith("/*")) {
                int end = trimmed.indexOf("*/", 2);
                trimmed = end >= 0 ? trimmed.substring(end + 2).trim() : "";
                changed = true;
            }
        }
        return trimmed;
    }

    private static List<String> targetRelations(ConversationPlan plan, String generatedSql) {
        LinkedHashSet<String> relations = new LinkedHashSet<>();
        if (plan != null) {
            if (StringUtils.hasText(plan.martTable())) {
                relations.add(plan.martTable());
            }
            if (StringUtils.hasText(plan.primaryTarget()) && looksLikeRelation(plan.primaryTarget())) {
                relations.add(plan.primaryTarget());
            }
            plan.sourceRefs().stream()
                    .map(CopilotChatContract::extractSourceTable)
                    .filter(CopilotChatContract::looksLikeRelation)
                    .forEach(relations::add);
        }
        extractSqlRelations(generatedSql).forEach(relations::add);
        return List.copyOf(relations);
    }

    private static boolean looksLikeRelation(String value) {
        return StringUtils.hasText(value) && (value.contains(".") || value.startsWith("xycyl_"));
    }

    private static List<String> extractSqlRelations(String sql) {
        if (!StringUtils.hasText(sql)) {
            return List.of();
        }
        List<String> relations = new ArrayList<>();
        java.util.regex.Matcher matcher = Pattern.compile("(?i)\\b(?:from|join)\\s+([a-zA-Z0-9_\\.]+)")
                .matcher(sql);
        while (matcher.find()) {
            relations.add(matcher.group(1));
        }
        return relations;
    }

    private static String sanitizeSql(String sql) {
        if (!StringUtils.hasText(sql)) {
            return "";
        }
        String withoutSensitiveComments = SENSITIVE_COMMENT_PATTERN.matcher(sql)
                .replaceAll(match -> containsSensitiveMarker(match.group()) ? " " : match.group());
        String withoutJdbc = JDBC_PATTERN.matcher(withoutSensitiveComments).replaceAll("[redacted-jdbc-url]");
        return SECRET_ASSIGNMENT_PATTERN.matcher(withoutJdbc)
                .replaceAll("$1=[redacted]")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static boolean containsSensitiveMarker(String value) {
        String lower = value == null ? "" : value.toLowerCase(Locale.ROOT);
        return lower.contains("jdbc:")
                || lower.contains("password")
                || lower.contains("passwd")
                || lower.contains("token")
                || lower.contains("secret")
                || lower.contains("authorization")
                || lower.contains("access_key");
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }

    private static String text(String value) {
        return value == null ? "" : value;
    }

    private static List<Map<String, Object>> buildTraceSources(ConversationPlan plan) {
        LinkedHashSet<String> tables = new LinkedHashSet<>();
        if (StringUtils.hasText(plan.martTable())) {
            tables.add(plan.martTable());
        }
        if (StringUtils.hasText(plan.primaryTarget()) && plan.primaryTarget().contains(".")) {
            tables.add(plan.primaryTarget());
        }
        plan.sourceRefs().stream()
                .map(CopilotChatContract::extractSourceTable)
                .filter(StringUtils::hasText)
                .forEach(tables::add);

        List<Map<String, Object>> sources = new ArrayList<>();
        for (String table : tables) {
            Map<String, Object> source = new LinkedHashMap<>();
            source.put("table", table);
            source.put("role", "primary");
            sources.add(source);
        }
        return sources;
    }

    private static List<Map<String, Object>> buildRouteTrace(ConversationPlan plan) {
        if (plan == null || plan.routeTrace().isEmpty()) {
            return List.of();
        }
        return plan.routeTrace().stream()
                .map(step -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    putIfPresent(item, "tier", step.tier());
                    putIfPresent(item, "label", step.label());
                    putIfPresent(item, "status", step.status());
                    putIfPresent(item, "reason", step.reason());
                    putIfPresent(item, "target", step.target());
                    return item;
                })
                .filter(item -> !item.isEmpty())
                .toList();
    }

    private static String extractSourceTable(String sourceRef) {
        if (!StringUtils.hasText(sourceRef)) {
            return null;
        }
        String trimmed = sourceRef.trim();
        int separator = trimmed.indexOf(':');
        return separator >= 0 && separator < trimmed.length() - 1
                ? trimmed.substring(separator + 1)
                : trimmed;
    }

    private static void putIfPresent(Map<String, Object> target, String key, String value) {
        if (StringUtils.hasText(value)) {
            target.put(key, value);
        }
    }

    private static String writeJson(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof List<?> list && list.isEmpty()) {
            return null;
        }
        if (value instanceof Map<?, ?> map && map.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private static void putJsonField(Map<String, Object> target, String key, String rawJson) {
        if (!StringUtils.hasText(rawJson)) {
            return;
        }
        try {
            target.put(key, MAPPER.readValue(rawJson, Object.class));
        } catch (JsonProcessingException e) {
            target.put(key, rawJson);
        }
    }

    private static void putAccuracyEvidenceField(Map<String, Object> target, String rawTraceJson) {
        if (!StringUtils.hasText(rawTraceJson)) {
            return;
        }
        try {
            var trace = MAPPER.readTree(rawTraceJson);
            var evidence = trace.path("accuracyEvidence");
            if (!evidence.isMissingNode() && !evidence.isNull()) {
                target.put("accuracyEvidence", MAPPER.convertValue(evidence, Object.class));
            }
        } catch (Exception ignored) {
            // Trace parsing is best-effort for older persisted messages.
        }
    }
}
