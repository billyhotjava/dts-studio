package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class SemanticPackCaliberGuardrailTest {

    private static final List<String> REQUIRED_RULE_IDS = List.of(
            "CAL-BIZTYPE-SCOPE",
            "CAL-SETTLEMENT-CHAIN",
            "CAL-MONTH-AMOUNT-TIER",
            "CAL-SALE-IN-RENT",
            "CAL-RENT-HISTORY",
            "CAL-INVENTORY-COST",
            "CAL-JSON-EXPAND",
            "CAL-VARCHAR-AMOUNT-CAST",
            "CAL-EXTRA-COST-VS-EXPENSE");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldLoadSprint30CaliberGuardrailsForFlowerbizAndFinancePacks() {
        SemanticPackService service = new SemanticPackService(objectMapper);

        service.init();

        SemanticPackService.SemanticPack flowerbiz = service.getPack("flowerbiz").orElseThrow();
        SemanticPackService.SemanticPack finance = service.getPack("finance").orElseThrow();
        assertPackHasRequiredGuardrails(flowerbiz);
        assertPackHasRequiredGuardrails(finance);
    }

    @Test
    void shouldExposeCaliberGuardrailsInPromptContext() {
        SemanticPackService service = new SemanticPackService(objectMapper);

        service.init();

        String context = service.getContextForDomain("flowerbiz");
        assertThat(context)
                .contains("【口径护栏】")
                .contains("CAL-BIZTYPE-SCOPE")
                .contains("CAL-JSON-EXPAND")
                .contains("CAL-EXTRA-COST-VS-EXPENSE");
    }

    @Test
    void shouldLoadFinanceVerticalSliceObjectsAndFewShots() {
        SemanticPackService service = new SemanticPackService(objectMapper);

        service.init();

        SemanticPackService.SemanticPack finance = service.getPack("finance").orElseThrow();
        assertThat(finance.objects()).extracting(SemanticPackService.SemanticObject::view)
                .containsExactly(
                        "public.xycyl_ads_finance_month_settlement",
                        "public.xycyl_ads_finance_invoice_progress",
                        "public.xycyl_ads_finance_collection");
        assertThat(finance.fewShots()).extracting(SemanticPackService.FewShot::sql)
                .anySatisfy(sql -> assertThat(sql).contains("public.xycyl_ads_finance_month_settlement"))
                .anySatisfy(sql -> assertThat(sql).contains("public.xycyl_ads_finance_invoice_progress"))
                .anySatisfy(sql -> assertThat(sql).contains("public.xycyl_ads_finance_collection"));
    }

    @Test
    void shouldRejectSyntheticFinanceSummaryTableInPromptContext() {
        SemanticPackService service = new SemanticPackService(objectMapper);

        service.init();

        String context = service.getContextForDomain("finance");
        assertThat(context)
                .contains("禁止使用 public.xycyl_ads_finance_summary")
                .contains("public.xycyl_ads_finance_month_settlement")
                .contains("public.xycyl_ads_finance_collection");
    }

    private static void assertPackHasRequiredGuardrails(SemanticPackService.SemanticPack pack) {
        assertThat(pack.guardrails()).hasSizeGreaterThanOrEqualTo(REQUIRED_RULE_IDS.size());
        for (String ruleId : REQUIRED_RULE_IDS) {
            assertThat(pack.guardrails())
                    .as("%s guardrails contain %s", pack.domain(), ruleId)
                    .anySatisfy(guardrail -> assertThat(guardrail).contains("[" + ruleId + "]"));
        }
    }
}
