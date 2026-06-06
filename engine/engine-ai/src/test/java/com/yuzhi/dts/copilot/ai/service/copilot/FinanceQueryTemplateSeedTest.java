package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class FinanceQueryTemplateSeedTest {

    private static final String INITIAL_CHANGELOG_PATH =
            "config/liquibase/changelog/v1_0_0_030__finance_year_summary_query_template.xml";
    private static final String RUNTIME_FIX_CHANGELOG_PATH =
            "config/liquibase/changelog/v1_0_0_031__finance_year_summary_query_template_runtime_fix.xml";
    private static final String DATASET_FIX_CHANGELOG_PATH =
            "config/liquibase/changelog/v1_0_0_032__finance_year_summary_query_template_dataset_fix.xml";
    private static final String ADS_QUERY_CHANGELOG_PATH =
            "config/liquibase/changelog/v1_0_0_033__finance_ads_query_templates.xml";

    @Test
    void masterIncludesFinanceYearSummaryTemplateChangelog() throws IOException {
        String master = readResource("config/liquibase/master.xml");

        assertThat(master).contains(INITIAL_CHANGELOG_PATH);
        assertThat(master).contains(RUNTIME_FIX_CHANGELOG_PATH);
        assertThat(master).contains(DATASET_FIX_CHANGELOG_PATH);
        assertThat(master).contains(ADS_QUERY_CHANGELOG_PATH);
    }

    @Test
    void financeYearSummaryTemplateUsesWarehouseAdsTables() throws IOException {
        String changelog = readResource(ADS_QUERY_CHANGELOG_PATH);

        assertThat(changelog).contains("TPL-56");
        assertThat(changelog).contains("public.xycyl_ads_finance_month_settlement");
        assertThat(changelog).contains("public.xycyl_ads_finance_collection");
        assertThat(changelog).contains("s.\"业务月份\" LIKE CAST(:year AS VARCHAR) || '-%'");
        assertThat(changelog).doesNotContain("mysql.rs_cloud_flower.a_month_accounting");
        assertThat(changelog).doesNotContain("mysql.rs_cloud_flower.a_collection_record");
    }

    @Test
    void financeVoucherYearSummaryTemplateUsesWarehouseVoucherAdsTable() throws IOException {
        String changelog = readResource(ADS_QUERY_CHANGELOG_PATH);

        assertThat(changelog).contains("TPL-57");
        assertThat(changelog).contains("public.xycyl_ads_finance_voucher_monthly");
        assertThat(changelog).contains("v.\"会计月份\" LIKE CAST(:year AS VARCHAR) || '-%'");
        assertThat(changelog).contains("\"凭证数\"");
        assertThat(changelog).contains("\"借贷差额\"");
        assertThat(changelog).doesNotContain("mysql.rs_cloud_flower.f_voucher");
        assertThat(changelog).doesNotContain("mysql.rs_cloud_flower.f_voucher_item");
    }

    private static String readResource(String path) throws IOException {
        try (InputStream input = FinanceQueryTemplateSeedTest.class
                .getClassLoader()
                .getResourceAsStream(path)) {
            assertThat(input).as("classpath resource %s", path).isNotNull();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
