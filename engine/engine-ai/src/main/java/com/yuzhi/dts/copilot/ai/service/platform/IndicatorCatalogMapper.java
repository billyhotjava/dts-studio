package com.yuzhi.dts.copilot.ai.service.platform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import org.springframework.util.StringUtils;

public final class IndicatorCatalogMapper {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private IndicatorCatalogMapper() {
    }

    public static IndicatorCatalogEntry fromPlatform(PlatformIndicatorDto dto) {
        List<String> tags = splitList(dto.tags());
        List<String> dimensions = splitList(dto.dimensionFields());
        return new IndicatorCatalogEntry(
                clean(dto.id()),
                clean(dto.code()),
                clean(dto.name()),
                clean(dto.category()),
                resolveDomain(dto),
                clean(dto.definition()),
                clean(dto.expressionSql()),
                clean(dto.status()),
                clean(dto.version()),
                tags,
                dimensions,
                clean(dto.dateColumn()),
                clean(dto.timeGrain()),
                clean(dto.aggregationType()),
                clean(dto.measureField()),
                clean(dto.dataLevel()),
                clean(dto.owner()),
                buildMatchKeywords(dto, tags, dimensions));
    }

    public static boolean isPublished(PlatformIndicatorDto dto) {
        if (dto == null || !StringUtils.hasText(dto.name())) {
            return false;
        }
        String status = clean(dto.status());
        return !StringUtils.hasText(status) || "已发布".equals(status) || "PUBLISHED".equalsIgnoreCase(status);
    }

    private static String resolveDomain(PlatformIndicatorDto dto) {
        String domain = clean(dto.domain());
        if (StringUtils.hasText(domain)) {
            return domain;
        }
        return clean(dto.category());
    }

    private static List<String> buildMatchKeywords(
            PlatformIndicatorDto dto,
            List<String> tags,
            List<String> dimensions) {
        LinkedHashSet<String> keywords = new LinkedHashSet<>();
        addKeyword(keywords, dto.name());
        addKeyword(keywords, dto.code());
        addKeyword(keywords, dto.category());
        addKeyword(keywords, dto.domain());
        tags.forEach(value -> addKeyword(keywords, value));
        dimensions.forEach(value -> addKeyword(keywords, value));
        return List.copyOf(keywords);
    }

    private static void addKeyword(LinkedHashSet<String> keywords, String value) {
        String normalized = clean(value);
        if (StringUtils.hasText(normalized)) {
            keywords.add(normalized.toLowerCase(Locale.ROOT));
        }
    }

    static List<String> splitList(String value) {
        String text = clean(value);
        if (!StringUtils.hasText(text)) {
            return List.of();
        }
        if (text.startsWith("[") && text.endsWith("]")) {
            List<String> parsed = parseJsonArray(text);
            if (!parsed.isEmpty()) {
                return parsed;
            }
        }
        List<String> result = new ArrayList<>();
        for (String part : text.split("[,;；，]")) {
            String item = clean(part);
            if (StringUtils.hasText(item)) {
                result.add(item);
            }
        }
        return List.copyOf(result);
    }

    private static List<String> parseJsonArray(String text) {
        try {
            JsonNode node = MAPPER.readTree(text);
            if (!node.isArray()) {
                return List.of();
            }
            List<String> result = new ArrayList<>();
            for (JsonNode item : node) {
                String value = item.asText("").trim();
                if (!value.isEmpty()) {
                    result.add(value);
                }
            }
            return List.copyOf(result);
        } catch (RuntimeException ignored) {
            return List.of();
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private static String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
