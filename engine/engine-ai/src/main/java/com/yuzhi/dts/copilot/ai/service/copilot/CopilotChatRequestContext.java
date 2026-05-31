package com.yuzhi.dts.copilot.ai.service.copilot;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.util.StringUtils;

public record CopilotChatRequestContext(
        Map<String, Boolean> martHealthSnapshot,
        Map<String, String> assumptionOverrides,
        Map<String, String> clarificationAnswers) {

    private static final CopilotChatRequestContext EMPTY =
            new CopilotChatRequestContext(Map.of(), Map.of(), Map.of());

    public CopilotChatRequestContext {
        martHealthSnapshot = copyHealthOrEmpty(martHealthSnapshot);
        assumptionOverrides = copyTextOrEmpty(assumptionOverrides);
        clarificationAnswers = copyTextOrEmpty(clarificationAnswers);
    }

    public static CopilotChatRequestContext empty() {
        return EMPTY;
    }

    public static CopilotChatRequestContext of(
            Map<String, Boolean> martHealthSnapshot,
            Map<String, String> assumptionOverrides,
            Map<String, String> clarificationAnswers) {
        if ((martHealthSnapshot == null || martHealthSnapshot.isEmpty())
                && (assumptionOverrides == null || assumptionOverrides.isEmpty())
                && (clarificationAnswers == null || clarificationAnswers.isEmpty())) {
            return EMPTY;
        }
        return new CopilotChatRequestContext(martHealthSnapshot, assumptionOverrides, clarificationAnswers);
    }

    public boolean hasAssumptionOverrides() {
        return !assumptionOverrides.isEmpty();
    }

    public boolean hasClarificationAnswers() {
        return !clarificationAnswers.isEmpty();
    }

    private static Map<String, Boolean> copyHealthOrEmpty(Map<String, Boolean> value) {
        if (value == null || value.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Boolean> copy = new LinkedHashMap<>();
        value.forEach((key, healthy) -> {
            if (StringUtils.hasText(key) && healthy != null) {
                copy.put(key.trim(), healthy);
            }
        });
        return copy.isEmpty() ? Collections.emptyMap() : Collections.unmodifiableMap(copy);
    }

    private static Map<String, String> copyTextOrEmpty(Map<String, String> value) {
        if (value == null || value.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> copy = new LinkedHashMap<>();
        value.forEach((key, text) -> {
            if (StringUtils.hasText(key) && StringUtils.hasText(text)) {
                copy.put(key.trim(), text.trim());
            }
        });
        return copy.isEmpty() ? Collections.emptyMap() : Collections.unmodifiableMap(copy);
    }
}
