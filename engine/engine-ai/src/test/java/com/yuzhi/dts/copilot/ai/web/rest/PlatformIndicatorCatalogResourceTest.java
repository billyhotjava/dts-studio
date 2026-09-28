package com.yuzhi.dts.copilot.ai.web.rest;

import static org.assertj.core.api.Assertions.assertThat;

import com.yuzhi.dts.copilot.ai.service.platform.IndicatorCatalogEntry;
import com.yuzhi.dts.copilot.ai.service.platform.IndicatorCatalogStore;
import com.yuzhi.dts.copilot.ai.service.platform.IndicatorCatalogSyncProperties;
import com.yuzhi.dts.copilot.ai.service.platform.IndicatorCatalogSyncService;
import com.yuzhi.dts.copilot.ai.service.platform.PlatformIndicatorCatalogClient;
import com.yuzhi.dts.copilot.ai.service.platform.PlatformIndicatorDto;
import com.yuzhi.dts.copilot.ai.service.platform.PlatformIndicatorPage;
import com.yuzhi.dts.copilot.ai.service.platform.SyncResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

class PlatformIndicatorCatalogResourceTest {

    @Test
    void exposesManualPlatformIndicatorSyncEndpointForGovernanceBackflow() throws Exception {
        RequestMapping mapping = PlatformIndicatorCatalogResource.class.getAnnotation(RequestMapping.class);
        assertThat(mapping).isNotNull();
        assertThat(mapping.value()).containsExactly("/api/copilot/platform-indicators");
        assertThat(PlatformIndicatorCatalogResource.class.getDeclaredMethod("status")
                .getAnnotation(GetMapping.class).value()).containsExactly("/status");
        assertThat(PlatformIndicatorCatalogResource.class.getDeclaredMethod("sync", String.class)
                .getAnnotation(PostMapping.class).value()).containsExactly("/sync");
    }

    @Test
    void syncRefreshesPublishedIndicatorsAndReportsBackflowState() {
        FakePlatformIndicatorCatalogClient client = new FakePlatformIndicatorCatalogClient(
                PlatformIndicatorPage.of(List.of(
                        dto("id-approved", "codex.published.backflow", "已审核回流指标", "PUBLISHED"),
                        dto("id-draft", "codex.draft.blocked", "未审核草稿指标", "DRAFT")),
                        0,
                        100,
                        1));
        IndicatorCatalogStore store = new IndicatorCatalogStore();
        IndicatorCatalogSyncService syncService = new IndicatorCatalogSyncService(
                client,
                store,
                new IndicatorCatalogSyncProperties(true, 100, 10));
        PlatformIndicatorCatalogResource resource = new PlatformIndicatorCatalogResource(syncService, store, "admin-secret");

        ResponseEntity<Map<String, Object>> response = resource.sync("admin-secret");

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody())
                .containsEntry("status", "SUCCESS")
                .containsEntry("fetched", 2)
                .containsEntry("catalogEntryCount", 1)
                .containsEntry("stale", false);
        assertThat(response.getBody().get("caliberChangedCodes")).asList().isEmpty();
        assertThat(store.all())
                .extracting(IndicatorCatalogEntry::code)
                .containsExactly("codex.published.backflow");
    }

    @Test
    void statusShowsStaleCacheAfterFailedBackflowRefresh() {
        FakePlatformIndicatorCatalogClient client = new FakePlatformIndicatorCatalogClient(
                PlatformIndicatorPage.of(List.of(dto("id-old", "codex.old.metric", "旧指标", "PUBLISHED")), 0, 100, 1));
        IndicatorCatalogStore store = new IndicatorCatalogStore();
        IndicatorCatalogSyncService syncService = new IndicatorCatalogSyncService(
                client,
                store,
                new IndicatorCatalogSyncProperties(true, 100, 10));
        PlatformIndicatorCatalogResource resource = new PlatformIndicatorCatalogResource(syncService, store, "admin-secret");
        syncService.refresh();
        client.failWith(new IllegalStateException("HTTP 503"));

        resource.sync("admin-secret");
        ResponseEntity<Map<String, Object>> status = resource.status();

        assertThat(status.getBody())
                .containsEntry("entryCount", 1)
                .containsEntry("stale", true)
                .containsEntry("lastStatus", SyncResult.Status.FAILED.name())
                .containsEntry("error", "HTTP 503");
    }

    @Test
    void syncRejectsInvalidAdminSecretBeforeCallingPlatform() {
        FakePlatformIndicatorCatalogClient client = new FakePlatformIndicatorCatalogClient(
                PlatformIndicatorPage.of(List.of(dto("id-approved", "codex.published.backflow", "已审核回流指标", "PUBLISHED")), 0, 100, 1));
        IndicatorCatalogStore store = new IndicatorCatalogStore();
        IndicatorCatalogSyncService syncService = new IndicatorCatalogSyncService(
                client,
                store,
                new IndicatorCatalogSyncProperties(true, 100, 10));
        PlatformIndicatorCatalogResource resource = new PlatformIndicatorCatalogResource(syncService, store, "admin-secret");

        ResponseEntity<Map<String, Object>> response = resource.sync("wrong-secret");

        assertThat(response.getStatusCode().value()).isEqualTo(403);
        assertThat(response.getBody()).containsEntry("error", "Invalid admin secret");
        assertThat(store.all()).isEmpty();
    }

    private static PlatformIndicatorDto dto(String id, String code, String name, String status) {
        return new PlatformIndicatorDto(
                id,
                code,
                name,
                "finance",
                "finance",
                name + "定义",
                "sum(amount)",
                status,
                "v1",
                "copilot,governance",
                "month_id",
                "biz_date",
                "month",
                "SUM",
                "amount",
                "MEDIUM",
                "owner");
    }

    private static final class FakePlatformIndicatorCatalogClient implements PlatformIndicatorCatalogClient {
        private final PlatformIndicatorPage page;
        private RuntimeException failure;

        private FakePlatformIndicatorCatalogClient(PlatformIndicatorPage page) {
            this.page = page;
        }

        @Override
        public PlatformIndicatorPage listPublishedIndicators(int page, int size) {
            if (failure != null) {
                throw failure;
            }
            return this.page;
        }

        private void failWith(RuntimeException failure) {
            this.failure = failure;
        }
    }
}
