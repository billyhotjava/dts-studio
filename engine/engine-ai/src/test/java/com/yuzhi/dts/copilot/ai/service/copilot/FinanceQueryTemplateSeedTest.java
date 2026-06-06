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

    @Test
    void masterIncludesFinanceYearSummaryTemplateChangelog() throws IOException {
        String master = readResource("config/liquibase/master.xml");

        assertThat(master).contains(INITIAL_CHANGELOG_PATH);
        assertThat(master).contains(RUNTIME_FIX_CHANGELOG_PATH);
        assertThat(master).contains(DATASET_FIX_CHANGELOG_PATH);
    }

    @Test
    void financeYearSummaryTemplateUsesLiveMysqlTablesAndTrinoCompatibleSql() throws IOException {
        String changelog = readResource(DATASET_FIX_CHANGELOG_PATH);

        assertThat(changelog).contains("TPL-56");
        assertThat(changelog).contains("WITH months AS");
        assertThat(changelog).contains("FROM (VALUES");
        assertThat(changelog).contains("mysql.rs_cloud_flower.a_month_accounting");
        assertThat(changelog).contains("mysql.rs_cloud_flower.a_collection_record");
        assertThat(changelog).contains("m.settlement_year = :year");
        assertThat(changelog).contains("TIMESTAMP ':year-01-01 00:00:00'");
        assertThat(changelog).doesNotContain("::numeric");
        assertThat(changelog).doesNotContain("to_char");
        assertThat(changelog).doesNotContain("UNNEST");
        assertThat(changelog).doesNotContain("FULL OUTER JOIN");
        assertThat(changelog).doesNotContain("public.xycyl_ads_finance_month_settlement");
        assertThat(changelog).doesNotContain("public.xycyl_ads_finance_collection");
        assertThat(changelog).doesNotContain("public.xycyl_ads_finance_summary");
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
