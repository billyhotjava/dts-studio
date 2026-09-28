package com.yuzhi.dts.copilot.ai.service.platform;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class IndicatorCatalogSyncServiceTest {

    @Test
    void refreshAggregatesPublishedIndicatorsAcrossPages() {
        FakePlatformIndicatorCatalogClient client = new FakePlatformIndicatorCatalogClient(
                PlatformIndicatorPage.of(List.of(dto("id-1", "prs.finance.cash_in", "现金流入", "v1")), 0, 2, 2),
                PlatformIndicatorPage.of(List.of(dto("id-2", "prs.finance.cash_out", "现金流出", "v1")), 1, 2, 2));
        IndicatorCatalogStore store = new IndicatorCatalogStore();
        IndicatorCatalogSyncService service = new IndicatorCatalogSyncService(
                client,
                store,
                new IndicatorCatalogSyncProperties(true, 100, 10));

        SyncResult result = service.refresh();

        assertThat(result.status()).isEqualTo(SyncResult.Status.SUCCESS);
        assertThat(result.fetched()).isEqualTo(2);
        assertThat(result.added()).isEqualTo(2);
        assertThat(client.requests()).containsExactly("0:100", "1:100");
        assertThat(store.all())
                .extracting(IndicatorCatalogEntry::code)
                .containsExactly("prs.finance.cash_in", "prs.finance.cash_out");
    }

    @Test
    void failedRefreshKeepsPreviousSnapshotAsStaleCache() {
        FakePlatformIndicatorCatalogClient client = new FakePlatformIndicatorCatalogClient(
                PlatformIndicatorPage.of(List.of(dto("id-1", "prs.finance.cash_in", "现金流入", "v1")), 0, 1, 1));
        IndicatorCatalogStore store = new IndicatorCatalogStore();
        IndicatorCatalogSyncService service = new IndicatorCatalogSyncService(
                client,
                store,
                new IndicatorCatalogSyncProperties(true, 100, 10));
        service.refresh();

        client.failWith(new IllegalStateException("HTTP 503"));
        SyncResult result = service.refresh();

        assertThat(result.status()).isEqualTo(SyncResult.Status.FAILED);
        assertThat(result.error()).contains("HTTP 503");
        assertThat(store.all())
                .extracting(IndicatorCatalogEntry::code)
                .containsExactly("prs.finance.cash_in");
        assertThat(store.status().stale()).isTrue();
    }

    @Test
    void detectsCaliberVersionChangesWhenReplacingSnapshot() {
        FakePlatformIndicatorCatalogClient client = new FakePlatformIndicatorCatalogClient(
                PlatformIndicatorPage.of(List.of(dto("id-1", "prs.finance.cash_in", "现金流入", "v1")), 0, 1, 1));
        IndicatorCatalogStore store = new IndicatorCatalogStore();
        IndicatorCatalogSyncService service = new IndicatorCatalogSyncService(
                client,
                store,
                new IndicatorCatalogSyncProperties(true, 100, 10));
        service.refresh();

        client.replacePages(
                PlatformIndicatorPage.of(List.of(dto("id-1", "prs.finance.cash_in", "现金流入", "v2")), 0, 1, 1));
        SyncResult result = service.refresh();

        assertThat(result.status()).isEqualTo(SyncResult.Status.SUCCESS);
        assertThat(result.caliberChangedCodes()).containsExactly("prs.finance.cash_in");
        assertThat(store.byCode("prs.finance.cash_in")).get()
                .extracting(IndicatorCatalogEntry::version)
                .isEqualTo("v2");
    }

    @Test
    void disabledScheduledRefreshSkipsClientCalls() {
        FakePlatformIndicatorCatalogClient client = new FakePlatformIndicatorCatalogClient(
                PlatformIndicatorPage.of(List.of(dto("id-1", "prs.finance.cash_in", "现金流入", "v1")), 0, 1, 1));
        IndicatorCatalogStore store = new IndicatorCatalogStore();
        IndicatorCatalogSyncService service = new IndicatorCatalogSyncService(
                client,
                store,
                new IndicatorCatalogSyncProperties(false, 100, 10));

        SyncResult result = service.scheduledRefresh();

        assertThat(result.status()).isEqualTo(SyncResult.Status.SKIPPED);
        assertThat(client.requests()).isEmpty();
        assertThat(store.all()).isEmpty();
    }

    private static PlatformIndicatorDto dto(String id, String code, String name, String version) {
        return new PlatformIndicatorDto(
                id,
                code,
                name,
                "finance",
                "finance",
                name + "定义",
                "sum(amount)",
                "已发布",
                version,
                "现金,财务",
                "month_id",
                "biz_date",
                "month",
                "SUM",
                "amount",
                "MEDIUM",
                "owner");
    }

    private static final class FakePlatformIndicatorCatalogClient implements PlatformIndicatorCatalogClient {
        private final List<String> requests = new ArrayList<>();
        private final List<PlatformIndicatorPage> pages = new ArrayList<>();
        private RuntimeException failure;

        private FakePlatformIndicatorCatalogClient(PlatformIndicatorPage... pages) {
            replacePages(pages);
        }

        @Override
        public PlatformIndicatorPage listPublishedIndicators(int page, int size) {
            requests.add(page + ":" + size);
            if (failure != null) {
                throw failure;
            }
            if (page >= pages.size()) {
                return PlatformIndicatorPage.of(List.of(), page, size, pages.size());
            }
            return pages.get(page);
        }

        private List<String> requests() {
            return requests;
        }

        private void failWith(RuntimeException failure) {
            this.failure = failure;
        }

        private void replacePages(PlatformIndicatorPage... replacement) {
            this.failure = null;
            this.requests.clear();
            this.pages.clear();
            this.pages.addAll(List.of(replacement));
        }
    }
}
