package com.yuzhi.dts.copilot.analytics.service;

import com.yuzhi.dts.copilot.analytics.domain.AnalyticsReportTemplate;
import com.yuzhi.dts.copilot.analytics.repository.AnalyticsReportTemplateRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReportTemplateCatalogService {

    private final AnalyticsReportTemplateRepository templateRepository;

    public ReportTemplateCatalogService(AnalyticsReportTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    public List<Map<String, Object>> listTemplates(String domain, String category, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        String normalizedDomain = normalize(domain);
        String normalizedCategory = normalize(category);

        return templateRepository.findCatalogTemplates(normalizedDomain, normalizedCategory, PageRequest.of(0, safeLimit)).stream()
                .map(this::toCatalogRow)
                .toList();
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
        row.put("updatedAt", template.getUpdatedAt());
        return row;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase(Locale.ROOT);
    }
}
