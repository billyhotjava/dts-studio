package com.yuzhi.dts.copilot.ai.service.platform;

import java.time.Instant;

public record CatalogStatus(
        int entryCount,
        Instant lastSyncAt,
        boolean stale,
        SyncResult.Status lastStatus,
        String lastError
) {
}
