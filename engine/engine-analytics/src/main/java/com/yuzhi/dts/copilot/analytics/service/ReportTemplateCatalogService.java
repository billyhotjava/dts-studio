package com.yuzhi.dts.copilot.analytics.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuzhi.dts.copilot.analytics.domain.AnalyticsReportTemplate;
import com.yuzhi.dts.copilot.analytics.repository.AnalyticsReportTemplateRepository;
import com.yuzhi.dts.copilot.analytics.service.report.FixedReportPageAnchorService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReportTemplateCatalogService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final AnalyticsReportTemplateRepository templateRepository;
    private final FixedReportPageAnchorService pageAnchorService;

    public ReportTemplateCatalogService(
            AnalyticsReportTemplateRepository templateRepository,
            FixedReportPageAnchorService pageAnchorService) {
        this.templateRepository = templateRepository;
        this.pageAnchorService = pageAnchorService;
    }

    public List<Map<String, Object>> listTemplates(String domain, String category, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        String normalizedDomain = normalize(domain);
        String normalizedCategory = normalize(category);

        return templateRepository.findCatalogTemplates(normalizedDomain, normalizedCategory, PageRequest.of(0, safeLimit)).stream()
                .map(this::toCatalogRow)
                .toList();
    }

    public Optional<Map<String, Object>> getTemplate(String templateCode) {
        String normalizedTemplateCode = normalize(templateCode);
        if (normalizedTemplateCode == null) {
            return Optional.empty();
        }
        return templateRepository.findLatestRunnableTemplateByTemplateCode(normalizedTemplateCode)
                .map(this::toCatalogRow);
    }

    private Map<String, Object> toCatalogRow(AnalyticsReportTemplate template) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", template.getId());
        row.put("name", template.getName());
        row.put("description", template.getDescription());
        row.put("templateCode", template.getTemplateCode());
        row.put("domain", template.getDomain());
        row.put("category", template.getCategory());
        row.put("dataSourceType", template.getDataSourceType());
        row.put("targetObject", template.getTargetObject());
        row.put("refreshPolicy", template.getRefreshPolicy());
        row.put("certificationStatus", template.getCertificationStatus());
        row.put("published", template.isPublished());
        row.put("parameterSchemaJson", template.getParameterSchemaJson());
        row.put("placeholderReviewRequired", isPlaceholderReviewRequired(template.getSpecJson()));
        row.put("updatedAt", template.getUpdatedAt());
        pageAnchorService.resolve(template.getTemplateCode()).ifPresent(anchor -> {
            row.put("legacyPageTitle", anchor.title());
            row.put("legacyPagePath", anchor.path());
        });
        return row;
    }

    private static boolean isPlaceholderReviewRequired(String specJson) {
        JsonNode spec = parseJson(specJson);
        return spec != null && spec.path("placeholderReviewRequired").asBoolean(false);
    }

    private static JsonNode parseJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readTree(raw);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase(Locale.ROOT);
    }
}
