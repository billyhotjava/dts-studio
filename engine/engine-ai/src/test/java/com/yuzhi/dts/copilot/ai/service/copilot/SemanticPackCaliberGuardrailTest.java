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

    private static void assertPackHasRequiredGuardrails(SemanticPackService.SemanticPack pack) {
        assertThat(pack.guardrails()).hasSizeGreaterThanOrEqualTo(REQUIRED_RULE_IDS.size());
        for (String ruleId : REQUIRED_RULE_IDS) {
            assertThat(pack.guardrails())
                    .as("%s guardrails contain %s", pack.domain(), ruleId)
                    .anySatisfy(guardrail -> assertThat(guardrail).contains("[" + ruleId + "]"));
        }
    }
}
