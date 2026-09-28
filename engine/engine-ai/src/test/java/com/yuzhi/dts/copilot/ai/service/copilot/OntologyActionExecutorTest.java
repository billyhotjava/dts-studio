package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OntologyActionExecutorTest {

    @Mock
    private SemanticPackService semanticPackService;

    @Mock
    private AdminApiActionClient adminApiActionClient;

    @Test
    void shouldAssembleParamsAndCallOnlyDraftEndpoint() {
        when(semanticPackService.getPack("flowerbiz")).thenReturn(Optional.of(flowerbizActionPack()));
        when(adminApiActionClient.postDraft(eq("/rs-flowers-base/flower/bizBadDebt/saveDraftFlowerBadDebt"), anyMap()))
                .thenReturn(new AdminApiActionClient.AdminApiActionResponse(
                        true,
                        "ok",
                        Map.of("id", 9001, "status", 20)));
        OntologyActionExecutor executor = new OntologyActionExecutor(
                new OntologyService(semanticPackService),
                adminApiActionClient,
                new ObjectMapper());

        OntologyActionExecutor.ActionDraftResult result = executor.createDraft(
                "flowerbiz",
                "创建坏账处理单",
                Map.of(
                        "项目id", 101,
                        "明细id", List.of(501, 502),
                        "业务类型", 1));

        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(adminApiActionClient).postDraft(
                eq("/rs-flowers-base/flower/bizBadDebt/saveDraftFlowerBadDebt"),
                payloadCaptor.capture());
        verify(adminApiActionClient, never()).postCommit(
                eq("/rs-flowers-base/flower/bizBadDebt/saveFlowerBadDebt"),
                anyMap());
        assertThat(result.success()).isTrue();
        assertThat(result.draftEndpoint()).isEqualTo("/rs-flowers-base/flower/bizBadDebt/saveDraftFlowerBadDebt");
        assertThat(result.commitEndpoint()).isEqualTo("/rs-flowers-base/flower/bizBadDebt/saveFlowerBadDebt");
        assertThat(result.message()).isEqualTo("ok");
        assertThat(payloadCaptor.getValue())
                .containsEntry("projectId", 101)
                .containsEntry("badDebtType", 1);
        assertThat(payloadCaptor.getValue().get("draftItemJson").toString()).contains("501", "502");
    }

    @Test
    void shouldAcceptApprovedFormParamNamesAsObjectAttributes() {
        when(semanticPackService.getPack("flowerbiz")).thenReturn(Optional.of(flowerbizActionPack()));
        when(adminApiActionClient.postDraft(eq("/rs-flowers-base/flower/bizBadDebt/saveDraftFlowerBadDebt"), anyMap()))
                .thenReturn(new AdminApiActionClient.AdminApiActionResponse(
                        true,
                        "ok",
                        Map.of("id", 9001)));
        OntologyActionExecutor executor = new OntologyActionExecutor(
                new OntologyService(semanticPackService),
                adminApiActionClient,
                new ObjectMapper());

        OntologyActionExecutor.ActionDraftResult result = executor.createDraft(
                "flowerbiz",
                "创建坏账处理单",
                Map.of(
                        "projectId", 101,
                        "draftItemJson", "[501]",
                        "badDebtType", 1));

        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(adminApiActionClient).postDraft(
                eq("/rs-flowers-base/flower/bizBadDebt/saveDraftFlowerBadDebt"),
                payloadCaptor.capture());
        assertThat(result.success()).isTrue();
        assertThat(payloadCaptor.getValue())
                .containsEntry("projectId", 101)
                .containsEntry("draftItemJson", "[501]")
                .containsEntry("badDebtType", 1);
    }

    @Test
    void shouldRejectMissingRequiredParamBeforeCallingAdminapi() {
        when(semanticPackService.getPack("flowerbiz")).thenReturn(Optional.of(flowerbizActionPack()));
        OntologyActionExecutor executor = new OntologyActionExecutor(
                new OntologyService(semanticPackService),
                adminApiActionClient,
                new ObjectMapper());

        OntologyActionExecutor.ActionDraftResult result = executor.createDraft(
                "flowerbiz",
                "创建坏账处理单",
                Map.of("项目id", 101));

        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("Missing required action param: draftItemJson");
        verify(adminApiActionClient, never()).postDraft(
                eq("/rs-flowers-base/flower/bizBadDebt/saveDraftFlowerBadDebt"),
                anyMap());
        verify(adminApiActionClient, never()).postCommit(
                eq("/rs-flowers-base/flower/bizBadDebt/saveFlowerBadDebt"),
                anyMap());
    }

    @Test
    void shouldPassThroughAdminapiErrors() {
        when(semanticPackService.getPack("flowerbiz")).thenReturn(Optional.of(flowerbizActionPack()));
        when(adminApiActionClient.postDraft(eq("/rs-flowers-base/flower/bizBadDebt/saveDraftFlowerBadDebt"), anyMap()))
                .thenReturn(new AdminApiActionClient.AdminApiActionResponse(
                        false,
                        "参数错误",
                        Map.of()));
        OntologyActionExecutor executor = new OntologyActionExecutor(
                new OntologyService(semanticPackService),
                adminApiActionClient,
                new ObjectMapper());

        OntologyActionExecutor.ActionDraftResult result = executor.createDraft(
                "flowerbiz",
                "创建坏账处理单",
                Map.of(
                        "项目id", 101,
                        "明细id", 501,
                        "业务类型", 1));

        assertThat(result.success()).isFalse();
        assertThat(result.message()).isEqualTo("参数错误");
        verify(adminApiActionClient, never()).postCommit(
                eq("/rs-flowers-base/flower/bizBadDebt/saveFlowerBadDebt"),
                anyMap());
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
