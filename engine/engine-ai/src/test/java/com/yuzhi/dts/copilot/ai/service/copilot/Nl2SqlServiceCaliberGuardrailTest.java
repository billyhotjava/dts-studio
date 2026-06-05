package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuzhi.dts.copilot.ai.service.llm.gateway.LlmGatewayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class Nl2SqlServiceCaliberGuardrailTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private LlmGatewayService llmGateway;
    private Nl2SqlService service;

    @BeforeEach
    void setUp() {
        CaliberRuleRegistry registry = new CaliberRuleRegistry(OBJECT_MAPPER);
        registry.init();
        llmGateway = mock(LlmGatewayService.class);
        service = new Nl2SqlService(llmGateway, new SqlSafetyChecker(registry), null);
    }

    @Test
    void shouldBlockFinanceCaliberViolationBeforeReturningGeneratedSql() throws Exception {
        when(llmGateway.chatCompletion(anyList(), any(), any(), any()))
                .thenReturn(llmResponse("""
                        SELECT SUM(m.receivable_total_amount) + SUM(s.receivable) AS amount
                        FROM a_month_accounting m
                        JOIN a_sale_account s ON s.project_id = m.project_id
                        """));

        assertThatThrownBy(() -> service.nl2sql("财务混链汇总", "", "", "finance"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CAL-SETTLEMENT-CHAIN")
                .hasMessageContaining("租摆链和售赠坏链混算风险");
    }

    private static com.fasterxml.jackson.databind.JsonNode llmResponse(String sql) throws Exception {
        return OBJECT_MAPPER.readTree("""
                {
                  "choices": [
                    {
                      "message": {
                        "content": %s
                      }
                    }
                  ]
                }
                """.formatted(OBJECT_MAPPER.writeValueAsString(sql)));
    }
}
