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
        assertThat(match.get().responseKind()).isEqualTo("FIXED_REPORT");
        assertThat(match.get().dataSurface()).isEqualTo("L2_FIXED_REPORT");
        assertThat(match.get().primaryTarget()).isEqualTo("public.xycyl_ads_flowerbiz_lease_summary");
        assertThat(match.get().qualityLevel()).isEqualTo("MEDIUM");
        assertThat(match.get().defaultDisplay()).isEqualTo("table");
        assertThat(match.get().qualityNotes()).isNotEmpty();
        assertThat(match.get().sourceRefs())
                .contains(
                        "fixed-report:PRS-FLOWERBIZ-LEASE-EXECUTION",
                        "dbt-model:public.xycyl_ads_flowerbiz_lease_summary",
                        "semantic-pack:flowerbiz"
                );
    }

    @Test
    void matchesFlowerbizMonthlyReportOpenQuestionToLeaseExecutionFixedReport() {
        Optional<ReportCatalogEntry> match =
                catalog.findBestMatch("打开报花月报，按月展示 PRS 租赁报花执行、收入、回收和异常波动。", "flowerbiz");

        assertThat(match).isPresent();
        assertThat(match.get().reportCode()).isEqualTo("prs.flowerbiz.lease_execution_monthly");
        assertThat(match.get().responseKind()).isEqualTo("FIXED_REPORT");
        assertThat(match.get().primaryTarget()).isEqualTo("public.xycyl_ads_flowerbiz_lease_summary");
        assertThat(match.get().sourceRefs())
                .contains("fixed-report:PRS-FLOWERBIZ-LEASE-EXECUTION");
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
    void matchesProjectTopQuestionToUnifiedFixedReportAsset() {
        Optional<ReportCatalogEntry> match =
                catalog.findBestMatch("项目经营 TOP", "project");

        assertThat(match).isPresent();
        assertThat(match.get().reportCode()).isEqualTo("prs.project.overview");
        assertThat(match.get().responseKind()).isEqualTo("FIXED_REPORT");
        assertThat(match.get().dataSurface()).isEqualTo("L2_FIXED_REPORT");
        assertThat(match.get().primaryTarget()).isEqualTo("public.xycyl_ads_project_overview");
        assertThat(match.get().sourceRefs())
                .contains(
                        "fixed-report:PRS-PROJECT-OVERVIEW",
                        "dbt-model:public.xycyl_ads_project_overview",
                        "semantic-pack:project");
    }

    @Test
    void matchesProjectContractExpiryQuestionToSprint25DtsMart() {
        Optional<ReportCatalogEntry> match =
                catalog.findBestMatch("哪些合同90天内到期", "project");

        assertThat(match).isPresent();
        assertThat(match.get().reportCode()).isEqualTo("prs.project.contract_expiry_alert");
        assertThat(match.get().dataSurface()).isEqualTo("L2_FIXED_REPORT");
        assertThat(match.get().primaryTarget()).isEqualTo("public.xycyl_ads_contract_expiry_alert");
        assertThat(match.get().sourceRefs())
                .contains("dbt-model:public.xycyl_ads_contract_expiry_alert");
    }

    @Test
    void matchesProjectStatusQuestionToSprint25DtsMart() {
        Optional<ReportCatalogEntry> match =
                catalog.findBestMatch("项目状态和实摆状态分布", "project");

        assertThat(match).isPresent();
        assertThat(match.get().reportCode()).isEqualTo("prs.project.status_distribution");
        assertThat(match.get().dataSurface()).isEqualTo("L2_FIXED_REPORT");
        assertThat(match.get().primaryTarget()).isEqualTo("public.xycyl_ads_project_status_dist");
        assertThat(match.get().sourceRefs())
                .contains("dbt-model:public.xycyl_ads_project_status_dist");
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
        template.setTargetView("public.xycyl_ads_flowerbiz_lease_summary");
        template.setQuestionSamples("[\"租赁收入趋势\"]");
        template.setIntentPatterns("[\".*租赁.*趋势.*\"]");
        template.setSqlTemplate("select * from public.xycyl_ads_flowerbiz_lease_summary");
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
