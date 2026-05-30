package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OntologyServiceTest {

    @Mock
    private SemanticPackService semanticPackService;

    @Test
    void shouldBuildObjectGraphAndIndexesFromPack() {
        SemanticPackService.SemanticObject customer = object("Customer", "public.customer");
        SemanticPackService.SemanticObject project = object("Project", "public.project");
        SemanticPackService.OntologyLink link = new SemanticPackService.OntologyLink(
                "customer_to_project",
                "Customer",
                "Project",
                "customer_id",
                "id",
                "one-to-many",
                "left_join",
                "keep orphan projects");
        SemanticPackService.OntologyMetric metric = new SemanticPackService.OntologyMetric(
                "baddebt_amount",
                "Customer",
                "baddebt_amount",
                "CNY",
                "currency",
                "sum baddebt amount");
        SemanticPackService.OntologySignal signal = new SemanticPackService.OntologySignal(
                "baddebt_risk",
                "Customer",
                "high",
                "baddebt_amount > 0",
                "create baddebt draft",
                List.of("create_baddebt_draft"));
        SemanticPackService.OntologyAction action = new SemanticPackService.OntologyAction(
                "create_baddebt_draft",
                "Customer",
                "create draft",
                new SemanticPackService.OntologyActionEndpoint("adminapi", "/draft", "/commit"),
                List.of(new SemanticPackService.OntologyActionParam("customerId", "customer_id", true)),
                "human",
                true,
                "flowerbiz:baddebt:draft");
        SemanticPackService.SemanticPack pack = new SemanticPackService.SemanticPack(
                "flowerbiz",
                "demo",
                List.of(customer, project),
                Map.of(),
                List.of(),
                List.of(),
                List.of(link),
                List.of(metric),
                List.of(signal),
                List.of(action));
        when(semanticPackService.getPack("flowerbiz")).thenReturn(Optional.of(pack));

        OntologyService service = new OntologyService(semanticPackService);

        OntologyService.OntologyModel model = service.load("flowerbiz").orElseThrow();
        assertThat(model.getObject("Customer")).contains(customer);
        assertThat(model.neighbors("Customer")).containsExactly(link);
        assertThat(model.metricsOf("Customer")).containsExactly(metric);
        assertThat(model.signalsOf("Customer")).containsExactly(signal);
        assertThat(model.actionsOf("Customer")).containsExactly(action);
    }

    @Test
    void shouldReturnEmptyIndexesWhenPackHasNoOntologySections() {
        SemanticPackService.SemanticPack pack = new SemanticPackService.SemanticPack(
                "legacy",
                "legacy pack",
                List.of(object("Lease", "public.lease")),
                Map.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of());
        when(semanticPackService.getPack("legacy")).thenReturn(Optional.of(pack));

        OntologyService service = new OntologyService(semanticPackService);

        OntologyService.OntologyModel model = service.load("legacy").orElseThrow();
        assertThat(model.getObject("Lease")).isPresent();
        assertThat(model.neighbors("Lease")).isEmpty();
        assertThat(model.metricsOf("Lease")).isEmpty();
        assertThat(model.signalsOf("Lease")).isEmpty();
        assertThat(model.actionsOf("Lease")).isEmpty();
    }

    @Test
    void shouldBuildLeftJoinPlanForCustomerToLeaseDetail() {
        when(semanticPackService.getPack("flowerbiz")).thenReturn(Optional.of(flowerbizGraphPack()));
        OntologyService service = new OntologyService(semanticPackService);

        OntologyService.JoinPlan plan = service.load("flowerbiz")
                .flatMap(model -> model.buildJoinPlan("客户", "租赁报花明细"))
                .orElseThrow();

        assertThat(plan.sql())
                .contains("FROM public.xycyl_dim_customer o0")
                .contains("LEFT JOIN public.xycyl_dim_project o1 ON o0.\"customer_code\" = o1.\"customer_code\"")
                .contains("LEFT JOIN public.xycyl_ads_flowerbiz_lease_detail o2 ON o1.\"project_id\" = o2.\"项目id\"");
        assertThat(plan.sourceRefs()).containsExactly(
                "public.xycyl_dim_customer",
                "public.xycyl_dim_project",
                "public.xycyl_ads_flowerbiz_lease_detail");
        assertThat(plan.preservesOrphans()).isTrue();
        assertThat(plan.joinHints()).containsExactly("客户_项目: 可能孤儿");
    }

    @Test
    void shouldBuildJsonArrayJoinPlanForLeaseDetailToSettlement() {
        when(semanticPackService.getPack("flowerbiz")).thenReturn(Optional.of(flowerbizGraphPack()));
        OntologyService service = new OntologyService(semanticPackService);

        OntologyService.JoinPlan plan = service.load("flowerbiz")
                .flatMap(model -> model.buildJoinPlan("租赁报花明细", "结算单"))
                .orElseThrow();

        assertThat(plan.sql())
                .contains("FROM public.xycyl_ads_flowerbiz_lease_detail o0")
                .contains("LEFT JOIN public.ods_ptr_mysql_f_settlement o1 ON EXISTS")
                .contains("jsonb_array_elements_text(o1.\"biz_ids_json\"::jsonb)")
                .contains("j0.value = o0.\"报花单id\"::text")
                .doesNotContain("o0.\"报花单id\" = o1.\"biz_ids_json\"");
        assertThat(plan.sourceRefs()).containsExactly(
                "public.xycyl_ads_flowerbiz_lease_detail",
                "public.ods_ptr_mysql_f_settlement");
        assertThat(plan.preservesOrphans()).isTrue();
        assertThat(plan.joinHints()).containsExactly("报花_结算: biz_ids_json 多报花 JSON 数组需展开");
    }

    @Test
    void shouldReturnCandidateJoinPlansWhenMultipleShortestPathsExist() {
        SemanticPackService.SemanticPack pack = new SemanticPackService.SemanticPack(
                "demo",
                "multi path demo",
                List.of(
                        object("客户", "public.customer", List.of("customer_id")),
                        object("项目", "public.project", List.of("customer_id", "project_id")),
                        object("合同", "public.contract", List.of("customer_id", "contract_id")),
                        object("报花", "public.flowerbiz", List.of("project_id", "contract_id"))),
                Map.of(),
                List.of(),
                List.of(),
                List.of(
                        new SemanticPackService.OntologyLink("客户_项目", "客户", "项目", "customer_id", "customer_id", "1:N", "", ""),
                        new SemanticPackService.OntologyLink("项目_报花", "项目", "报花", "project_id", "project_id", "1:N", "", ""),
                        new SemanticPackService.OntologyLink("客户_合同", "客户", "合同", "customer_id", "customer_id", "1:N", "", ""),
                        new SemanticPackService.OntologyLink("合同_报花", "合同", "报花", "contract_id", "contract_id", "1:N", "", "")),
                List.of(),
                List.of(),
                List.of());
        when(semanticPackService.getPack("demo")).thenReturn(Optional.of(pack));
        OntologyService service = new OntologyService(semanticPackService);

        List<OntologyService.JoinPlan> candidates = service.load("demo")
                .map(model -> model.buildJoinPlans("客户", "报花"))
                .orElseThrow();

        assertThat(candidates).hasSize(2);
        assertThat(candidates).extracting(OntologyService.JoinPlan::sourceRefs)
                .containsExactly(
                        List.of("public.customer", "public.project", "public.flowerbiz"),
                        List.of("public.customer", "public.contract", "public.flowerbiz"));
    }

    @Test
    void shouldBuildSignalQueryPlanForBaddebtRisk() {
        when(semanticPackService.getPack("flowerbiz")).thenReturn(Optional.of(flowerbizSignalPack()));
        OntologyService service = new OntologyService(semanticPackService);

        OntologyService.SignalPlan plan = service.load("flowerbiz")
                .flatMap(model -> model.buildSignalPlan("坏账风险"))
                .orElseThrow();

        assertThat(plan.signalName()).isEqualTo("坏账风险");
        assertThat(plan.objectName()).isEqualTo("坏账汇总");
        assertThat(plan.severity()).isEqualTo("high");
        assertThat(plan.metricNames()).containsExactly("项目坏账率", "坏账租金损失");
        assertThat(plan.linkedActions()).containsExactly("创建坏账处理单");
        assertThat(plan.sourceRefs()).containsExactly("public.xycyl_ads_flowerbiz_baddebt_summary");
        assertThat(plan.sql())
                .contains("FROM public.xycyl_ads_flowerbiz_baddebt_summary s0")
                .contains("GROUP BY s0.\"业务月份\", s0.\"项目\", s0.\"客户\"")
                .contains("HAVING")
                .contains("SUM(\"坏账租金损失全口径\")")
                .contains("> 0.15");
    }

    @Test
    void shouldEvaluateSignalHitsFromMetricValues() {
        when(semanticPackService.getPack("flowerbiz")).thenReturn(Optional.of(flowerbizSignalPack()));
        OntologyService service = new OntologyService(semanticPackService);
        OntologyService.OntologyModel model = service.load("flowerbiz").orElseThrow();

        List<OntologyService.SignalEvaluation> hits = model.evaluateSignals(Map.of(
                "项目坏账率", new BigDecimal("0.20"),
                "坏账租金损失", new BigDecimal("1200"),
                "客户在租金额", new BigDecimal("80000")));
        List<OntologyService.SignalEvaluation> misses = model.evaluateSignals(Map.of(
                "项目坏账率", new BigDecimal("0.05"),
                "坏账租金损失", BigDecimal.ZERO,
                "客户在租金额", new BigDecimal("80000")));

        assertThat(hits).extracting(OntologyService.SignalEvaluation::signalName)
                .containsExactly("坏账风险", "欠费预警");
        assertThat(hits.getFirst().advice()).contains("坏账处理");
        assertThat(hits.getFirst().linkedActions()).containsExactly("创建坏账处理单");
        assertThat(misses).isEmpty();
    }

    private static SemanticPackService.SemanticObject object(String name, String view) {
        return new SemanticPackService.SemanticObject(
                name,
                view,
                name + " description",
                List.of(name + " id"),
                List.of("amount"),
                List.of("month"),
                "month");
    }

    private static SemanticPackService.SemanticPack flowerbizGraphPack() {
        return new SemanticPackService.SemanticPack(
                "flowerbiz",
                "flowerbiz graph",
                List.of(
                        object("客户", "public.xycyl_dim_customer", List.of("customer_code", "customer_name")),
                        object("项目", "public.xycyl_dim_project", List.of("project_id", "customer_code")),
                        object("租赁报花明细", "public.xycyl_ads_flowerbiz_lease_detail", List.of("报花单id", "项目id", "明细id")),
                        object("采购明细", "authority.procurement.purchase_item_detail", List.of("flower_item_id")),
                        object("结算单", "public.ods_ptr_mysql_f_settlement", List.of("biz_ids_json"))),
                Map.of(),
                List.of(),
                List.of(),
                List.of(
                        new SemanticPackService.OntologyLink(
                                "客户_项目",
                                "客户",
                                "项目",
                                "customer_code",
                                "customer_code",
                                "1:N",
                                "可能孤儿",
                                "shared dimension may have orphan rows"),
                        new SemanticPackService.OntologyLink(
                                "项目_报花",
                                "项目",
                                "租赁报花明细",
                                "project_id",
                                "项目id",
                                "1:N",
                                "",
                                "project to lease detail"),
                        new SemanticPackService.OntologyLink(
                                "报花_采购",
                                "租赁报花明细",
                                "采购明细",
                                "明细id",
                                "flower_item_id",
                                "1:N",
                                "采购 flower_item_id 软外键",
                                "purchase soft foreign key"),
                        new SemanticPackService.OntologyLink(
                                "报花_结算",
                                "租赁报花明细",
                                "结算单",
                                "报花单id",
                                "biz_ids_json",
                                "N:1",
                                "biz_ids_json 多报花 JSON 数组需展开",
                                "settlement stores many flower biz ids as a JSON array")),
                List.of(),
                List.of(),
                List.of());
    }

    private static SemanticPackService.SemanticObject object(String name, String view, List<String> keyDimensions) {
        return new SemanticPackService.SemanticObject(
                name,
                view,
                name + " description",
                keyDimensions,
                List.of(),
                keyDimensions,
                "");
    }

    private static SemanticPackService.SemanticPack flowerbizSignalPack() {
        SemanticPackService.SemanticObject baddebt = object(
                "坏账汇总",
                "public.xycyl_ads_flowerbiz_baddebt_summary",
                List.of("业务月份", "项目", "客户"),
                List.of("坏账成本全口径", "坏账租金损失全口径"));
        SemanticPackService.SemanticObject customerMonthly = object(
                "客户月度汇总",
                "public.xycyl_dws_flowerbiz_customer_monthly",
                List.of("业务月份", "客户"),
                List.of("租金净额全口径", "坏账成本全口径"));
        return new SemanticPackService.SemanticPack(
                "flowerbiz",
                "flowerbiz signals",
                List.of(baddebt, customerMonthly),
                Map.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(
                        new SemanticPackService.OntologyMetric(
                                "坏账租金损失",
                                "坏账汇总",
                                "SUM(\"坏账租金损失全口径\")",
                                "CNY",
                                "currency",
                                "dbt_amount:rent"),
                        new SemanticPackService.OntologyMetric(
                                "项目坏账率",
                                "坏账汇总",
                                "SUM(\"坏账租金损失全口径\") / NULLIF(SUM(\"坏账成本全口径\") + SUM(\"坏账租金损失全口径\"), 0)",
                                "%",
                                "percent",
                                "dbt_amount:rent + dbt_amount:cost"),
                        new SemanticPackService.OntologyMetric(
                                "客户在租金额",
                                "客户月度汇总",
                                "SUM(\"租金净额全口径\")",
                                "CNY",
                                "currency",
                                "dbt_amount:rent")),
                List.of(
                        new SemanticPackService.OntologySignal(
                                "坏账风险",
                                "坏账汇总",
                                "high",
                                "项目坏账率 > 0.15 AND 坏账租金损失 > 0",
                                "建议发起坏账处理单草稿",
                                List.of("创建坏账处理单")),
                        new SemanticPackService.OntologySignal(
                                "欠费预警",
                                "客户月度汇总",
                                "medium",
                                "客户在租金额 > 50000 AND 坏账租金损失 > 0",
                                "建议跟进回款",
                                List.of())),
                List.of());
    }

    private static SemanticPackService.SemanticObject object(
            String name,
            String view,
            List<String> keyDimensions,
            List<String> keyMeasures) {
        return new SemanticPackService.SemanticObject(
                name,
                view,
                name + " description",
                keyDimensions,
                keyMeasures,
                keyDimensions,
                keyDimensions.getFirst());
    }
}
