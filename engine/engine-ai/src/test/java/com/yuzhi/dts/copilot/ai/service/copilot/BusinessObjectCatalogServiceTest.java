package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;

import com.yuzhi.dts.copilot.ai.service.copilot.BusinessObjectCatalogService.BusinessObjectEntry;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class BusinessObjectCatalogServiceTest {

    private final BusinessObjectCatalogService catalog = new BusinessObjectCatalogService();

    @Test
    void exposesProcurementDeliveryRecordAsBusinessObject() {
        Optional<BusinessObjectEntry> match = catalog.findBestMatch("采购配送记录各状态有多少", "purchase_inventory");

        assertThat(match).isPresent();
        assertThat(match.get().objectCode()).isEqualTo("prs.procurement.delivery_record");
        assertThat(match.get().responseKind()).isEqualTo("BUSINESS_INSIGHT");
        assertThat(match.get().dataSurface()).isEqualTo("L0_BUSINESS_OBJECT_PROFILE");
        assertThat(match.get().pagePath()).isEqualTo("采购管理 > 配送记录");
        assertThat(match.get().keyFields())
                .contains("标题", "状态", "类型", "起始", "目的地", "配送人", "配送时间", "接收人", "接收时间");
        assertThat(match.get().sourceRefs())
                .contains("business-object:prs.procurement.delivery_record", "ods-profile:procurement.delivery_record");
    }

    @Test
    void coversCoreBusinessDomainsFromAdminApiAndAdminWeb() {
        assertThat(catalog.entries())
                .extracting(BusinessObjectEntry::domain)
                .contains("flowerbiz", "purchase_inventory", "project", "finance", "warehouse");
        assertThat(catalog.entries())
                .extracting(BusinessObjectEntry::objectCode)
                .contains(
                        "prs.flowerbiz.biz_order",
                        "prs.flowerbiz.position_placement",
                        "prs.procurement.plan_purchase",
                        "prs.project.project_site",
                        "prs.finance.settlement",
                        "prs.finance.bank_statement",
                        "prs.warehouse.stock_info",
                        "prs.warehouse.inout_record");
    }

    @Test
    void warehouseStockQuestionsExposeRealInventoryTablesAndApis() {
        Optional<BusinessObjectEntry> match = catalog.findBestMatch("库存现量按仓库统计", "warehouse");

        assertThat(match).isPresent();
        assertThat(match.get().objectCode()).isEqualTo("prs.warehouse.stock_info");
        assertThat(match.get().pagePath()).isEqualTo("仓库管理 > 库存管理 > 库存");
        assertThat(match.get().sourceRefs())
                .contains(
                        "mysql-table:s_stock_info",
                        "mysql-table:s_stock_item",
                        "mysql-table:s_storehouse_info",
                        "adminapi:/rs-flowers-base/store/info/listPage");
        assertThat(match.get().qualityNotes())
                .anySatisfy(note -> assertThat(note).contains("s_stock_info"));
    }

    @Test
    void warehouseInOutQuestionsExposeMovementTablesAndApis() {
        Optional<BusinessObjectEntry> match = catalog.findBestMatch("本月出入库记录按仓库统计", "warehouse");

        assertThat(match).isPresent();
        assertThat(match.get().objectCode()).isEqualTo("prs.warehouse.inout_record");
        assertThat(match.get().pagePath()).isEqualTo("仓库管理 > 库存管理 > 出入库记录");
        assertThat(match.get().sourceRefs())
                .contains(
                        "mysql-table:t_warehousing_info",
                        "mysql-table:t_warehousing_item",
                        "mysql-table:t_ex_warehouse_info",
                        "mysql-table:t_ex_warehouse_item",
                        "adminapi:/rs-flowers-base/store/info/listInOutItemPage");
        assertThat(match.get().qualityNotes())
                .anySatisfy(note -> assertThat(note).contains("t_warehousing_info/item"));
    }

    @Test
    void matchesFlowerBizDocumentQuestions() {
        Optional<BusinessObjectEntry> match = catalog.findBestMatch("报花单据状态分布", "flowerbiz");

        assertThat(match).isPresent();
        assertThat(match.get().objectCode()).isEqualTo("prs.flowerbiz.biz_order");
        assertThat(match.get().pagePath()).isEqualTo("报花管理 > 报花单据");
        assertThat(match.get().keyFields())
                .contains("项目点", "业务类型", "单号", "标题", "租金日期", "发起人", "完成时间");
    }

    @Test
    void matchesProjectSiteQuestions() {
        Optional<BusinessObjectEntry> match = catalog.findBestMatch("项目点状态统计", "project");

        assertThat(match).isPresent();
        assertThat(match.get().objectCode()).isEqualTo("prs.project.project_site");
        assertThat(match.get().pagePath()).isEqualTo("项目点管理 > 项目点");
        assertThat(match.get().keyFields())
                .contains("项目点编码", "项目点名称", "项目状态", "项目经理", "合同名称");
    }

    @Test
    void matchesFinanceBankStatementQuestions() {
        Optional<BusinessObjectEntry> match = catalog.findBestMatch("银行流水未核对有多少", "finance");

        assertThat(match).isPresent();
        assertThat(match.get().objectCode()).isEqualTo("prs.finance.bank_statement");
        assertThat(match.get().pagePath()).isEqualTo("财务管理 > 银行流水");
        assertThat(match.get().keyFields())
                .contains("交易日期", "银行名称", "收入金额", "支出金额", "对方户名", "状态");
    }

    @Test
    void ignoresUnrelatedQuestionsWithoutKeywordEvidence() {
        Optional<BusinessObjectEntry> match = catalog.findBestMatch("租赁收入 TOP", "flowerbiz");

        assertThat(match).isEmpty();
    }
}
