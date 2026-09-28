package com.yuzhi.dts.copilot.ai.service.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuzhi.dts.copilot.ai.domain.AiChatMessage;
import com.yuzhi.dts.copilot.ai.repository.AiChatMessageRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RouteTelemetryServiceTest {

    @Mock
    private AiChatMessageRepository messageRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void attachQuestionAddsFinalTierAndWeakPathFlagToTrace() {
        RouteTelemetryService service = new RouteTelemetryService(messageRepository, objectMapper);
        AiChatMessage message = assistantMessage(
                "flowerbiz",
                "BUSINESS_INSIGHT",
                "business-object:prs.warehouse.stock_info",
                """
                        {
                          "routeTrace": [
                            {"tier": "TIER_1_PUBLISHED_INDICATOR", "status": "MISS"},
                            {"tier": "TIER_2_MART_TEMPLATE", "status": "MISS"},
                            {"tier": "TIER_5_DIRECT_DETAIL", "status": "HIT", "target": "business-object:prs.warehouse.stock_info"}
                          ]
                        }
                        """);

        service.attachQuestion(message, "低库存预警");

        assertThat(message.getTrace())
                .contains("\"question\":\"低库存预警\"")
                .contains("\"finalTier\":\"TIER_5_DIRECT_DETAIL\"")
                .contains("\"finalTarget\":\"business-object:prs.warehouse.stock_info\"")
                .contains("\"weakPath\":true");
    }

    @Test
    void summarizeAggregatesTierCountsAndWeakPathMartCandidates() {
        RouteTelemetryService service = new RouteTelemetryService(messageRepository, objectMapper);
        AiChatMessage weak = assistantMessage(
                "warehouse",
                "BUSINESS_INSIGHT",
                "business-object:prs.warehouse.stock_info",
                """
                        {
                          "routeTrace": [
                            {"tier": "TIER_1_PUBLISHED_INDICATOR", "status": "MISS"},
                            {"tier": "TIER_2_MART_TEMPLATE", "status": "MISS"},
                            {"tier": "TIER_5_DIRECT_DETAIL", "status": "HIT", "target": "business-object:prs.warehouse.stock_info"}
                          ],
                          "telemetry": {
                            "question": "低库存预警",
                            "finalTier": "TIER_5_DIRECT_DETAIL",
                            "weakPath": true
                          }
                        }
                        """);
        AiChatMessage strong = assistantMessage(
                "flowerbiz",
                "FIXED_REPORT",
                "public.xycyl_ads_flowerbiz_lease_summary",
                """
                        {
                          "routeTrace": [
                            {"tier": "TIER_1_PUBLISHED_INDICATOR", "status": "MISS"},
                            {"tier": "TIER_2_MART_TEMPLATE", "status": "HIT", "target": "PRS-FLOWERBIZ-LEASE-EXECUTION"}
                          ],
                          "telemetry": {
                            "question": "租赁执行看板",
                            "finalTier": "TIER_2_MART_TEMPLATE",
                            "weakPath": false
                          }
                        }
                        """);
        when(messageRepository.findByRoleAndTraceIsNotNullAndCreatedAtAfterOrderByCreatedAtDesc(
                eq("assistant"), any(Instant.class)))
                .thenReturn(List.of(weak, strong));

        RouteTelemetryService.RouteTelemetrySummary summary = service.summarize(7, 5);

        assertThat(summary.tierCounts())
                .containsEntry("TIER_5_DIRECT_DETAIL", 1L)
                .containsEntry("TIER_2_MART_TEMPLATE", 1L);
        assertThat(summary.martCandidateSignals()).hasSize(1);
        RouteTelemetryService.MartCandidateSignal signal = summary.martCandidateSignals().getFirst();
        assertThat(signal.finalTier()).isEqualTo("TIER_5_DIRECT_DETAIL");
        assertThat(signal.domain()).isEqualTo("warehouse");
        assertThat(signal.target()).isEqualTo("business-object:prs.warehouse.stock_info");
        assertThat(signal.questionSamples()).containsExactly("低库存预警");
    }

    private static AiChatMessage assistantMessage(String domain, String responseKind, String target, String trace) {
        AiChatMessage message = new AiChatMessage();
        message.setRole("assistant");
        message.setRoutedDomain(domain);
        message.setResponseKind(responseKind);
        message.setTargetView(target);
        message.setTrace(trace);
        message.setCreatedAt(Instant.parse("2026-06-03T08:00:00Z"));
        return message;
    }
}
