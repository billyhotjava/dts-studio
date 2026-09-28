package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ProcurementQueryTemplateSeedTest {

    private static final String CHANGELOG_PATH =
            "config/liquibase/changelog/v1_0_0_024__procurement_federated_query_templates.xml";

    @Test
    void masterIncludesFederatedProcurementTemplateChangelog() throws IOException {
        String master = readResource("config/liquibase/master.xml");

        assertThat(master).contains(CHANGELOG_PATH);
    }

    @Test
    void federatedGreenPurchaseTemplateUsesAuthorizedMysqlCatalogOnly() throws IOException {
        String changelog = readResource(CHANGELOG_PATH);

        assertThat(changelog).contains("TPL-34");
        assertThat(changelog).contains("mysql.rs_cloud_flower.t_purchase_price_item");
        assertThat(changelog).contains("mysql.rs_cloud_flower.t_purchase_info");
        assertThat(changelog).contains("mysql.rs_cloud_flower.t_plan_purchase_item");
        assertThat(changelog).contains("mysql.rs_cloud_flower.t_flower_biz_item");
        assertThat(changelog).contains("TIMESTAMP ':year-01-01 00:00:00'");
        assertThat(changelog).doesNotContain("PRODUCTION");
        assertThat(changelog).doesNotContain("FLOWER_BIZ");
        assertThat(changelog).doesNotContain("PRS_PROCUREMENT_DELIVERY_RECORD");
        assertThat(changelog).doesNotContain("TRY_TO_NUMBER");
    }

    private static String readResource(String path) throws IOException {
        try (InputStream input = ProcurementQueryTemplateSeedTest.class
                .getClassLoader()
                .getResourceAsStream(path)) {
            assertThat(input).as("classpath resource %s", path).isNotNull();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
