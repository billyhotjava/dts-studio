package com.yuzhi.dts.copilot.ai.service.platform;

import java.util.List;

public record PlatformIndicatorPage(
        List<PlatformIndicatorDto> items,
        int page,
        int size,
        int totalPages
) {
    public PlatformIndicatorPage {
        items = items == null ? List.of() : List.copyOf(items);
        page = Math.max(0, page);
        size = Math.max(1, size);
        totalPages = Math.max(0, totalPages);
    }

    public static PlatformIndicatorPage of(List<PlatformIndicatorDto> items, int page, int size, int totalPages) {
        return new PlatformIndicatorPage(items, page, size, totalPages);
    }
}
