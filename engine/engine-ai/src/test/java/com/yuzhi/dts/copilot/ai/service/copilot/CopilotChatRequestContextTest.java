package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CopilotChatRequestContextTest {

    @Test
    void filtersInvalidContractInputsWithoutThrowing() {
        Map<String, String> assumptionOverrides = new LinkedHashMap<>();
        assumptionOverrides.put("period", " 2026-05 ");
        assumptionOverrides.put("", "empty-key");
        assumptionOverrides.put("ignoredBlank", " ");
        assumptionOverrides.put("ignoredNull", null);
        assumptionOverrides.put(null, "null-key");

        Map<String, String> clarificationAnswers = new LinkedHashMap<>();
        clarificationAnswers.put("target", " 在租项目 ");
        clarificationAnswers.put("ignored", null);

        Map<String, Boolean> martHealth = new LinkedHashMap<>();
        martHealth.put("ads_project_monthly", true);
        martHealth.put("ignored", null);

        CopilotChatRequestContext context = CopilotChatRequestContext.of(
                martHealth,
                assumptionOverrides,
                clarificationAnswers);

        assertThat(context.martHealthSnapshot()).containsExactly(Map.entry("ads_project_monthly", true));
        assertThat(context.assumptionOverrides()).containsExactly(Map.entry("period", "2026-05"));
        assertThat(context.clarificationAnswers()).containsExactly(Map.entry("target", "在租项目"));
    }

    @Test
    void filtersAndNormalizesFreshnessSnapshot() {
        Map<String, String> freshness = new LinkedHashMap<>();
        freshness.put(" public.xycyl_ads_finance_voucher_monthly ", " fresh ");
        freshness.put("xycyl_dws_flowerbiz_project_monthly", "STALE");
        freshness.put("missing_model", "missing");
        freshness.put("ignored", "other");
        freshness.put("", "FRESH");
        freshness.put("blank", " ");
        freshness.put(null, "FRESH");

        CopilotChatRequestContext context = CopilotChatRequestContext.of(
                Map.of(),
                freshness,
                Map.of(),
                Map.of());

        assertThat(context.freshnessSnapshot()).containsExactly(
                Map.entry("public.xycyl_ads_finance_voucher_monthly", "FRESH"),
                Map.entry("xycyl_dws_flowerbiz_project_monthly", "STALE"),
                Map.entry("missing_model", "MISSING"));
    }
}
