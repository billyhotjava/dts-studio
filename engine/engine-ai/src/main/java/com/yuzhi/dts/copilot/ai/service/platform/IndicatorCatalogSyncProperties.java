package com.yuzhi.dts.copilot.ai.service.platform;

public record IndicatorCatalogSyncProperties(
        boolean enabled,
        int pageSize,
        int maxPages
) {
    public IndicatorCatalogSyncProperties {
        pageSize = pageSize > 0 ? pageSize : 100;
        maxPages = maxPages > 0 ? maxPages : 200;
    }
}
