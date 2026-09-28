package com.yuzhi.dts.copilot.ai.service.platform;

public record PlatformIndicatorDto(
        String id,
        String code,
        String name,
        String category,
        String domain,
        String definition,
        String expressionSql,
        String status,
        String version,
        String tags,
        String dimensionFields,
        String dateColumn,
        String timeGrain,
        String aggregationType,
        String measureField,
        String dataLevel,
        String owner
) {
}
