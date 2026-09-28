package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class FlowerbizBusinessObjectQueryTemplateSeedTest {

    private static final String CHANGELOG_PATH =
            "config/liquibase/changelog/v1_0_0_028__flowerbiz_business_object_query_templates.xml";

    @Test
    void masterIncludesFlowerbizBusinessObjectTemplateChangelog() throws IOException {
        String master = readResource("config/liquibase/master.xml");

        assertThat(master).contains(CHANGELOG_PATH);
    }

    @Test
    void flowerbizMonthlyOrderTemplateUsesRealMysqlColumnsAndTrinoTimeLiterals() throws IOException {
        String changelog = readResource(CHANGELOG_PATH);

        assertThat(changelog).contains("TPL-55");
        assertThat(changelog).contains("mysql.rs_cloud_flower.t_flower_biz_info");
        assertThat(changelog).contains("customer_name");
        assertThat(changelog).contains("project_manage_name");
        assertThat(changelog).contains("apply_use_name");
        assertThat(changelog).contains("CAST(:month || '-01 00:00:00' AS TIMESTAMP)");
        assertThat(changelog).doesNotContain("curr_customer_name");
        assertThat(changelog).doesNotContain("proj_manager_name");
        assertThat(changelog).doesNotContain("apply_user_name");
    }

    private static String readResource(String path) throws IOException {
        try (InputStream input = FlowerbizBusinessObjectQueryTemplateSeedTest.class
                .getClassLoader()
                .getResourceAsStream(path)) {
            assertThat(input).as("classpath resource %s", path).isNotNull();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
