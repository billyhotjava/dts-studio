package com.yuzhi.dts.copilot.ai.service.copilot;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yuzhi.dts.copilot.ai.domain.AiChatMessage;
import com.yuzhi.dts.copilot.ai.service.copilot.ConversationPlannerService.ConversationPlan;
import com.yuzhi.dts.copilot.ai.service.copilot.ConversationPlannerService.ResponseKind;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public final class CopilotChatContract {

    private static final ObjectMapper MAPPER = new ObjectMapper();

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
        if (message == null) {
            return;
        }
        message.setAssumptions(writeJson(buildAssumptions(plan, requestContext)));
        message.setConfidence(resolveConfidence(plan));
        message.setClarifications(writeJson(buildClarifications(plan, requestContext)));
        message.setTrace(writeJson(buildTrace(plan, generatedSql)));
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
        Map<String, Object> trace = buildTrace(plan, generatedSql);
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
        Map<String, Object> trace = new LinkedHashMap<>();
        if (plan == null) {
            if (StringUtils.hasText(generatedSql)) {
                trace.put("sql", generatedSql);
            }
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

        String sql = StringUtils.hasText(generatedSql) ? generatedSql : plan.resolvedSql();
        if (StringUtils.hasText(sql)) {
            trace.put("sql", sql);
        }
        return trace;
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
}
