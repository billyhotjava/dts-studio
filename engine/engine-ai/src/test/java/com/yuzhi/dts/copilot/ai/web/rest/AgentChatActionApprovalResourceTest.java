package com.yuzhi.dts.copilot.ai.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yuzhi.dts.copilot.ai.security.CopilotUserContext;
import com.yuzhi.dts.copilot.ai.security.CopilotUserContextHolder;
import com.yuzhi.dts.copilot.ai.service.chat.AgentChatService;
import com.yuzhi.dts.copilot.ai.service.chat.RouteTelemetryService;
import com.yuzhi.dts.copilot.ai.service.copilot.OntologyActionApprovalService;
import com.yuzhi.dts.copilot.ai.service.copilot.OntologyActionExecutor;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.ResponseEntity;

class AgentChatActionApprovalResourceTest {

    private final AgentChatService agentChatService = mock(AgentChatService.class);
    private final RouteTelemetryService routeTelemetryService = mock(RouteTelemetryService.class);
    private final OntologyActionApprovalService approvalService = mock(OntologyActionApprovalService.class);

    @AfterEach
    void clearUserContext() {
        CopilotUserContextHolder.clear();
    }

    @Test
    void approveActionDelegatesToApprovalServiceWithCurrentUserContext() {
        OntologyActionApprovalService.ActionApprovalCard card = new OntologyActionApprovalService.ActionApprovalCard(
                "flowerbiz:创建坏账处理单",
                "ontology.action.createDraft",
                Map.of("projectId", 101),
                "确认创建草稿",
                "创建坏账处理单 / 租赁报花明细",
                "只创建草稿",
                new OntologyActionApprovalService.MicroFormSchema(
                        "创建坏账处理单",
                        "确认后创建草稿",
                        "HIGH",
                        "需要人工确认",
                        List.of()));
        OntologyActionExecutor.ActionDraftResult draftResult = new OntologyActionExecutor.ActionDraftResult(
                true,
                "创建坏账处理单",
                "/rs-flowers-base/flower/bizBadDebt/saveDraftFlowerBadDebt",
                "/rs-flowers-base/flower/bizBadDebt/saveFlowerBadDebt",
                Map.of("projectId", 101, "draftItemJson", "[501]", "badDebtType", 1),
                "ok",
                Map.of("id", 9001));
        when(approvalService.requestDraft(any())).thenReturn(new OntologyActionApprovalService.ActionApprovalResult(
                true,
                false,
                "ok",
                card,
                draftResult));
        CopilotUserContextHolder.set(new CopilotUserContext(
                "alice",
                "alice",
                "Alice",
                List.of("flowerbiz:baddebt:draft"),
                "ops",
                "key-1"));
        AgentChatResource resource = new AgentChatResource(agentChatService, routeTelemetryService, approvalService);

        ResponseEntity<Map<String, Object>> response = resource.approveAction(new AgentChatResource.ApproveActionRequest(
                "sess-1",
                "flowerbiz:创建坏账处理单",
                Map.of("projectId", 101, "draftItemJson", "[501]", "badDebtType", 1)));

        ArgumentCaptor<OntologyActionApprovalService.ActionApprovalRequest> requestCaptor =
                ArgumentCaptor.forClass(OntologyActionApprovalService.ActionApprovalRequest.class);
        org.mockito.Mockito.verify(approvalService).requestDraft(requestCaptor.capture());
        OntologyActionApprovalService.ActionApprovalRequest serviceRequest = requestCaptor.getValue();
        assertThat(serviceRequest.domain()).isEqualTo("flowerbiz");
        assertThat(serviceRequest.actionName()).isEqualTo("创建坏账处理单");
        assertThat(serviceRequest.confirmed()).isTrue();
        assertThat(serviceRequest.sessionId()).isEqualTo("sess-1");
        assertThat(serviceRequest.userContext().userId()).isEqualTo("alice");
        assertThat(serviceRequest.objectAttributes())
                .containsEntry("projectId", 101)
                .containsEntry("draftItemJson", "[501]")
                .containsEntry("badDebtType", 1);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body)
                .containsEntry("sessionId", "sess-1")
                .containsEntry("agentMessage", "ok")
                .containsEntry("requiresApproval", false);
        assertThat(body.get("pendingAction")).isNull();
        assertThat((List<?>) body.get("toolCalls")).hasSize(1);
    }

    @Test
    void cancelActionReturnsChatCompatibleResponse() {
        AgentChatResource resource = new AgentChatResource(agentChatService, routeTelemetryService, approvalService);

        ResponseEntity<Map<String, Object>> response = resource.cancelAction(new AgentChatResource.CancelActionRequest(
                "sess-1",
                "flowerbiz:创建坏账处理单"));

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body)
                .containsEntry("sessionId", "sess-1")
                .containsEntry("requiresApproval", false);
        assertThat(String.valueOf(body.get("agentMessage"))).contains("已取消");
        assertThat((List<?>) body.get("toolCalls")).isEmpty();
        assertThat(body.get("pendingAction")).isNull();
    }
}
