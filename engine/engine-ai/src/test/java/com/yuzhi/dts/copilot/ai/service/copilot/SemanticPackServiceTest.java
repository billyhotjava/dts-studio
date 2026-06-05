package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class SemanticPackServiceTest {

    @Test
    void shouldLoadProcurementSemanticPackContext() {
        SemanticPackService service = new SemanticPackService(new ObjectMapper());

        service.init();

        assertThat(service.getDomains()).contains("procurement");
        assertThat(service.getContextForDomain("procurement"))
                .contains("采购明细")
                .contains("t_purchase_price_item")
                .contains("good_name")
                .contains("采购人");
        assertThat(service.getSynonyms("procurement"))
                .containsEntry("采购人", "purchase_user_name")
                .containsEntry("产品", "good_name");
    }

    @Test
    void shouldLoadFlowerbizSemanticPackFromDbtPublicModels() {
        SemanticPackService service = new SemanticPackService(new ObjectMapper());

        service.init();

        String context = service.getContextForDomain("flowerbiz");
        assertThat(service.getDomains()).contains("flowerbiz");
        assertThat(context)
                .contains("public.xycyl_ads_flowerbiz_lease_summary")
                .contains("public.xycyl_ads_flowerbiz_pending")
                .contains("PostgreSQL")
                .doesNotContain("v_flower_biz_detail")
                .doesNotContain("DATE_FORMAT")
                .doesNotContain("CURDATE")
                .doesNotContain("DATEDIFF");
    }

    @Test
    void shouldLoadProjectSemanticPackFromSprint25DbtPublicModels() {
        SemanticPackService service = new SemanticPackService(new ObjectMapper());

        service.init();

        String context = service.getContextForDomain("project");
        assertThat(service.getDomains()).contains("project");
        assertThat(context)
                .contains("public.xycyl_ads_project_overview")
                .contains("public.xycyl_dws_project_green_monthly")
                .contains("public.xycyl_ads_contract_expiry_alert")
                .contains("rent_amount_adminweb_sum")
                .contains("cost_amount_adminweb_sum")
                .contains("real_good_number_adminweb_sum")
                .contains("PostgreSQL")
                .doesNotContain("v_project_overview")
                .doesNotContain("v_project_green_current")
                .doesNotContain("DATEDIFF")
                .doesNotContain("CURDATE");
    }

    @Test
    void shouldLoadWarehouseInventorySemanticPack() {
        SemanticPackService service = new SemanticPackService(new ObjectMapper());

        service.init();

        String context = service.getContextForDomain("warehouse");
        assertThat(service.getDomains()).contains("warehouse");
        assertThat(context)
                .contains("mysql.rs_cloud_flower.s_stock_info")
                .contains("库存现量")
                .contains("低库存预警")
                .contains("good_price_id")
                .contains("三段式");
        assertThat(service.getSynonyms("warehouse"))
                .containsEntry("库存", "good_number")
                .containsEntry("SKU", "good_price_id");
    }
}
