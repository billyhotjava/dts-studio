package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuzhi.dts.copilot.ai.domain.Nl2SqlQueryTemplate;
import com.yuzhi.dts.copilot.ai.repository.Nl2SqlQueryTemplateRepository;
import com.yuzhi.dts.copilot.ai.service.copilot.AgentBiReportCatalogService.ReportCatalogEntry;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AgentBiReportCatalogServiceTest {

    private final AgentBiReportCatalogService catalog = new AgentBiReportCatalogService();

    @Test
    void exposesPrsBusinessDomainsForAgentBi() {
        assertThat(catalog.entries())
                .extracting(ReportCatalogEntry::domain)
                .contains("flowerbiz", "project", "rental_receivable", "purchase_inventory", "task_supervise", "workflow_audit");
    }

    @Test
    void matchesFlowerbizTrendQuestionToReportDraftMartSurface() {
        Optional<ReportCatalogEntry> match =
                catalog.findBestMatch("从2025年5月到现在，租赁收入按月趋势怎么样", "flowerbiz");

        assertThat(match).isPresent();
        assertThat(match.get().reportCode()).isEqualTo("prs.flowerbiz.lease_execution_monthly");
        assertThat(match.get().responseKind()).isEqualTo("REPORT_DRAFT");
        assertThat(match.get().dataSurface()).isEqualTo("L1_DBT_MART");
        assertThat(match.get().primaryTarget()).isEqualTo("public.xycyl_dws_flowerbiz_project_monthly");
        assertThat(match.get().qualityLevel()).isEqualTo("MEDIUM");
        assertThat(match.get().defaultDisplay()).isEqualTo("line");
        assertThat(match.get().qualityNotes()).isNotEmpty();
        assertThat(match.get().sourceRefs())
                .contains(
                        "dbt-model:public.xycyl_dws_flowerbiz_project_monthly",
                        "semantic-pack:flowerbiz"
                );
    }

    @Test
    void matchesProjectBillDetailQuestionToAdminApiReadonlySurface() {
        Optional<ReportCatalogEntry> match =
                catalog.findBestMatch("这个项目有哪些待确认账单", "project");

        assertThat(match).isPresent();
        assertThat(match.get().reportCode()).isEqualTo("prs.rental.pending_bill_detail");
        assertThat(match.get().responseKind()).isEqualTo("BUSINESS_DETAIL");
        assertThat(match.get().dataSurface()).isEqualTo("L0_ADMINAPI_READONLY");
        assertThat(match.get().primaryTarget()).contains("/operate/monthAccount");
        assertThat(match.get().qualityLevel()).isEqualTo("MEDIUM");
        assertThat(match.get().sourceRefs())
                .contains("adminapi:/rs-flowers-base/operate/monthAccount/listGreenAccountingPage");
    }

    @Test
    void matchesCollectionFollowupRequestToSafeActionProposal() {
        Optional<ReportCatalogEntry> match =
                catalog.findBestMatch("帮我发起催收任务", "rental_receivable");

        assertThat(match).isPresent();
        assertThat(match.get().reportCode()).isEqualTo("prs.rental.collection_followup_proposal");
        assertThat(match.get().responseKind()).isEqualTo("ACTION_PROPOSAL");
        assertThat(match.get().dataSurface()).isEqualTo("ACTION_PROPOSAL");
        assertThat(match.get().primaryTarget()).isEqualTo("rental.create_collection_followup");
        assertThat(match.get().businessActionAllowed()).isFalse();
    }

    @Test
    void enrichesCatalogEntriesWithActiveDbtQueryTemplateRefs() {
        Nl2SqlQueryTemplate template = new Nl2SqlQueryTemplate();
        template.setTemplateCode("TPL-PRS-LEASE-TREND");
        template.setDomain("flowerbiz");
        template.setTargetView("public.xycyl_dws_flowerbiz_project_monthly");
        template.setQuestionSamples("[\"租赁收入趋势\"]");
        template.setIntentPatterns("[\".*租赁.*趋势.*\"]");
        template.setSqlTemplate("select * from public.xycyl_dws_flowerbiz_project_monthly");
        template.setIsActive(true);
        template.setPriority(100);

        Nl2SqlQueryTemplateRepository repository = mock(Nl2SqlQueryTemplateRepository.class);
        when(repository.findByIsActiveTrueOrderByPriorityDesc()).thenReturn(List.of(template));
        SemanticPackService semanticPackService = new SemanticPackService(new ObjectMapper());
        semanticPackService.init();

        AgentBiReportCatalogService dynamicCatalog = new AgentBiReportCatalogService(
                semanticPackService,
                repository,
                new ObjectMapper());

        Optional<ReportCatalogEntry> match =
                dynamicCatalog.findBestMatch("租赁收入趋势", "flowerbiz");

        assertThat(match).isPresent();
        assertThat(match.get().sourceRefs())
                .contains("query-template:TPL-PRS-LEASE-TREND");
    }
}
