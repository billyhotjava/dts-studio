package com.yuzhi.dts.copilot.ai.service.platform;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class IndicatorCatalogStore {

    private volatile Snapshot snapshot = new Snapshot(List.of(), null, null);

    public List<IndicatorCatalogEntry> all() {
        return snapshot.entries();
    }

    public Optional<IndicatorCatalogEntry> byId(String id) {
        if (!StringUtils.hasText(id)) {
            return Optional.empty();
        }
        String normalized = id.trim();
        return snapshot.entries().stream()
                .filter(entry -> normalized.equals(entry.id()))
                .findFirst();
    }

    public Optional<IndicatorCatalogEntry> byCode(String code) {
        if (!StringUtils.hasText(code)) {
            return Optional.empty();
        }
        String normalized = code.trim();
        return snapshot.entries().stream()
                .filter(entry -> normalized.equals(entry.code()))
                .findFirst();
    }

    public boolean isReady() {
        return !snapshot.entries().isEmpty();
    }

    public CatalogStatus status() {
        Snapshot current = snapshot;
        SyncResult lastResult = current.lastResult();
        boolean stale = lastResult != null && lastResult.status() == SyncResult.Status.FAILED && !current.entries().isEmpty();
        return new CatalogStatus(
                current.entries().size(),
                current.lastSyncAt(),
                stale,
                lastResult == null ? null : lastResult.status(),
                lastResult == null ? null : lastResult.error());
    }

    public void replaceAll(List<IndicatorCatalogEntry> entries, SyncResult result) {
        List<IndicatorCatalogEntry> next = entries == null ? List.of() : List.copyOf(entries);
        snapshot = new Snapshot(next, result == null ? Instant.now() : result.completedAt(), result);
    }

    public void recordRefreshResult(SyncResult result) {
        Snapshot current = snapshot;
        snapshot = new Snapshot(current.entries(), current.lastSyncAt(), result);
    }

    private record Snapshot(
            List<IndicatorCatalogEntry> entries,
            Instant lastSyncAt,
            SyncResult lastResult
    ) {
        private Snapshot {
            entries = entries == null ? List.of() : List.copyOf(entries);
        }
    }
}
