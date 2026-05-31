package com.yuzhi.dts.copilot.ai.service.platform;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class IndicatorCatalogMapperTest {

    @Test
    void mapsPlatformIndicatorDtoToCatalogEntryForRoutingAndTrace() {
        PlatformIndicatorDto dto = new PlatformIndicatorDto(
                "uuid-cash-in",
                "prs.finance.cash_in",
                "现金流入",
                "finance",
                "finance",
                "统计已确认回款金额",
                "sum(received_amount)",
                "已发布",
                "v3",
                "现金,回款；收入",
                "[\"project_id\",\"month_id\"]",
                "biz_date",
                "month",
                "SUM",
                "received_amount",
                "HIGH",
                "财务部");

        IndicatorCatalogEntry entry = IndicatorCatalogMapper.fromPlatform(dto);

        assertThat(entry.id()).isEqualTo("uuid-cash-in");
        assertThat(entry.code()).isEqualTo("prs.finance.cash_in");
        assertThat(entry.name()).isEqualTo("现金流入");
        assertThat(entry.domain()).isEqualTo("finance");
        assertThat(entry.definition()).isEqualTo("统计已确认回款金额");
        assertThat(entry.expressionSql()).isEqualTo("sum(received_amount)");
        assertThat(entry.version()).isEqualTo("v3");
        assertThat(entry.tags()).containsExactly("现金", "回款", "收入");
        assertThat(entry.dimensionFields()).containsExactly("project_id", "month_id");
        assertThat(entry.matchKeywords())
                .contains("现金流入", "prs.finance.cash_in", "现金", "回款", "收入", "project_id", "month_id");
    }

    @Test
    void safelySplitsLooseTextFieldsWithoutThrowing() {
        PlatformIndicatorDto dto = new PlatformIndicatorDto(
                "uuid-lease",
                "prs.flowerbiz.lease_amount",
                "租金金额",
                "flowerbiz",
                null,
                "租金口径",
                "sum(lease_amount)",
                "已发布",
                "v1",
                "租赁;金额,经营",
                "project_name;month_id",
                null,
                null,
                null,
                null,
                null,
                null);

        IndicatorCatalogEntry entry = IndicatorCatalogMapper.fromPlatform(dto);

        assertThat(entry.domain()).isEqualTo("flowerbiz");
        assertThat(entry.tags()).containsExactly("租赁", "金额", "经营");
        assertThat(entry.dimensionFields()).containsExactly("project_name", "month_id");
        assertThat(entry.matchKeywords()).contains("租金金额", "prs.flowerbiz.lease_amount", "租赁", "project_name");
    }
}
