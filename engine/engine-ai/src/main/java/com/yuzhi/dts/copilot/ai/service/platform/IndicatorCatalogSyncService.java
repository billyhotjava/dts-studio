package com.yuzhi.dts.copilot.ai.service.platform;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class IndicatorCatalogSyncService {

    private static final Logger LOG = LoggerFactory.getLogger(IndicatorCatalogSyncService.class);

    private final PlatformIndicatorCatalogClient client;
    private final IndicatorCatalogStore store;
    private final IndicatorCatalogSyncProperties properties;

    public IndicatorCatalogSyncService(
            PlatformIndicatorCatalogClient client,
            IndicatorCatalogStore store,
            IndicatorCatalogSyncProperties properties) {
        this.client = client;
        this.store = store;
        this.properties = properties;
    }

    @PostConstruct
    void warmUp() {
        if (properties.enabled()) {
            refresh();
        }
    }

    @Scheduled(
            fixedDelayString = "${copilot.platform.indicator.sync.interval-ms:3600000}",
            initialDelayString = "${copilot.platform.indicator.sync.initial-delay-ms:15000}")
    public SyncResult scheduledRefresh() {
        if (!properties.enabled()) {
            SyncResult result = SyncResult.skipped("platform indicator sync disabled");
            store.recordRefreshResult(result);
            return result;
        }
        return refresh();
    }

    public SyncResult refresh() {
        List<PlatformIndicatorDto> fetched = new ArrayList<>();
        try {
            int page = 0;
            int totalPages = 1;
            while (page < totalPages && page < properties.maxPages()) {
                PlatformIndicatorPage response = client.listPublishedIndicators(page, properties.pageSize());
                fetched.addAll(response.items());
                totalPages = response.totalPages() > 0 ? response.totalPages() : page + 1;
                page += 1;
            }
            List<IndicatorCatalogEntry> entries = fetched.stream()
                    .filter(IndicatorCatalogMapper::isPublished)
                    .map(IndicatorCatalogMapper::fromPlatform)
                    .toList();
            SyncResult result = buildSuccessResult(entries);
            store.replaceAll(entries, result);
            LOG.info("Platform indicator catalog synced: fetched={}, entries={}, changed={}",
                    fetched.size(), entries.size(), result.caliberChangedCodes().size());
            return result;
        } catch (RuntimeException e) {
            SyncResult result = SyncResult.failed(fetched.size(), StringUtils.hasText(e.getMessage())
                    ? e.getMessage()
                    : e.getClass().getSimpleName());
            store.recordRefreshResult(result);
            LOG.warn("Platform indicator catalog sync failed, stale cache kept: {}", result.error());
            return result;
        }
    }

    private SyncResult buildSuccessResult(List<IndicatorCatalogEntry> nextEntries) {
        Map<String, IndicatorCatalogEntry> previousByCode = byCode(store.all());
        Map<String, IndicatorCatalogEntry> nextByCode = byCode(nextEntries);
        int added = 0;
        int updated = 0;
        List<String> caliberChangedCodes = new ArrayList<>();
        for (Map.Entry<String, IndicatorCatalogEntry> entry : nextByCode.entrySet()) {
            IndicatorCatalogEntry previous = previousByCode.get(entry.getKey());
            if (previous == null) {
                added += 1;
                continue;
            }
            if (!same(previous.version(), entry.getValue().version())) {
                updated += 1;
                caliberChangedCodes.add(entry.getKey());
            }
        }
        int removed = 0;
        for (String code : previousByCode.keySet()) {
            if (!nextByCode.containsKey(code)) {
                removed += 1;
            }
        }
        return SyncResult.success(nextEntries.size(), added, updated, removed, caliberChangedCodes);
    }

    private static Map<String, IndicatorCatalogEntry> byCode(List<IndicatorCatalogEntry> entries) {
        Map<String, IndicatorCatalogEntry> result = new LinkedHashMap<>();
        for (IndicatorCatalogEntry entry : entries) {
            if (StringUtils.hasText(entry.code())) {
                result.put(entry.code(), entry);
            }
        }
        return result;
    }

    private static boolean same(String left, String right) {
        return String.valueOf(left).equals(String.valueOf(right));
    }
}
