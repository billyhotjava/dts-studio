package com.yuzhi.dts.copilot.ai.service.platform;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class IndicatorCatalogStoreTest {

    private final IndicatorCatalogStore store = new IndicatorCatalogStore();

    @Test
    void exposesEmptyStoreAsNotReady() {
        assertThat(store.isReady()).isFalse();
        assertThat(store.all()).isEmpty();
        assertThat(store.status().entryCount()).isZero();
    }

    @Test
    void atomicallyReplacesSnapshotAndSupportsLookups() {
        IndicatorCatalogEntry first = entry("uuid-1", "prs.finance.cash_in", "现金流入", "v1");
        IndicatorCatalogEntry second = entry("uuid-2", "prs.flowerbiz.lease_amount", "租金金额", "v2");

        SyncResult result = SyncResult.success(2, 2, 0, 0, List.of());
        store.replaceAll(List.of(first, second), result);

        assertThat(store.isReady()).isTrue();
        assertThat(store.all()).containsExactly(first, second);
        assertThat(store.byId("uuid-1")).contains(first);
        assertThat(store.byCode("prs.flowerbiz.lease_amount")).contains(second);
        assertThat(store.status().entryCount()).isEqualTo(2);
        assertThat(store.status().lastStatus()).isEqualTo(SyncResult.Status.SUCCESS);
    }

    @Test
    void recordsFailedRefreshWithoutClearingStaleSnapshot() {
        IndicatorCatalogEntry first = entry("uuid-1", "prs.finance.cash_in", "现金流入", "v1");
        store.replaceAll(List.of(first), SyncResult.success(1, 1, 0, 0, List.of()));

        store.recordRefreshResult(SyncResult.failed(0, "HTTP 503"));

        assertThat(store.isReady()).isTrue();
        assertThat(store.all()).containsExactly(first);
        assertThat(store.status().entryCount()).isEqualTo(1);
        assertThat(store.status().stale()).isTrue();
        assertThat(store.status().lastStatus()).isEqualTo(SyncResult.Status.FAILED);
        assertThat(store.status().lastError()).isEqualTo("HTTP 503");
    }

    private static IndicatorCatalogEntry entry(String id, String code, String name, String version) {
        return new IndicatorCatalogEntry(
                id,
                code,
                name,
                "finance",
                "finance",
                name + "定义",
                "sum(amount)",
                "已发布",
                version,
                List.of(name),
                List.of("month_id"),
                "biz_date",
                "month",
                "SUM",
                "amount",
                "MEDIUM",
                "owner",
                List.of(name, code, "month_id"));
    }
}
