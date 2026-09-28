package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yuzhi.dts.copilot.ai.security.CopilotUserContext;
import com.yuzhi.dts.copilot.ai.service.audit.AiAuditService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OntologyActionApprovalServiceTest {

    @Mock
    private SemanticPackService semanticPackService;

    @Mock
    private OntologyActionExecutor actionExecutor;

    @Mock
    private AiAuditService auditService;

    @Test
    void shouldBuildApprovalCardWithoutCallingDraftBeforeConfirmation() {
        when(semanticPackService.getPack("flowerbiz")).thenReturn(Optional.of(flowerbizActionPack()));
        OntologyActionApprovalService service = service();

        OntologyActionApprovalService.ActionApprovalResult result = service.requestDraft(
                new OntologyActionApprovalService.ActionApprovalRequest(
                        "flowerbiz",
                        "创建坏账处理单",
                        objectAttributes(),
                        false,
                        userWithGuard(),
                        "sess-1"));

        assertThat(result.success()).isFalse();
        assertThat(result.requiresApproval()).isTrue();
        assertThat(result.message()).contains("需要用户确认");
        assertThat(result.card()).isNotNull();
        assertThat(result.card().actionId()).isEqualTo("flowerbiz:创建坏账处理单");
        assertThat(result.card().toolId()).isEqualTo("ontology.action.createDraft");
        assertThat(result.card().planSummary()).contains("创建坏账处理单", "租赁报花明细");
        assertThat(result.card().impactScope()).contains("只创建草稿");
        assertThat(result.card().microForm().fields())
                .extracting(OntologyActionApprovalService.MicroFormField::key)
                .containsExactly("projectId", "draftItemJson", "badDebtType");
        verify(actionExecutor, never()).createDraft(eq("flowerbiz"), eq("创建坏账处理单"), anyMap());
        verify(auditService, never()).logActionExecution(any());
    }

    @Test
    void shouldRejectConfirmedDraftWhenGuardIsMissing() {
        when(semanticPackService.getPack("flowerbiz")).thenReturn(Optional.of(flowerbizActionPack()));
        OntologyActionApprovalService service = service();

        OntologyActionApprovalService.ActionApprovalResult result = service.requestDraft(
                new OntologyActionApprovalService.ActionApprovalRequest(
                        "flowerbiz",
                        "创建坏账处理单",
                        objectAttributes(),
                        true,
                        new CopilotUserContext("bob", "bob", "Bob", List.of("finance:viewer"), "finance", "key-1"),
                        "sess-1"));

        assertThat(result.success()).isFalse();
        assertThat(result.requiresApproval()).isFalse();
        assertThat(result.message()).contains("缺少权限", "flowerbiz:baddebt:draft");
        verify(actionExecutor, never()).createDraft(eq("flowerbiz"), eq("创建坏账处理单"), anyMap());
        ArgumentCaptor<AiAuditService.ActionAuditEvent> auditCaptor =
                ArgumentCaptor.forClass(AiAuditService.ActionAuditEvent.class);
        verify(auditService).logActionExecution(auditCaptor.capture());
        assertThat(auditCaptor.getValue().success()).isFalse();
        assertThat(auditCaptor.getValue().errorMessage()).contains("缺少权限");
        assertThat(auditCaptor.getValue().guard()).isEqualTo("flowerbiz:baddebt:draft");
    }

    @Test
    void shouldExecuteDraftAndAuditAfterConfirmationAndGuardPass() {
        when(semanticPackService.getPack("flowerbiz")).thenReturn(Optional.of(flowerbizActionPack()));
        when(actionExecutor.createDraft(eq("flowerbiz"), eq("创建坏账处理单"), anyMap()))
                .thenReturn(new OntologyActionExecutor.ActionDraftResult(
                        true,
                        "创建坏账处理单",
                        "/rs-flowers-base/flower/bizBadDebt/saveDraftFlowerBadDebt",
                        "/rs-flowers-base/flower/bizBadDebt/saveFlowerBadDebt",
                        Map.of("projectId", 101, "draftItemJson", "[501,502]", "badDebtType", 1),
                        "ok",
                        Map.of("id", 9001, "status", 20)));
        OntologyActionApprovalService service = service();

        OntologyActionApprovalService.ActionApprovalResult result = service.requestDraft(
                new OntologyActionApprovalService.ActionApprovalRequest(
                        "flowerbiz",
                        "创建坏账处理单",
                        objectAttributes(),
                        true,
                        userWithGuard(),
                        "sess-1"));

        assertThat(result.success()).isTrue();
        assertThat(result.requiresApproval()).isFalse();
        assertThat(result.message()).isEqualTo("ok");
        assertThat(result.draftResult()).isNotNull();
        assertThat(result.draftResult().responseBody()).containsEntry("id", 9001);
        verify(actionExecutor).createDraft(eq("flowerbiz"), eq("创建坏账处理单"), eq(objectAttributes()));
        ArgumentCaptor<AiAuditService.ActionAuditEvent> auditCaptor =
                ArgumentCaptor.forClass(AiAuditService.ActionAuditEvent.class);
        verify(auditService).logActionExecution(auditCaptor.capture());
        AiAuditService.ActionAuditEvent audit = auditCaptor.getValue();
        assertThat(audit.userId()).isEqualTo("alice");
        assertThat(audit.sessionId()).isEqualTo("sess-1");
        assertThat(audit.actionName()).isEqualTo("创建坏账处理单");
        assertThat(audit.objectName()).isEqualTo("租赁报花明细");
        assertThat(audit.guard()).isEqualTo("flowerbiz:baddebt:draft");
        assertThat(audit.input()).contains("项目id", "明细id");
        assertThat(audit.result()).contains("9001", "20");
        assertThat(audit.success()).isTrue();
    }

    private OntologyActionApprovalService service() {
        return new OntologyActionApprovalService(
                new OntologyService(semanticPackService),
                actionExecutor,
                new ActionGuardService(),
                auditService);
    }

    private static CopilotUserContext userWithGuard() {
        return new CopilotUserContext(
                "alice",
                "alice",
                "Alice",
                List.of("flowerbiz:baddebt:draft"),
                "ops",
                "key-1");
    }

    private static Map<String, Object> objectAttributes() {
        return Map.of(
                "项目id", 101,
                "明细id", List.of(501, 502),
                "业务类型", 1);
    }

    private static SemanticPackService.SemanticPack flowerbizActionPack() {
        SemanticPackService.SemanticObject leaseDetail = new SemanticPackService.SemanticObject(
                "租赁报花明细",
                "public.xycyl_ads_flowerbiz_lease_detail",
                "lease detail",
                List.of("项目id", "报花单id", "明细id", "业务类型"),
                List.of(),
                List.of("项目id", "业务类型"),
                "业务时间");
        return new SemanticPackService.SemanticPack(
                "flowerbiz",
                "flowerbiz action pack",
                List.of(leaseDetail),
                Map.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(new SemanticPackService.OntologyAction(
                        "创建坏账处理单",
                        "租赁报花明细",
                        "create baddebt draft",
                        new SemanticPackService.OntologyActionEndpoint(
                                "adminapi",
                                "/rs-flowers-base/flower/bizBadDebt/saveDraftFlowerBadDebt",
                                "/rs-flowers-base/flower/bizBadDebt/saveFlowerBadDebt"),
                        List.of(
                                new SemanticPackService.OntologyActionParam(
                                        "projectId", "租赁报花明细.项目id", true),
                                new SemanticPackService.OntologyActionParam(
                                        "draftItemJson", "租赁报花明细.明细id", true),
                                new SemanticPackService.OntologyActionParam(
                                        "badDebtType", "租赁报花明细.业务类型", true)),
                        "human",
                        true,
                        "flowerbiz:baddebt:draft")));
    }
}
