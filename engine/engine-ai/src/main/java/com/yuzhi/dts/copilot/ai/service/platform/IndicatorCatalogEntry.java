package com.yuzhi.dts.copilot.ai.service.platform;

import java.util.List;

public record IndicatorCatalogEntry(
        String id,
        String code,
        String name,
        String category,
        String domain,
        String definition,
        String expressionSql,
        String status,
        String version,
        List<String> tags,
        List<String> dimensionFields,
        String dateColumn,
        String timeGrain,
        String aggregationType,
        String measureField,
        String dataLevel,
        String owner,
        List<String> matchKeywords
) {
    public IndicatorCatalogEntry {
        tags = tags == null ? List.of() : List.copyOf(tags);
        dimensionFields = dimensionFields == null ? List.of() : List.copyOf(dimensionFields);
        matchKeywords = matchKeywords == null ? List.of() : List.copyOf(matchKeywords);
    }
}
