package com.yuzhi.dts.copilot.analytics.service.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yuzhi.dts.copilot.analytics.domain.AnalyticsDatabase;
import com.yuzhi.dts.copilot.analytics.domain.AnalyticsReportTemplate;
import com.yuzhi.dts.copilot.analytics.repository.AnalyticsDatabaseRepository;
import com.yuzhi.dts.copilot.analytics.service.DatasetQueryService;
import com.yuzhi.dts.copilot.analytics.service.DatasetQueryService.DatasetConstraints;
import com.yuzhi.dts.copilot.analytics.service.DatasetQueryService.DatasetResult;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultFixedReportExecutionServiceTest {

    @Mock
    private AnalyticsDatabaseRepository databaseRepository;

    @Mock
    private DatasetQueryService datasetQueryService;

    @Captor
    private ArgumentCaptor<String> sqlCaptor;

    @Captor
    private ArgumentCaptor<List<Object>> bindingsCaptor;

    @Test
    void shouldExecuteProcurementSummaryAgainstConfiguredBusinessDatabase() throws Exception {
        AnalyticsDatabase database = new AnalyticsDatabase();
        database.setId(7L);
        database.setName("园林业务库");
        when(databaseRepository.findFirstByNameIgnoreCase("园林业务库")).thenReturn(Optional.of(database));
        when(datasetQueryService.runNative(eq(7L), any(String.class), any(DatasetConstraints.class), any()))
                .thenReturn(new DatasetResult(
                        List.of(List.of("2025-02-28", "邹顿顿", new BigDecimal("11979.50"))),
                        List.of(
                                Map.of("name", "purchaseDate", "display_name", "采购时间", "base_type", "type/Date"),
                                Map.of("name", "purchaseUserName", "display_name", "采购人", "base_type", "type/Text"),
                                Map.of("name", "totalAmount", "display_name", "总金额", "base_type", "type/Number")),
                        List.of(),
                        "Asia/Shanghai"));

        DefaultFixedReportExecutionService service =
                new DefaultFixedReportExecutionService(databaseRepository, datasetQueryService);

        Optional<FixedReportExecutionService.ExecutionResult> result = service.execute(
                procurementSummaryTemplate(),
                Map.of(
                        "purchaseUserId", "42",
                        "startDate", "2025-02-01",
                        "endDate", "2025-02-28"),
                new ReportExecutionPlanService.ReportExecutionPlan(
                        ReportExecutionPlanService.Route.AUTHORITY_SQL,
                        "PROC-SUPPLIER-AMOUNT-RANK",
                        "authority.procurement",
                        "authority.procurement.purchase_summary",
                        "template uses realtime procurement summary sql",
                        "SQL",
                        "REALTIME"));

        assertThat(result).isPresent();
        assertThat(result.get().databaseId()).isEqualTo(7L);
        assertThat(result.get().databaseName()).isEqualTo("园林业务库");
        assertThat(result.get().rowCount()).isEqualTo(1);
        assertThat(result.get().columns()).extracting(FixedReportExecutionService.PreviewColumn::key)
                .containsExactly("purchaseDate", "purchaseUserName", "totalAmount");
        assertThat(result.get().rows()).containsExactly(Map.of(
                "purchaseDate", "2025-02-28",
                "purchaseUserName", "邹顿顿",
                "totalAmount", new BigDecimal("11979.50")));

        verify(datasetQueryService).runNative(eq(7L), sqlCaptor.capture(), any(DatasetConstraints.class), bindingsCaptor.capture());
        assertThat(sqlCaptor.getValue()).contains("FROM t_purchase_price_item a");
        assertThat(sqlCaptor.getValue()).contains("GROUP BY DATE_FORMAT( a.purchase_time, '%Y-%m-%d' ), b.purchase_user_id, b.purchase_user_name");
        assertThat(bindingsCaptor.getValue()).containsExactly("42", "2025-02-01", "2025-02-28");
    }

    @Test
    void shouldExecuteWarehouseStockOverviewAgainstConfiguredBusinessDatabase() throws Exception {
        AnalyticsDatabase database = new AnalyticsDatabase();
        database.setId(7L);
        database.setName("园林业务库");
        when(databaseRepository.findFirstByNameIgnoreCase("园林业务库")).thenReturn(Optional.of(database));
        when(datasetQueryService.runNative(eq(7L), any(String.class), any(DatasetConstraints.class), any()))
                .thenReturn(new DatasetResult(
                        List.of(List.of("大兴基地", "垃圾袋", "描述:厚（50个/包）#规格:120X140cm", 9906, new BigDecimal("0.4500"), new BigDecimal("13.00"))),
                        List.of(
                                Map.of("name", "storehouseName", "display_name", "所属库房", "base_type", "type/Text"),
                                Map.of("name", "goodName", "display_name", "物品名称", "base_type", "type/Text"),
                                Map.of("name", "goodSpecs", "display_name", "物品属性", "base_type", "type/Text"),
                                Map.of("name", "goodNumber", "display_name", "可用数量", "base_type", "type/Number"),
                                Map.of("name", "outCost", "display_name", "出库单价", "base_type", "type/Number"),
                                Map.of("name", "salePrice", "display_name", "销售单价", "base_type", "type/Number")),
                        List.of(),
                        "Asia/Shanghai"));

        DefaultFixedReportExecutionService service =
                new DefaultFixedReportExecutionService(databaseRepository, datasetQueryService);

        Optional<FixedReportExecutionService.ExecutionResult> result = service.execute(
                warehouseStockOverviewTemplate(),
                Map.of(
                        "storehouseInfoId", "1001",
                        "goodType", "4",
                        "goodName", "垃圾袋",
                        "goodSpecs", "120X140cm",
                        "passNumber", "10",
                        "underNumber", "10000"),
                new ReportExecutionPlanService.ReportExecutionPlan(
                        ReportExecutionPlanService.Route.AUTHORITY_SQL,
                        "WH-STOCK-OVERVIEW",
                        "authority.inventory",
                        "authority.inventory.stock_overview",
                        "template uses realtime stock overview sql",
                        "SQL",
                        "REALTIME"));

        assertThat(result).isPresent();
        assertThat(result.get().databaseId()).isEqualTo(7L);
        assertThat(result.get().databaseName()).isEqualTo("园林业务库");
        assertThat(result.get().rowCount()).isEqualTo(1);
        assertThat(result.get().columns()).extracting(FixedReportExecutionService.PreviewColumn::key)
                .containsExactly("storehouseName", "goodName", "goodSpecs", "goodNumber", "outCost", "salePrice");
        assertThat(result.get().rows()).containsExactly(Map.of(
                "storehouseName", "大兴基地",
                "goodName", "垃圾袋",
                "goodSpecs", "描述:厚（50个/包）#规格:120X140cm",
                "goodNumber", 9906,
                "outCost", new BigDecimal("0.4500"),
                "salePrice", new BigDecimal("13.00")));

        verify(datasetQueryService).runNative(eq(7L), sqlCaptor.capture(), any(DatasetConstraints.class), bindingsCaptor.capture());
        assertThat(sqlCaptor.getValue()).contains("FROM s_stock_info a");
        assertThat(sqlCaptor.getValue()).contains("LEFT JOIN b_goods_price gp ON gp.id = a.good_price_id");
        assertThat(sqlCaptor.getValue()).contains("a.del_flag = '0'");
        assertThat(bindingsCaptor.getValue()).containsExactly("1001", "4", "%垃圾袋%", "%120X140cm%", "10", "10000");
    }

    @Test
    void shouldExecuteWarehouseLowStockAlertAgainstConfiguredBusinessDatabase() throws Exception {
        AnalyticsDatabase database = new AnalyticsDatabase();
        database.setId(7L);
        database.setName("园林业务库");
        when(databaseRepository.findFirstByNameIgnoreCase("园林业务库")).thenReturn(Optional.of(database));
        when(datasetQueryService.runNative(eq(7L), any(String.class), any(DatasetConstraints.class), any()))
                .thenReturn(new DatasetResult(
                        List.of(List.of("中石油缓养库", "白塑料盆", "小", "10X12", 0, new BigDecimal("2.3000"), new BigDecimal("4.50"), "李四")),
                        List.of(
                                Map.of("name", "storehouseName", "display_name", "所属库房", "base_type", "type/Text"),
                                Map.of("name", "goodName", "display_name", "物品名称", "base_type", "type/Text"),
                                Map.of("name", "goodNorms", "display_name", "物品规格", "base_type", "type/Text"),
                                Map.of("name", "goodSpecs", "display_name", "物品属性", "base_type", "type/Text"),
                                Map.of("name", "goodNumber", "display_name", "可用数量", "base_type", "type/Number"),
                                Map.of("name", "outCost", "display_name", "出库单价", "base_type", "type/Number"),
                                Map.of("name", "salePrice", "display_name", "销售单价", "base_type", "type/Number"),
                                Map.of("name", "leaderUserName", "display_name", "负责人", "base_type", "type/Text")),
                        List.of(),
                        "Asia/Shanghai"));

        DefaultFixedReportExecutionService service =
                new DefaultFixedReportExecutionService(databaseRepository, datasetQueryService);

        Optional<FixedReportExecutionService.ExecutionResult> result = service.execute(
                warehouseLowStockAlertTemplate(),
                Map.of(
                        "storehouseInfoId", "1001",
                        "goodType", "2",
                        "goodName", "白塑料盆",
                        "goodSpecs", "10X12",
                        "underNumber", "10",
                        "status", "0"),
                new ReportExecutionPlanService.ReportExecutionPlan(
                        ReportExecutionPlanService.Route.AUTHORITY_SQL,
                        "WH-LOW-STOCK-ALERT",
                        "authority.inventory",
                        "authority.inventory.low_stock_alert",
                        "template uses realtime low stock alert sql",
                        "SQL",
                        "REALTIME"));

        assertThat(result).isPresent();
        assertThat(result.get().databaseId()).isEqualTo(7L);
        assertThat(result.get().databaseName()).isEqualTo("园林业务库");
        assertThat(result.get().rowCount()).isEqualTo(1);
        assertThat(result.get().columns()).extracting(FixedReportExecutionService.PreviewColumn::key)
                .containsExactly("storehouseName", "goodName", "goodNorms", "goodSpecs", "goodNumber", "outCost", "salePrice", "leaderUserName");
        assertThat(result.get().rows()).containsExactly(Map.of(
                "storehouseName", "中石油缓养库",
                "goodName", "白塑料盆",
                "goodNorms", "小",
                "goodSpecs", "10X12",
                "goodNumber", 0,
                "outCost", new BigDecimal("2.3000"),
                "salePrice", new BigDecimal("4.50"),
                "leaderUserName", "李四"));

        verify(datasetQueryService).runNative(eq(7L), sqlCaptor.capture(), any(DatasetConstraints.class), bindingsCaptor.capture());
        assertThat(sqlCaptor.getValue()).contains("FROM s_stock_info a");
        assertThat(sqlCaptor.getValue()).contains("a.good_number <= ?");
        assertThat(sqlCaptor.getValue()).contains("a.status = ?");
        assertThat(bindingsCaptor.getValue()).containsExactly("1001", "2", "%白塑料盆%", "%10X12%", "10", "0");
    }

    @Test
    void shouldExecuteFinanceSettlementSummaryAgainstConfiguredBusinessDatabase() throws Exception {
        AnalyticsDatabase database = new AnalyticsDatabase();
        database.setId(7L);
        database.setName("园林业务库");
        when(databaseRepository.findFirstByNameIgnoreCase("园林业务库")).thenReturn(Optional.of(database));
        when(datasetQueryService.runNative(eq(7L), any(String.class), any(DatasetConstraints.class), any()))
                .thenReturn(new DatasetResult(
                        List.of(List.of(
                                "202504",
                                "国企大厦",
                                "申请售花",
                                "主营业务收入/销售收入",
                                "月季",
                                new BigDecimal("8800.0000"),
                                "公司",
                                "收入/项目销售额挂帐",
                                "张三",
                                "演示备注")),
                        List.of(
                                Map.of("name", "accountPeriod", "display_name", "财务账期", "base_type", "type/Text"),
                                Map.of("name", "projectName", "display_name", "项目", "base_type", "type/Text"),
                                Map.of("name", "bizTypeName", "display_name", "流程类型", "base_type", "type/Text"),
                                Map.of("name", "subjectFullName", "display_name", "财务科目", "base_type", "type/Text"),
                                Map.of("name", "feeName", "display_name", "收支名称", "base_type", "type/Text"),
                                Map.of("name", "feeNumber", "display_name", "金额", "base_type", "type/Number"),
                                Map.of("name", "feeBelongName", "display_name", "成本归属", "base_type", "type/Text"),
                                Map.of("name", "settleHandleName", "display_name", "结算处理", "base_type", "type/Text"),
                                Map.of("name", "feeUserName", "display_name", "关联人员", "base_type", "type/Text"),
                                Map.of("name", "remark", "display_name", "备注", "base_type", "type/Text")),
                        List.of(),
                        "Asia/Shanghai"));

        DefaultFixedReportExecutionService service =
                new DefaultFixedReportExecutionService(databaseRepository, datasetQueryService);

        Optional<FixedReportExecutionService.ExecutionResult> result = service.execute(
                financeSettlementSummaryTemplate(),
                Map.of(
                        "accountPeriod", "202504",
                        "projectId", "2001",
                        "feeUserId", "99",
                        "status", "1"),
                new ReportExecutionPlanService.ReportExecutionPlan(
                        ReportExecutionPlanService.Route.AUTHORITY_SQL,
                        "FIN-AR-OVERVIEW",
                        "authority.finance",
                        "authority.finance.settlement_summary",
                        "template uses realtime settlement summary sql",
                        "SQL",
                        "REALTIME"));

        assertThat(result).isPresent();
        assertThat(result.get().databaseId()).isEqualTo(7L);
        assertThat(result.get().databaseName()).isEqualTo("园林业务库");
        assertThat(result.get().rowCount()).isEqualTo(1);
        assertThat(result.get().columns()).extracting(FixedReportExecutionService.PreviewColumn::key)
                .containsExactly(
                        "accountPeriod",
                        "projectName",
                        "bizTypeName",
                        "subjectFullName",
                        "feeName",
                        "feeNumber",
                        "feeBelongName",
                        "settleHandleName",
                        "feeUserName",
                        "remark");
        assertThat(result.get().rows()).containsExactly(Map.of(
                "accountPeriod", "202504",
                "projectName", "国企大厦",
                "bizTypeName", "申请售花",
                "subjectFullName", "主营业务收入/销售收入",
                "feeName", "月季",
                "feeNumber", new BigDecimal("8800.0000"),
                "feeBelongName", "公司",
                "settleHandleName", "收入/项目销售额挂帐",
                "feeUserName", "张三",
                "remark", "演示备注"));

        verify(datasetQueryService).runNative(eq(7L), sqlCaptor.capture(), any(DatasetConstraints.class), bindingsCaptor.capture());
        assertThat(sqlCaptor.getValue()).contains("FROM f_settled_items");
        assertThat(sqlCaptor.getValue()).contains("status > 0");
        assertThat(sqlCaptor.getValue()).contains("ORDER BY a.account_period ASC");
        assertThat(bindingsCaptor.getValue()).containsExactly("202504", "2001", "99", "1");
    }

    @Test
    void shouldExecuteFinanceAdvanceRequestStatusAgainstConfiguredBusinessDatabase() throws Exception {
        AnalyticsDatabase database = new AnalyticsDatabase();
        database.setId(7L);
        database.setName("园林业务库");
        when(databaseRepository.findFirstByNameIgnoreCase("园林业务库")).thenReturn(Optional.of(database));
        when(datasetQueryService.runNative(eq(7L), any(String.class), any(DatasetConstraints.class), any()))
                .thenReturn(new DatasetResult(
                        List.of(List.of(
                                "武延娟",
                                "YZ202602100273",
                                "备用金",
                                new BigDecimal("1000.0000"),
                                new BigDecimal("1000.0000"),
                                new BigDecimal("800.0000"),
                                "已付款",
                                "有",
                                "未提供",
                                "武延娟于20260210152437发起预支",
                                "2026-02-10 15:24:38",
                                "2026-02-10 18:00:00")),
                        List.of(
                                Map.of("name", "applyUserName", "display_name", "申请人", "base_type", "type/Text"),
                                Map.of("name", "code", "display_name", "单号", "base_type", "type/Text"),
                                Map.of("name", "typeName", "display_name", "申请类型", "base_type", "type/Text"),
                                Map.of("name", "totalAmount", "display_name", "申请金额", "base_type", "type/Number"),
                                Map.of("name", "payAmount", "display_name", "付款金额", "base_type", "type/Number"),
                                Map.of("name", "offsetTotalAmount", "display_name", "抵消金额", "base_type", "type/Number"),
                                Map.of("name", "statusName", "display_name", "状态", "base_type", "type/Text"),
                                Map.of("name", "invoiceStatusName", "display_name", "发票", "base_type", "type/Text"),
                                Map.of("name", "invoiceOfferName", "display_name", "发票提供", "base_type", "type/Text"),
                                Map.of("name", "title", "display_name", "标题", "base_type", "type/Text"),
                                Map.of("name", "applyTime", "display_name", "申请时间", "base_type", "type/Text"),
                                Map.of("name", "payTime", "display_name", "付款日期", "base_type", "type/Text")),
                        List.of(),
                        "Asia/Shanghai"));

        DefaultFixedReportExecutionService service =
                new DefaultFixedReportExecutionService(databaseRepository, datasetQueryService);

        Optional<FixedReportExecutionService.ExecutionResult> result = service.execute(
                financeAdvanceRequestStatusTemplate(),
                Map.of(
                        "code", "YZ202602100273",
                        "status", "7",
                        "applyUserId", "1001"),
                new ReportExecutionPlanService.ReportExecutionPlan(
                        ReportExecutionPlanService.Route.AUTHORITY_SQL,
                        "FIN-ADVANCE-REQUEST-STATUS",
                        "authority.finance",
                        "authority.finance.advance_request_status",
                        "template uses realtime advance request status sql",
                        "SQL",
                        "REALTIME"));

        assertThat(result).isPresent();
        assertThat(result.get().databaseId()).isEqualTo(7L);
        assertThat(result.get().databaseName()).isEqualTo("园林业务库");
        assertThat(result.get().rowCount()).isEqualTo(1);
        assertThat(result.get().columns()).extracting(FixedReportExecutionService.PreviewColumn::key)
                .containsExactly(
                        "applyUserName",
                        "code",
                        "typeName",
                        "totalAmount",
                        "payAmount",
                        "offsetTotalAmount",
                        "statusName",
                        "invoiceStatusName",
                        "invoiceOfferName",
                        "title",
                        "applyTime",
                        "payTime");
        assertThat(result.get().rows()).containsExactly(Map.ofEntries(
                Map.entry("applyUserName", "武延娟"),
                Map.entry("code", "YZ202602100273"),
                Map.entry("typeName", "备用金"),
                Map.entry("totalAmount", new BigDecimal("1000.0000")),
                Map.entry("payAmount", new BigDecimal("1000.0000")),
                Map.entry("offsetTotalAmount", new BigDecimal("800.0000")),
                Map.entry("statusName", "已付款"),
                Map.entry("invoiceStatusName", "有"),
                Map.entry("invoiceOfferName", "未提供"),
                Map.entry("title", "武延娟于20260210152437发起预支"),
                Map.entry("applyTime", "2026-02-10 15:24:38"),
                Map.entry("payTime", "2026-02-10 18:00:00")));

        verify(datasetQueryService).runNative(eq(7L), sqlCaptor.capture(), any(DatasetConstraints.class), bindingsCaptor.capture());
        assertThat(sqlCaptor.getValue()).contains("FROM f_advance_info a");
        assertThat(sqlCaptor.getValue()).contains("a.code LIKE ?");
        assertThat(sqlCaptor.getValue()).contains("a.status = ?");
        assertThat(sqlCaptor.getValue()).contains("a.apply_user_id = ?");
        assertThat(bindingsCaptor.getValue()).containsExactly("%YZ202602100273%", "7", "1001");
    }

    private static AnalyticsReportTemplate procurementSummaryTemplate() {
        AnalyticsReportTemplate template = new AnalyticsReportTemplate();
        template.setTemplateCode("PROC-SUPPLIER-AMOUNT-RANK");
        template.setName("采购汇总");
        template.setDomain("采购");
        template.setCategory("明细");
        template.setDataSourceType("SQL");
        template.setTargetObject("authority.procurement.purchase_summary");
        template.setRefreshPolicy("REALTIME");
        template.setSpecJson("""
                {
                  "templateCode":"PROC-SUPPLIER-AMOUNT-RANK",
                  "reportType":"fixed",
                  "displayType":"table",
                  "queryContract":{
                    "sourceType":"AUTHORITY_SQL",
                    "targetObject":"authority.procurement.purchase_summary",
                    "databaseName":"园林业务库"
                  },
                  "placeholderReviewRequired":false
                }
                """);
        return template;
    }

    private static AnalyticsReportTemplate warehouseStockOverviewTemplate() {
        AnalyticsReportTemplate template = new AnalyticsReportTemplate();
        template.setTemplateCode("WH-STOCK-OVERVIEW");
        template.setName("库存现量");
        template.setDomain("仓库");
        template.setCategory("明细");
        template.setDataSourceType("SQL");
        template.setTargetObject("authority.inventory.stock_overview");
        template.setRefreshPolicy("REALTIME");
        template.setSpecJson("""
                {
                  "templateCode":"WH-STOCK-OVERVIEW",
                  "reportType":"fixed",
                  "displayType":"table",
                  "queryContract":{
                    "sourceType":"AUTHORITY_SQL",
                    "targetObject":"authority.inventory.stock_overview",
                    "databaseName":"园林业务库"
                  },
                  "placeholderReviewRequired":false
                }
                """);
        return template;
    }

    private static AnalyticsReportTemplate financeSettlementSummaryTemplate() {
        AnalyticsReportTemplate template = new AnalyticsReportTemplate();
        template.setTemplateCode("FIN-AR-OVERVIEW");
        template.setName("财务结算汇总");
        template.setDomain("财务");
        template.setCategory("明细");
        template.setDataSourceType("SQL");
        template.setTargetObject("authority.finance.settlement_summary");
        template.setRefreshPolicy("REALTIME");
        template.setSpecJson("""
                {
                  "templateCode":"FIN-AR-OVERVIEW",
                  "reportType":"fixed",
                  "displayType":"table",
                  "queryContract":{
                    "sourceType":"AUTHORITY_SQL",
                    "targetObject":"authority.finance.settlement_summary",
                    "databaseName":"园林业务库"
                  },
                  "placeholderReviewRequired":false
                }
                """);
        return template;
    }

    private static AnalyticsReportTemplate warehouseLowStockAlertTemplate() {
        AnalyticsReportTemplate template = new AnalyticsReportTemplate();
        template.setTemplateCode("WH-LOW-STOCK-ALERT");
        template.setName("库存现量-低库存预警");
        template.setDomain("仓库");
        template.setCategory("预警");
        template.setDataSourceType("SQL");
        template.setTargetObject("authority.inventory.low_stock_alert");
        template.setRefreshPolicy("REALTIME");
        template.setSpecJson("""
                {
                  "templateCode":"WH-LOW-STOCK-ALERT",
                  "reportType":"fixed",
                  "displayType":"alert-list",
                  "queryContract":{
                    "sourceType":"AUTHORITY_SQL",
                    "targetObject":"authority.inventory.low_stock_alert",
                    "databaseName":"园林业务库"
                  },
                  "placeholderReviewRequired":false
                }
                """);
        return template;
    }

    private static AnalyticsReportTemplate financeAdvanceRequestStatusTemplate() {
        AnalyticsReportTemplate template = new AnalyticsReportTemplate();
        template.setTemplateCode("FIN-ADVANCE-REQUEST-STATUS");
        template.setName("预支申请");
        template.setDomain("财务");
        template.setCategory("状态");
        template.setDataSourceType("SQL");
        template.setTargetObject("authority.finance.advance_request_status");
        template.setRefreshPolicy("REALTIME");
        template.setSpecJson("""
                {
                  "templateCode":"FIN-ADVANCE-REQUEST-STATUS",
                  "reportType":"fixed",
                  "displayType":"status-board",
                  "queryContract":{
                    "sourceType":"AUTHORITY_SQL",
                    "targetObject":"authority.finance.advance_request_status",
                    "databaseName":"园林业务库"
                  },
                  "placeholderReviewRequired":false
                }
                """);
        return template;
    }
}
