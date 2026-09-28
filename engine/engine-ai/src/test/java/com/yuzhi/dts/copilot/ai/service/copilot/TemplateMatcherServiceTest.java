package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuzhi.dts.copilot.ai.domain.Nl2SqlQueryTemplate;
import com.yuzhi.dts.copilot.ai.repository.Nl2SqlQueryTemplateRepository;
import com.yuzhi.dts.copilot.ai.service.copilot.TemplateMatcherService.SuggestedQuestion;
import com.yuzhi.dts.copilot.ai.service.copilot.TemplateMatcherService.TemplateMatchResult;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Unit tests for TemplateMatcherService.
 * Covers template matching scenarios T-01 through T-10 from the acceptance matrix,
 * plus parameter extraction and suggested questions.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TemplateMatcherServiceTest {

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    @Mock
    private Nl2SqlQueryTemplateRepository templateRepository;

    private TemplateMatcherService matcherService;

    @BeforeEach
    void setUp() {
        matcherService = new TemplateMatcherService(templateRepository, new ObjectMapper());
        when(templateRepository.findByIsActiveTrueOrderByPriorityDesc()).thenReturn(buildMockTemplates());
    }

    // ===================== Template matching =====================

    @Test
    @DisplayName("T-01: 项目绿植数 (TPL-01) - 含项目名参数提取")
    void matchProjectGreenCount() {
        TemplateMatchResult result = matcherService.match("翠湖项目的绿植有多少");

        assertThat(result.matched()).isTrue();
        assertThat(result.template().getTemplateCode()).isEqualTo("TPL-01");
        assertThat(result.resolvedSql()).contains("v_project_green_current");
    }

    @Test
    @DisplayName("T-02: 在服项目总数 (TPL-02)")
    void matchActiveProjectCount() {
        TemplateMatchResult result = matcherService.match("当前在服项目一共多少个");

        assertThat(result.matched()).isTrue();
        assertThat(result.template().getTemplateCode()).isEqualTo("TPL-02");
        assertThat(result.resolvedSql()).contains("v_project_overview");
        assertThat(result.resolvedSql()).contains("正常");
    }

    @Test
    @DisplayName("T-04: 加花排行 (TPL-06)")
    void matchFlowerAddRanking() {
        TemplateMatchResult result = matcherService.match("各项目加花次数排行");

        assertThat(result.matched()).isTrue();
        assertThat(result.template().getTemplateCode()).isEqualTo("TPL-06");
        assertThat(result.resolvedSql()).contains("v_flower_biz_detail");
    }

    @Test
    @DisplayName("T-05: 待审批报花单 (TPL-08)")
    void matchPendingApproval() {
        TemplateMatchResult result = matcherService.match("有多少待审批的报花单");

        assertThat(result.matched()).isTrue();
        assertThat(result.template().getTemplateCode()).isEqualTo("TPL-08");
        assertThat(result.resolvedSql()).contains("审核中");
    }

    @Test
    @DisplayName("T-06: 项目月租金 (TPL-09) - 上月时间解析")
    void matchProjectRentWithLastMonth() {
        TemplateMatchResult result = matcherService.match("万科项目上个月租金");

        assertThat(result.matched()).isTrue();
        assertThat(result.template().getTemplateCode()).isEqualTo("TPL-09");
        assertThat(result.extractedParams()).containsKey("month");
        String expectedMonth = LocalDate.now().minusMonths(1).format(MONTH_FMT);
        assertThat(result.extractedParams().get("month")).isEqualTo(expectedMonth);
        assertThat(result.extractedParams()).containsKey("project_name");
        assertThat(result.extractedParams().get("project_name")).isEqualTo("万科");
        assertThat(result.resolvedSql()).contains("v_monthly_settlement");
    }

    @Test
    @DisplayName("T-07: 未结算项目 (TPL-10) - 上月默认")
    void matchUnsettledProjects() {
        TemplateMatchResult result = matcherService.match("上月未结算的项目");

        assertThat(result.matched()).isTrue();
        assertThat(result.template().getTemplateCode()).isEqualTo("TPL-10");
        String expectedMonth = LocalDate.now().minusMonths(1).format(MONTH_FMT);
        assertThat(result.extractedParams().get("month")).isEqualTo(expectedMonth);
        assertThat(result.resolvedSql()).contains("待结算");
    }

    @Test
    @DisplayName("T-08: 进行中任务 (TPL-13)")
    void matchInProgressTasks() {
        TemplateMatchResult result = matcherService.match("进行中的任务有哪些");

        assertThat(result.matched()).isTrue();
        assertThat(result.template().getTemplateCode()).isEqualTo("TPL-13");
        assertThat(result.resolvedSql()).contains("v_task_progress");
        assertThat(result.resolvedSql()).contains("进行中");
    }

    @Test
    @DisplayName("T-10: 结算方式分布 (TPL-20)")
    void matchSettlementTypeDistribution() {
        TemplateMatchResult result = matcherService.match("各结算方式的项目分布");

        assertThat(result.matched()).isTrue();
        assertThat(result.template().getTemplateCode()).isEqualTo("TPL-20");
        assertThat(result.resolvedSql()).contains("settlement_type_name");
    }

    @Test
    @DisplayName("固定报表意图: 旧财务采购模板不再作为固定报表快路径")
    void legacyFinanceAndProcurementPageLabelsNoLongerMatchFixedReportFastPath() {
        assertThat(matcherService.match("打开财务结算汇总").matched()).isFalse();
        assertThat(matcherService.match("查看采购汇总").matched()).isFalse();
    }

    @Test
    @DisplayName("仓库主题: 库存现量和低库存走可执行模板, 未发布出入库资产仍不进快路径")
    void warehouseAuthorityTopicsUseExecutableTemplatesInsteadOfFixedReportPlaceholders() {
        TemplateMatchResult stockOverview = matcherService.match("库存现量");
        assertThat(stockOverview.matched()).isTrue();
        assertThat(stockOverview.template().getTemplateCode()).isEqualTo("TPL-53");
        assertThat(stockOverview.template().getTargetView()).isEqualTo("mysql.rs_cloud_flower.s_stock_info");

        TemplateMatchResult lowStock = matcherService.match("低库存预警");
        assertThat(lowStock.matched()).isTrue();
        assertThat(lowStock.template().getTemplateCode()).isEqualTo("TPL-54");
        assertThat(lowStock.template().getTargetView()).isEqualTo("mysql.rs_cloud_flower.s_stock_info");

        assertThat(matcherService.match("本月出入库记录").matched()).isFalse();
    }

    @Test
    @DisplayName("PRS 大屏意图: 租赁经营总览命中大屏资产")
    void matchPrsFlowerbizOverviewScreen() {
        TemplateMatchResult result = matcherService.match("打开PRS租赁经营总览大屏");

        assertThat(result.matched()).isTrue();
        assertThat(result.template().getTemplateCode()).isEqualTo("PRS-FLOWERBIZ-OVERVIEW");
        assertThat(result.template().getTargetView()).isEqualTo("screen.prs-flowerbiz-overview-v1");
        assertThat(result.resolvedSql()).isNull();
    }

    @Test
    @DisplayName("PRS 细分固定报表意图: 项目经营 TOP 命中项目客户 ADS 子报表")
    void matchPrsProjectCustomerTopFixedReport() {
        TemplateMatchResult result = matcherService.match("项目经营 TOP");

        assertThat(result.matched()).isTrue();
        assertThat(result.template().getTemplateCode()).isEqualTo("PRS-FLOWERBIZ-PROJECT-CUSTOMER-TOP");
        assertThat(result.template().getTargetView()).isEqualTo("public.xycyl_ads_flowerbiz_project_customer");
        assertThat(result.resolvedSql()).isNull();
    }


    @Test
    @DisplayName("PRS 大屏候选: flowerbiz 域返回页面化大屏候选")
    void fixedReportSuggestionsIncludePrsFlowerbizScreens() {
        List<SuggestedQuestion> suggestions = matcherService.getFixedReportSuggestionsByDomain("flowerbiz", 20);

        assertThat(suggestions)
                .extracting(SuggestedQuestion::question)
	                .containsExactly(
	                        "PRS 租赁经营总览",
	                        "PRS 租赁报花执行看板",
	                        "PRS 销售坏账与费用看板",
	                        "PRS 坏账排行",
	                        "PRS 养护人工作量看板",
	                        "PRS 在途审批与操作监控",
	                        "PRS 在途状态清单",
	                        "PRS 项目经营 TOP",
	                        "PRS 项目客户经营看板",
	                        "PRS 变更与租期调整看板",
                        "PRS 回收撤摆与去向看板",
                        "PRS 报花单明细钻取",
                        "PRS 变更明细钻取",
                        "PRS 回收明细钻取",
                        "PRS 审批操作链路钻取");
        assertThat(suggestions)
                .extracting(SuggestedQuestion::templateCode)
                .allMatch(code -> code.startsWith("PRS-FLOWERBIZ-"));
    }

    @Test
    @DisplayName("采购域: 某月某产品采购明细按采购人金额统计命中 authority 模板")
    void matchProcurementProductAmountByBuyer() {
        TemplateMatchResult result = matcherService.match("查询2025年2月，绿萝这个产品的采购详细情况，按采购人、采购金额统计");

        assertThat(result.matched()).isTrue();
        assertThat(result.template().getTemplateCode()).isEqualTo("TPL-33");
        assertThat(result.extractedParams().get("month")).isEqualTo("2025-02");
        assertThat(result.extractedParams().get("good_name")).isEqualTo("绿萝");
        assertThat(result.resolvedSql()).contains("t_purchase_price_item");
        assertThat(result.resolvedSql()).contains("purchase_user_name");
        assertThat(result.resolvedSql()).doesNotContain("i_pendulum_purchase");
        assertThat(result.resolvedSql()).doesNotContain("title like");
    }

    @Test
    @DisplayName("采购域: 某月某产品采购明细列表命中 authority 模板")
    void matchProcurementProductDetailList() {
        TemplateMatchResult result = matcherService.match("查询2025年2月绿萝采购明细");

        assertThat(result.matched()).isTrue();
        assertThat(result.template().getTemplateCode()).isEqualTo("TPL-31");
        assertThat(result.extractedParams().get("month")).isEqualTo("2025-02");
        assertThat(result.extractedParams().get("good_name")).isEqualTo("绿萝");
        assertThat(result.resolvedSql()).contains("t_purchase_price_item");
        assertThat(result.resolvedSql()).contains("ORDER BY a.purchase_time DESC");
    }

    @Test
    @DisplayName("采购域: 全年各绿植采购情况命中联邦 MySQL 汇总模板")
    void matchProcurementGreenPurchaseOverviewByYear() {
        TemplateMatchResult result = matcherService.match("看下2026年各个绿植的采购情况");

        assertThat(result.matched()).isTrue();
        assertThat(result.template().getTemplateCode()).isEqualTo("TPL-34");
        assertThat(result.extractedParams()).containsEntry("year", "2026");
        assertThat(result.resolvedSql()).contains("mysql.rs_cloud_flower.t_purchase_price_item");
        assertThat(result.resolvedSql()).contains("TIMESTAMP '2026-01-01 00:00:00'");
        assertThat(result.resolvedSql()).doesNotContain("PRODUCTION");
        assertThat(result.resolvedSql()).doesNotContain("TRY_TO_NUMBER");
        assertThat(result.resolvedSql()).doesNotContain("PRS_PROCUREMENT_DELIVERY_RECORD");
    }

    @Test
    @DisplayName("报花域: 年度销售情况命中 dbt 销售汇总模板")
    void matchFlowerbizSalesOverviewByYear() {
        TemplateMatchResult result = matcherService.match("查询2026年销售情况");

        assertThat(result.matched()).isTrue();
        assertThat(result.template().getTemplateCode()).isEqualTo("TPL-52");
        assertThat(result.extractedParams()).containsEntry("year", "2026");
        assertThat(result.template().getDomain()).isEqualTo("flowerbiz");
        assertThat(result.template().getTargetView()).isEqualTo("public.xycyl_ads_flowerbiz_sale_summary");
        assertThat(result.resolvedSql()).contains("public.xycyl_ads_flowerbiz_sale_summary");
        assertThat(result.resolvedSql()).contains("s.\"年份\" = 2026");
        assertThat(result.resolvedSql()).doesNotContain("::numeric");
        assertThat(result.resolvedSql()).doesNotContain("to_char");
        assertThat(result.resolvedSql()).doesNotContain("PRODUCTION");
        assertThat(result.resolvedSql()).doesNotContain("v_flower_biz_detail");
    }

    @Test
    @DisplayName("财务域: 年度财务数据统计命中 dbt ADS 财务模板")
    void matchFinanceDataOverviewByYear() {
        TemplateMatchResult result = matcherService.match("2026年财务数据统计下");

        assertThat(result.matched()).isTrue();
        assertThat(result.template().getTemplateCode()).isEqualTo("TPL-56");
        assertThat(result.extractedParams()).containsEntry("year", "2026");
        assertThat(result.template().getDomain()).isEqualTo("finance");
        assertThat(result.template().getTargetView()).isEqualTo("public.xycyl_ads_finance_month_settlement");
        assertThat(result.resolvedSql())
                .contains("public.xycyl_ads_finance_month_settlement")
                .contains("public.xycyl_ads_finance_collection")
                .contains("s.\"业务月份\" LIKE CAST(2026 AS VARCHAR) || '-%'")
                .contains("\"应收折前\"")
                .contains("\"收款金额\"")
                .doesNotContain("mysql.rs_cloud_flower.a_month_accounting")
                .doesNotContain("mysql.rs_cloud_flower.a_collection_record");
    }

    @Test
    @DisplayName("财务域: 年度凭证数据统计命中 dbt ADS 凭证模板")
    void matchFinanceVoucherOverviewByYear() {
        TemplateMatchResult result = matcherService.match("2026年凭证的数据统计下");

        assertThat(result.matched()).isTrue();
        assertThat(result.template().getTemplateCode()).isEqualTo("TPL-57");
        assertThat(result.extractedParams()).containsEntry("year", "2026");
        assertThat(result.template().getDomain()).isEqualTo("finance");
        assertThat(result.template().getTargetView()).isEqualTo("public.xycyl_ads_finance_voucher_monthly");
        assertThat(result.resolvedSql())
                .contains("public.xycyl_ads_finance_voucher_monthly")
                .contains("v.\"会计月份\" LIKE CAST(2026 AS VARCHAR) || '-%'")
                .contains("\"凭证数\"")
                .contains("\"分录数\"")
                .contains("\"借贷差额\"")
                .doesNotContain("mysql.rs_cloud_flower.f_voucher")
                .doesNotContain("mysql.rs_cloud_flower.f_voucher_item");
    }

    @Test
    @DisplayName("库存域: 库存现状命中联邦 MySQL 库存现量模板")
    void matchInventoryStockOverview() {
        TemplateMatchResult result = matcherService.match("展示2026年库存现状");

        assertThat(result.matched()).isTrue();
        assertThat(result.template().getTemplateCode()).isEqualTo("TPL-53");
        assertThat(result.template().getDomain()).isEqualTo("warehouse");
        assertThat(result.template().getTargetView()).isEqualTo("mysql.rs_cloud_flower.s_stock_info");
        assertThat(result.resolvedSql()).contains("mysql.rs_cloud_flower.s_stock_info");
        assertThat(result.resolvedSql()).contains("good_price_id");
        assertThat(result.resolvedSql()).doesNotContain("authority.inventory.stock_overview");
        assertThat(result.resolvedSql()).doesNotContain("WH-STOCK-OVERVIEW");
    }

    @Test
    @DisplayName("库存域: 低库存预警命中可执行弱路径模板而不是资产库占位")
    void matchInventoryLowStockAlert() {
        TemplateMatchResult result = matcherService.match("低库存预警");

        assertThat(result.matched()).isTrue();
        assertThat(result.template().getTemplateCode()).isEqualTo("TPL-54");
        assertThat(result.template().getDomain()).isEqualTo("warehouse");
        assertThat(result.template().getTargetView()).isEqualTo("mysql.rs_cloud_flower.s_stock_info");
        assertThat(result.resolvedSql()).contains("HAVING SUM(COALESCE(good_number, 0)) <= 2");
        assertThat(result.resolvedSql()).doesNotContain("authority.inventory.low_stock_alert");
        assertThat(result.resolvedSql()).doesNotContain("WH-LOW-STOCK-ALERT");
    }

    @Test
    @DisplayName("报花域: 某月报花单据明细命中业务对象真实字段模板")
    void matchFlowerbizMonthlyOrderDetail() {
        TemplateMatchResult result = matcherService.match("2026年3月的报花单据查下");

        assertThat(result.matched()).isTrue();
        assertThat(result.template().getTemplateCode()).isEqualTo("TPL-55");
        assertThat(result.extractedParams()).containsEntry("month", "2026-03");
        assertThat(result.template().getTargetView()).isEqualTo("mysql.rs_cloud_flower.t_flower_biz_info");
        assertThat(result.resolvedSql())
                .contains("mysql.rs_cloud_flower.t_flower_biz_info")
                .contains("f.customer_name")
                .contains("f.project_manage_name")
                .contains("f.apply_use_name")
                .contains("CAST('2026-03' || '-01 00:00:00' AS TIMESTAMP)")
                .doesNotContain("curr_customer_name")
                .doesNotContain("proj_manager_name")
                .doesNotContain("apply_user_name");
    }

    @Test
    @DisplayName("模板参数: curing_user_name 必填参数可从养护师傅问句提取")
    void extractCuringUserNameForFlowerbizTemplate() {
        TemplateMatchResult result = matcherService.match("李师傅本月经手多少报花单");

        assertThat(result.matched()).isTrue();
        assertThat(result.template().getTemplateCode()).isEqualTo("TPL-39");
        assertThat(result.extractedParams()).containsEntry("curing_user_name", "李师傅");
        assertThat(result.resolvedSql()).doesNotContain(":curing_user_name");
    }

    @Test
    @DisplayName("模板参数: 必填参数缺失时不返回带未解析占位符的 SQL")
    void requiredParamMissingDoesNotReturnUnresolvedSql() {
        TemplateMatchResult result = matcherService.match("师傅本月报花");

        assertThat(result.matched()).isFalse();
        assertThat(result.resolvedSql()).isNull();
    }

    // ===================== No match =====================

    @Test
    @DisplayName("T-09: 无匹配降级 - 无关问题")
    void noMatchForIrrelevantQuestion() {
        TemplateMatchResult result = matcherService.match("今天天气怎么样");

        assertThat(result.matched()).isFalse();
        assertThat(result.template()).isNull();
        assertThat(result.resolvedSql()).isNull();
    }

    @Test
    @DisplayName("空输入 -> matched=false")
    void emptyInputReturnsNoMatch() {
        TemplateMatchResult result = matcherService.match("");

        assertThat(result.matched()).isFalse();
    }

    @Test
    @DisplayName("null 输入 -> matched=false")
    void nullInputReturnsNoMatch() {
        TemplateMatchResult result = matcherService.match(null);

        assertThat(result.matched()).isFalse();
    }

    // ===================== Parameter extraction =====================

    @Test
    @DisplayName("时间参数: 本月 -> 当前 YYYY-MM")
    void extractCurrentMonth() {
        TemplateMatchResult result = matcherService.match("万科项目这个月租金");

        assertThat(result.matched()).isTrue();
        String expectedMonth = LocalDate.now().format(MONTH_FMT);
        assertThat(result.extractedParams().get("month")).isEqualTo(expectedMonth);
    }

    @Test
    @DisplayName("时间参数: 上月 -> 上月 YYYY-MM")
    void extractLastMonth() {
        TemplateMatchResult result = matcherService.match("万科项目上个月租金");

        assertThat(result.matched()).isTrue();
        String expectedMonth = LocalDate.now().minusMonths(1).format(MONTH_FMT);
        assertThat(result.extractedParams().get("month")).isEqualTo(expectedMonth);
    }

    @Test
    @DisplayName("时间参数: 中文年月 -> YYYY-MM")
    void extractChineseMonth() {
        TemplateMatchResult result = matcherService.match("查询2025年2月，绿萝这个产品的采购详细情况，按采购人、采购金额统计");

        assertThat(result.matched()).isTrue();
        assertThat(result.extractedParams().get("month")).isEqualTo("2025-02");
    }

    @Test
    @DisplayName("项目名提取: '翠湖项目' -> project_name=翠湖")
    void extractProjectName() {
        TemplateMatchResult result = matcherService.match("翠湖项目的绿植有多少");

        assertThat(result.matched()).isTrue();
        assertThat(result.extractedParams()).containsKey("project_name");
        assertThat(result.extractedParams().get("project_name")).isNotBlank();
    }

    // ===================== Suggested questions =====================

    @Test
    @DisplayName("getSuggestedQuestions 返回非空列表")
    void suggestedQuestionsNotEmpty() {
        List<SuggestedQuestion> suggestions = matcherService.getSuggestedQuestions(8);

        assertThat(suggestions).isNotEmpty();
        assertThat(suggestions.size()).isLessThanOrEqualTo(8);
    }

    @Test
    @DisplayName("getSuggestedQuestions 覆盖多个域")
    void suggestedQuestionsCoverMultipleDomains() {
        List<SuggestedQuestion> suggestions = matcherService.getSuggestedQuestions(20);

        long distinctDomains = suggestions.stream()
                .map(SuggestedQuestion::domain)
                .distinct()
                .count();
        assertThat(distinctDomains).isGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("getSuggestedQuestions 每项包含必要字段")
    void suggestedQuestionsHaveRequiredFields() {
        List<SuggestedQuestion> suggestions = matcherService.getSuggestedQuestions(4);

        for (SuggestedQuestion q : suggestions) {
            assertThat(q.templateCode()).isNotBlank();
            assertThat(q.domain()).isNotBlank();
            assertThat(q.question()).isNotBlank();
        }
    }

    @Test
    @DisplayName("getSuggestedQuestions 优先包含已激活固定报表建议")
    void suggestedQuestionsIncludeCurrentPagePhrases() {
        List<SuggestedQuestion> suggestions = matcherService.getSuggestedQuestions(12);

        assertThat(suggestions)
                .extracting(SuggestedQuestion::question)
                .contains("PRS 租赁经营总览", "PRS 租赁报花执行看板")
                .doesNotContain("库存现量", "低库存预警");
        assertThat(suggestions)
                .extracting(SuggestedQuestion::templateCode)
                .allMatch(code -> code.startsWith("PRS-FLOWERBIZ-") || code.startsWith("TPL-"));
    }

    @Test
    @DisplayName("getFixedReportSuggestionsByDomain 返回 PRS 页面化固定报表候选")
    void fixedReportSuggestionsByDomainUseCurrentPagePhrases() {
        List<SuggestedQuestion> suggestions = matcherService.getFixedReportSuggestionsByDomain("flowerbiz", 3);

        assertThat(suggestions)
                .extracting(SuggestedQuestion::question)
                .containsExactly("PRS 租赁经营总览", "PRS 租赁报花执行看板", "PRS 销售坏账与费用看板");
    }

    // ===================== Helper: build mock templates =====================

    /**
     * Build mock templates matching key entries from seed data.
     * Includes project / settlement / task templates plus procurement authority templates.
     */
    private List<Nl2SqlQueryTemplate> buildMockTemplates() {
        List<Nl2SqlQueryTemplate> templates = new ArrayList<>();

        // TPL-01: 项目在摆绿植数
        templates.add(buildTemplate(1L, "TPL-01", "project", null,
                "[\"(项目).*(绿植|花).*(多少|几|数量|总数)\",\"(项目).*(多少|几|数量|总数).*(绿植|花)\"]",
                "[\"XX项目目前有多少在摆绿植？\"]",
                "SELECT project_name, count(*) as 在摆绿植数 FROM v_project_green_current WHERE project_name LIKE CONCAT('%', :project_name, '%') GROUP BY project_name",
                "{\"project_name\":{\"type\":\"string\",\"required\":true}}",
                "v_project_green_current", "项目在摆绿植数", 10));

        // TPL-02: 在服项目总数
        templates.add(buildTemplate(2L, "TPL-02", "project", null,
                "[\"(在服|正常|当前|活跃).*(项目).*(多少|几|总数|数量)\"]",
                "[\"当前在服项目一共多少个？\"]",
                "SELECT project_status_name, count(*) as 项目数 FROM v_project_overview WHERE project_status_name = '正常' GROUP BY project_status_name",
                "{}",
                "v_project_overview", "在服项目总数", 10));

        // TPL-06: 各项目加花排行
        templates.add(buildTemplate(6L, "TPL-06", "flowerbiz", null,
                "[\"(项目|各项目).*(加花|换花|报花).*(排行|排名|最多)\"]",
                "[\"各项目加花次数排行\"]",
                "SELECT project_name, count(*) as 次数 FROM v_flower_biz_detail WHERE biz_type_name = :biz_type AND biz_month = :month GROUP BY project_name ORDER BY 次数 DESC LIMIT :top_n",
                "{\"biz_type\":{\"type\":\"string\",\"default\":\"加花\"},\"month\":{\"type\":\"string\",\"default\":\"CURRENT_MONTH\"},\"top_n\":{\"type\":\"integer\",\"default\":10}}",
                "v_flower_biz_detail", "各项目加花排行", 10));

        // TPL-08: 待审批报花单
        templates.add(buildTemplate(8L, "TPL-08", "flowerbiz", null,
                "[\"(待审批|审核中|未审批).*(报花|业务单)\"]",
                "[\"有多少待审批的报花单？\"]",
                "SELECT biz_code, biz_type_name, project_name, apply_user_name FROM v_flower_biz_detail WHERE biz_status_name = '审核中' ORDER BY apply_time DESC",
                "{}",
                "v_flower_biz_detail", "待审批报花单", 10));

        // TPL-09: 项目月租金查询
        templates.add(buildTemplate(9L, "TPL-09", "settlement", "finance",
                "[\"(项目|XX).*(上月|上个月|本月|这个月).*(租金|应收|收入)\"]",
                "[\"XX项目上个月租金是多少？\"]",
                "SELECT project_name, settlement_month, total_rent FROM v_monthly_settlement WHERE (:project_name IS NULL OR project_name LIKE CONCAT('%', :project_name, '%')) AND settlement_month = :month",
                "{\"project_name\":{\"type\":\"string\"},\"month\":{\"type\":\"string\",\"default\":\"LAST_MONTH\"}}",
                "v_monthly_settlement", "项目月租金查询", 10));

        // TPL-10: 未结算项目
        templates.add(buildTemplate(10L, "TPL-10", "settlement", "finance",
                "[\"(未结算|待结算|没结算).*(项目)\"]",
                "[\"上月未结算的项目有哪些？\"]",
                "SELECT project_name, settlement_month, total_rent, settlement_status_name FROM v_monthly_settlement WHERE settlement_status_name = '待结算' AND settlement_month = :month ORDER BY total_rent DESC",
                "{\"month\":{\"type\":\"string\",\"default\":\"LAST_MONTH\"}}",
                "v_monthly_settlement", "未结算项目", 10));

        // TPL-13: 待处理任务
        templates.add(buildTemplate(13L, "TPL-13", "task", null,
                "[\"(待处理|待办|进行中|未完成).*(任务)\"]",
                "[\"进行中的任务有哪些？\"]",
                "SELECT task_code, task_title, task_type_name, project_name FROM v_task_progress WHERE task_status_name = '进行中' ORDER BY launch_time DESC",
                "{}",
                "v_task_progress", "待处理任务", 10));

        // TPL-15: 养护人负责摆位
        templates.add(buildTemplate(15L, "TPL-15", "curing", null,
                "[\"(养护人|XX).*(负责|管理).*(摆位|多少)\"]",
                "[\"养护人均负责多少摆位？\"]",
                "SELECT curing_user_name, sum(total_position_count) as 负责摆位总数 FROM v_curing_coverage WHERE curing_month = :month GROUP BY curing_user_name ORDER BY 负责摆位总数 DESC",
                "{\"month\":{\"type\":\"string\",\"default\":\"CURRENT_MONTH\"},\"curing_user\":{\"type\":\"string\"}}",
                "v_curing_coverage", "养护人负责摆位", 10));

        // TPL-39: 养护人工作量（dbt flowerbiz 模板）
        templates.add(buildTemplate(39L, "TPL-39", "flowerbiz", null,
                "[\"(养护人|养护师傅|师傅).*(工作量|经手|多少|几次)\",\".*师傅.*(本月|这个月|月).*报花\"]",
                "[\"李师傅本月经手多少报花单\"]",
                "SELECT c.\"养护人\", SUM(c.\"经手单数\") AS \"经手单数\" FROM public.xycyl_ads_flowerbiz_curing_workload c WHERE c.\"养护人\" LIKE '%' || :curing_user_name || '%' AND c.\"业务月份\" = :month GROUP BY c.\"养护人\"",
                "{\"curing_user_name\":{\"type\":\"string\",\"required\":true},\"month\":{\"type\":\"string\",\"default\":\"CURRENT_MONTH\"}}",
                "public.xycyl_ads_flowerbiz_curing_workload", "养护人本月报花工作量", 20));

        // TPL-20: 结算方式分布
        templates.add(buildTemplate(20L, "TPL-20", "project", null,
                "[\"(结算方式|固定月租).*(哪些|分布|项目)\"]",
                "[\"各结算方式的项目分布\"]",
                "SELECT settlement_type_name, count(*) as 项目数 FROM v_project_overview WHERE project_status_name = '正常' GROUP BY settlement_type_name",
                "{}",
                "v_project_overview", "结算方式分布", 10));

        // TPL-31: 某月某产品采购明细
        templates.add(buildTemplate(31L, "TPL-31", "procurement", null,
                "[\".*(采购明细|采购详细情况).*(绿植|产品|物品|商品|采购).*\",\".*(\\\\d{4}年\\\\d{1,2}月|\\\\d{4}-\\\\d{2}).*(采购明细|采购详细情况).*\"]",
                "[\"查询2025年2月绿萝采购明细\"]",
                "SELECT b.purchase_user_name, a.purchase_time, a.good_name, a.good_specs, c.real_purchase_number AS parchase_number, a.parchase_price, ROUND(c.real_purchase_number * a.parchase_price, 2) AS total_amount, a.supply_name, f.code AS biz_code, f.project_name FROM t_purchase_price_item a LEFT JOIN t_purchase_info b ON a.purchase_info_id = b.id LEFT JOIN t_plan_purchase_item c ON c.purchase_price_id = a.id LEFT JOIN t_flower_biz_item d ON d.id = c.flower_item_id LEFT JOIN t_flower_biz_info f ON f.id = d.flower_biz_id WHERE d.status <> -1 AND d.id IS NOT NULL AND c.status <> -1 AND a.good_name = :good_name AND a.purchase_time >= CONCAT(:month, '-01') AND a.purchase_time < DATE_ADD(CONCAT(:month, '-01'), INTERVAL 1 MONTH) ORDER BY a.purchase_time DESC, b.purchase_user_name ASC",
                "{\"month\":{\"type\":\"string\",\"required\":true},\"good_name\":{\"type\":\"string\",\"required\":true}}",
                "authority.procurement.purchase_item_detail", "某月某产品采购明细", 20));

        // TPL-32: 某月某产品按采购人统计采购金额
        templates.add(buildTemplate(32L, "TPL-32", "procurement", null,
                "[\".*(按采购人|采购人).*(采购金额|金额|总价|统计).*\",\".*(采购金额|金额).*(采购人).*统计.*\"]",
                "[\"查询2025年2月绿萝按采购人统计采购金额\"]",
                "SELECT b.purchase_user_name, COUNT(*) AS row_count, SUM(c.real_purchase_number) AS total_quantity, ROUND(SUM(c.real_purchase_number * a.parchase_price), 2) AS purchase_amount FROM t_purchase_price_item a LEFT JOIN t_purchase_info b ON a.purchase_info_id = b.id LEFT JOIN t_plan_purchase_item c ON c.purchase_price_id = a.id LEFT JOIN t_flower_biz_item d ON d.id = c.flower_item_id LEFT JOIN t_flower_biz_info f ON f.id = d.flower_biz_id WHERE d.status <> -1 AND d.id IS NOT NULL AND c.status <> -1 AND a.good_name = :good_name AND a.purchase_time >= CONCAT(:month, '-01') AND a.purchase_time < DATE_ADD(CONCAT(:month, '-01'), INTERVAL 1 MONTH) GROUP BY b.purchase_user_name ORDER BY purchase_amount DESC",
                "{\"month\":{\"type\":\"string\",\"required\":true},\"good_name\":{\"type\":\"string\",\"required\":true}}",
                "authority.procurement.purchase_amount_by_buyer", "某月某产品按采购人统计采购金额", 22));

        // TPL-33: 某月某产品采购详细情况，按采购人/采购金额统计
        templates.add(buildTemplate(33L, "TPL-33", "procurement", null,
                "[\".*(采购详细情况|采购明细).*(按采购人).*(采购金额|金额).*\",\".*(\\\\d{4}年\\\\d{1,2}月|\\\\d{4}-\\\\d{2}).*(产品|物品|商品).*(采购详细情况|采购明细).*(采购人).*(金额).*\"]",
                "[\"查询2025年2月，绿萝这个产品的采购详细情况，按采购人、采购金额统计\"]",
                "SELECT b.purchase_user_name, COUNT(*) AS row_count, SUM(c.real_purchase_number) AS total_quantity, ROUND(SUM(c.real_purchase_number * a.parchase_price), 2) AS purchase_amount FROM t_purchase_price_item a LEFT JOIN t_purchase_info b ON a.purchase_info_id = b.id LEFT JOIN t_plan_purchase_item c ON c.purchase_price_id = a.id LEFT JOIN t_flower_biz_item d ON d.id = c.flower_item_id LEFT JOIN t_flower_biz_info f ON f.id = d.flower_biz_id WHERE d.status <> -1 AND d.id IS NOT NULL AND c.status <> -1 AND a.good_name = :good_name AND a.purchase_time >= CONCAT(:month, '-01') AND a.purchase_time < DATE_ADD(CONCAT(:month, '-01'), INTERVAL 1 MONTH) GROUP BY b.purchase_user_name ORDER BY purchase_amount DESC",
                "{\"month\":{\"type\":\"string\",\"required\":true},\"good_name\":{\"type\":\"string\",\"required\":true}}",
                "authority.procurement.purchase_amount_by_buyer", "某月某产品采购详细情况按采购人金额统计", 30));

        // TPL-34: 全年各绿植采购情况（Trino 联邦入口）
        templates.add(buildTemplate(34L, "TPL-34", "procurement", null,
                "[\".*(\\\\d{4}年).*(各个|各|所有).*(绿植|产品|物品|商品).*(采购情况|采购统计|采购).*\",\".*(各个|各|所有).*(绿植|产品|物品|商品).*(采购情况|采购统计|采购).*\"]",
                "[\"看下2026年各个绿植的采购情况\"]",
                "SELECT a.good_name AS \"绿植\", COALESCE(NULLIF(a.good_specs, ''), '未填') AS \"规格\", COUNT(*) AS \"采购明细行数\", COUNT(DISTINCT b.id) AS \"采购单数\", SUM(COALESCE(c.real_purchase_number, 0)) AS \"采购数量\", ROUND(SUM(COALESCE(c.real_purchase_number, 0) * COALESCE(a.parchase_price, 0)), 2) AS \"采购金额\", MIN(a.purchase_time) AS \"最早采购时间\", MAX(a.purchase_time) AS \"最近采购时间\" FROM mysql.rs_cloud_flower.t_purchase_price_item a LEFT JOIN mysql.rs_cloud_flower.t_purchase_info b ON a.purchase_info_id = b.id LEFT JOIN mysql.rs_cloud_flower.t_plan_purchase_item c ON c.purchase_price_id = a.id LEFT JOIN mysql.rs_cloud_flower.t_flower_biz_item d ON d.id = c.flower_item_id LEFT JOIN mysql.rs_cloud_flower.t_flower_biz_info f ON f.id = d.flower_biz_id WHERE d.status <> -1 AND d.id IS NOT NULL AND c.status <> -1 AND a.purchase_time >= TIMESTAMP ':year-01-01 00:00:00' AND a.purchase_time < TIMESTAMP ':year-01-01 00:00:00' + INTERVAL '1' YEAR GROUP BY a.good_name, COALESCE(NULLIF(a.good_specs, ''), '未填') ORDER BY \"采购金额\" DESC LIMIT 100",
                "{\"year\":{\"type\":\"integer\",\"required\":true}}",
                "mysql.rs_cloud_flower.t_purchase_price_item", "全年各绿植采购情况", 35));

        // TPL-52: 年度销售情况（dbt flowerbiz mart）
        templates.add(buildTemplate(52L, "TPL-52", "flowerbiz", null,
                "[\".*(20\\\\d{2}|\\\\d{4}年|今年|去年).*(销售|售花|卖花).*(情况|统计|汇总|金额|收入).*\",\".*(销售|售花|卖花).*(20\\\\d{2}|\\\\d{4}年|今年|去年).*(情况|统计|汇总|金额|收入).*\"]",
                "[\"查询2026年销售情况\"]",
                "SELECT s.\"业务月份\", s.\"项目\", ROUND(SUM(CAST(NULLIF(CAST(s.\"销售金额全口径\" AS VARCHAR), '') AS DECIMAL(18,2))), 2) AS \"销售金额\", SUM(CAST(NULLIF(CAST(s.\"销售单数\" AS VARCHAR), '') AS DECIMAL(18,2))) AS \"销售单数\", SUM(CAST(NULLIF(CAST(s.\"赠送单数\" AS VARCHAR), '') AS DECIMAL(18,2))) AS \"赠送单数\", ROUND(SUM(CAST(NULLIF(CAST(s.\"赠送成本全口径\" AS VARCHAR), '') AS DECIMAL(18,2))), 2) AS \"赠送成本\" FROM public.xycyl_ads_flowerbiz_sale_summary s WHERE s.\"年份\" = :year GROUP BY s.\"业务月份\", s.\"项目\" ORDER BY s.\"业务月份\", \"销售金额\" DESC LIMIT 200",
                "{\"year\":{\"type\":\"integer\",\"required\":true}}",
                "public.xycyl_ads_flowerbiz_sale_summary", "年度销售情况汇总", 35));

        // TPL-56: 年度财务数据统计（dbt finance ADS）
        templates.add(buildTemplate(56L, "TPL-56", "finance", null,
                "[\".*(20\\\\d{2}|\\\\d{4}年|今年|去年).*(财务|收款|回款|应收|实收).*(数据|情况|统计|汇总|总览).*\",\".*(财务|收款|回款|应收|实收).*(20\\\\d{2}|\\\\d{4}年|今年|去年).*(数据|情况|统计|汇总|总览).*\"]",
                "[\"2026年财务数据统计下\"]",
                "WITH settlement AS (SELECT s.\"业务月份\" AS finance_month, COUNT(DISTINCT s.\"项目\") AS settlement_project_count, SUM(s.\"结算单数\") AS settlement_record_count, ROUND(SUM(s.\"应收折前\"), 2) AS receivable_before_discount, ROUND(SUM(s.\"折后实收\"), 2) AS discounted_receivable, ROUND(SUM(s.\"已回款\"), 2) AS paid_amount, ROUND(SUM(s.\"未回款\"), 2) AS unpaid_amount FROM public.xycyl_ads_finance_month_settlement s WHERE s.\"业务月份\" LIKE CAST(:year AS VARCHAR) || '-%' GROUP BY s.\"业务月份\"), collection AS (SELECT c.\"收款月份\" AS finance_month, ROUND(SUM(c.\"收款金额\"), 2) AS collection_amount, SUM(c.\"收款单数\") AS collection_record_count, COUNT(DISTINCT c.\"项目\") AS collection_project_count FROM public.xycyl_ads_finance_collection c WHERE c.\"收款月份\" LIKE CAST(:year AS VARCHAR) || '-%' GROUP BY c.\"收款月份\") SELECT COALESCE(s.finance_month, c.finance_month) AS \"财务月份\", COALESCE(s.settlement_project_count, 0) AS \"结算项目数\", COALESCE(s.receivable_before_discount, 0) AS \"应收折前\", COALESCE(c.collection_amount, 0) AS \"收款金额\" FROM settlement s FULL OUTER JOIN collection c ON s.finance_month = c.finance_month ORDER BY \"财务月份\"",
                "{\"year\":{\"type\":\"integer\",\"required\":true}}",
                "public.xycyl_ads_finance_month_settlement", "年度财务数据统计", 42));

        // TPL-57: 年度凭证数据统计（dbt finance voucher ADS）
        templates.add(buildTemplate(57L, "TPL-57", "finance", null,
                "[\".*(20\\\\d{2}|\\\\d{4}年|今年|去年).*(凭证|会计凭证).*(数据|情况|统计|汇总|总览|数量|金额).*\",\".*(凭证|会计凭证).*(20\\\\d{2}|\\\\d{4}年|今年|去年).*(数据|情况|统计|汇总|总览|数量|金额).*\"]",
                "[\"2026年凭证的数据统计下\"]",
                "SELECT v.\"会计月份\", v.\"业务类型\", v.\"凭证状态\", SUM(v.\"凭证数\") AS \"凭证数\", SUM(v.\"分录数\") AS \"分录数\", ROUND(SUM(v.\"借方金额\"), 2) AS \"借方金额\", ROUND(SUM(v.\"贷方金额\"), 2) AS \"贷方金额\", ROUND(SUM(v.\"借贷差额\"), 2) AS \"借贷差额\" FROM public.xycyl_ads_finance_voucher_monthly v WHERE v.\"会计月份\" LIKE CAST(:year AS VARCHAR) || '-%' GROUP BY v.\"会计月份\", v.\"业务类型\", v.\"凭证状态\" ORDER BY v.\"会计月份\", v.\"业务类型\", v.\"凭证状态\"",
                "{\"year\":{\"type\":\"integer\",\"required\":true}}",
                "public.xycyl_ads_finance_voucher_monthly", "年度凭证数据统计", 45));

        // TPL-53: 库存现量（Trino 联邦 MySQL 弱路径）
        templates.add(buildTemplate(53L, "TPL-53", "warehouse", null,
                "[\".*(库存现量|当前库存|库存现状|库存总览|库存看板).*\", \".*(展示|查看|查询|统计).*(库存).*\"]",
                "[\"展示当前库存现状\"]",
                "SELECT COALESCE(storehouse_name, '未填') AS \"库房\", CAST(good_price_id AS VARCHAR) AS \"SKU\", COALESCE(good_name, '未填') AS \"物品名称\", COALESCE(good_norms, '') AS \"规格\", COALESCE(good_specs, '') AS \"属性\", COALESCE(good_unit, '') AS \"单位\", SUM(COALESCE(good_number, 0)) AS \"可用库存\", ROUND(SUM(CAST(COALESCE(good_number, 0) AS DOUBLE) * CAST(COALESCE(out_cost, 0) AS DOUBLE)), 2) AS \"库存成本\", COUNT(*) AS \"库存行数\", MAX(update_time) AS \"最近更新时间\" FROM mysql.rs_cloud_flower.s_stock_info WHERE (del_flag IS NULL OR del_flag = '0') GROUP BY COALESCE(storehouse_name, '未填'), good_price_id, COALESCE(good_name, '未填'), COALESCE(good_norms, ''), COALESCE(good_specs, ''), COALESCE(good_unit, '') ORDER BY 7 DESC, 8 DESC LIMIT 100",
                "{}",
                "mysql.rs_cloud_flower.s_stock_info", "库存现量按库房和 SKU 汇总", 46));

        // TPL-54: 低库存预警（Trino 联邦 MySQL 弱路径）
        templates.add(buildTemplate(54L, "TPL-54", "warehouse", null,
                "[\".*(低库存|缺货).*(预警|告警|清单|列表|情况).*\", \".*(库存).*(不足|偏低|低于|小于).*(预警|告警|清单|列表|情况)?.*\"]",
                "[\"查看低库存SKU清单\"]",
                "SELECT COALESCE(storehouse_name, '未填') AS \"库房\", CAST(good_price_id AS VARCHAR) AS \"SKU\", COALESCE(good_name, '未填') AS \"物品名称\", COALESCE(good_norms, '') AS \"规格\", COALESCE(good_specs, '') AS \"属性\", COALESCE(good_unit, '') AS \"单位\", SUM(COALESCE(good_number, 0)) AS \"可用库存\", ROUND(SUM(CAST(COALESCE(good_number, 0) AS DOUBLE) * CAST(COALESCE(out_cost, 0) AS DOUBLE)), 2) AS \"库存成本\", MAX(update_time) AS \"最近更新时间\" FROM mysql.rs_cloud_flower.s_stock_info WHERE (del_flag IS NULL OR del_flag = '0') GROUP BY COALESCE(storehouse_name, '未填'), good_price_id, COALESCE(good_name, '未填'), COALESCE(good_norms, ''), COALESCE(good_specs, ''), COALESCE(good_unit, '') HAVING SUM(COALESCE(good_number, 0)) <= 2 ORDER BY 7 ASC, 3 LIMIT 100",
                "{}",
                "mysql.rs_cloud_flower.s_stock_info", "低库存 SKU 清单", 47));

        // TPL-55: 某月报花单据明细（业务对象真实字段）
        templates.add(buildTemplate(55L, "TPL-55", "flowerbiz", null,
                "[\".*(\\\\d{4}年\\\\d{1,2}月|\\\\d{4}-\\\\d{2}).*(报花单据|报花单|报花业务单).*(查|查询|看|查看|列表|明细|清单)?.*\",\".*(报花单据|报花单|报花业务单).*(\\\\d{4}年\\\\d{1,2}月|\\\\d{4}-\\\\d{2}).*(查|查询|看|查看|列表|明细|清单)?.*\"]",
                "[\"2026年3月的报花单据查下\"]",
                "SELECT CAST(f.id AS VARCHAR) AS \"报花单ID\", f.code AS \"报花单号\", f.customer_name AS \"客户\", f.project_manage_name AS \"项目经理\", f.apply_use_name AS \"发起人\", f.create_time AS \"创建时间\" FROM mysql.rs_cloud_flower.t_flower_biz_info f WHERE (f.del_flag IS NULL OR f.del_flag = '0') AND f.create_time >= CAST(:month || '-01 00:00:00' AS TIMESTAMP) AND f.create_time < CAST(:month || '-01 00:00:00' AS TIMESTAMP) + INTERVAL '1' MONTH ORDER BY f.create_time DESC LIMIT 100",
                "{\"month\":{\"type\":\"string\",\"required\":true}}",
                "mysql.rs_cloud_flower.t_flower_biz_info", "某月报花单据明细（业务对象）", 55));

        return templates;
    }

    private Nl2SqlQueryTemplate buildTemplate(Long id, String templateCode, String domain,
                                               String roleHint, String intentPatterns,
                                               String questionSamples, String sqlTemplate,
                                               String parameters, String targetView,
                                               String description, int priority) {
        Nl2SqlQueryTemplate t = new Nl2SqlQueryTemplate();
        t.setId(id);
        t.setTemplateCode(templateCode);
        t.setDomain(domain);
        t.setRoleHint(roleHint);
        t.setIntentPatterns(intentPatterns);
        t.setQuestionSamples(questionSamples);
        t.setSqlTemplate(sqlTemplate);
        t.setParameters(parameters);
        t.setTargetView(targetView);
        t.setDescription(description);
        t.setPriority(priority);
        t.setIsActive(true);
        return t;
    }
}
