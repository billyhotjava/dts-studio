package com.yuzhi.dts.copilot.analytics.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.yuzhi.dts.copilot.analytics.domain.AnalyticsReportTemplate;
import com.yuzhi.dts.copilot.analytics.domain.AnalyticsUser;
import com.yuzhi.dts.copilot.analytics.repository.AnalyticsReportTemplateRepository;
import com.yuzhi.dts.copilot.analytics.service.AnalyticsSessionService;
import com.yuzhi.dts.copilot.analytics.service.ReportTemplateCatalogService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

@ExtendWith(MockitoExtension.class)
class ReportTemplateCatalogResourceTest {

    @Mock
    private AnalyticsSessionService sessionService;

    @Mock
    private AnalyticsReportTemplateRepository templateRepository;

    @Test
    void listTemplatesShouldRequireAuthenticatedSession() {
        ReportTemplateCatalogService service = new ReportTemplateCatalogService(templateRepository);
        ReportTemplateCatalogResource resource = new ReportTemplateCatalogResource(sessionService, service);

        ResponseEntity<?> response = resource.listTemplates(null, null, 100, null);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void listTemplatesShouldReturnOnlyCertifiedPublishedActiveTemplatesByDefault() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(sessionService.resolveUser(request)).thenReturn(Optional.of(buildUser()));
        stubCatalogTemplates(List.of(
                row(
                        101L,
                        "应收总览看板",
                        "财务",
                        "KPI",
                        "FIN-01",
                        "VIEW",
                        "v_monthly_settlement",
                        "real-time",
                        "certified",
                        true,
                        false,
                        Instant.parse("2026-03-20T08:00:00Z")),
                row(
                        102L,
                        "客户欠款排行",
                        "财务",
                        "RANK",
                        "FIN-02",
                        "MART",
                        "mart_finance_ar_rank",
                        "5m",
                        "certified",
                        false,
                        false,
                        Instant.parse("2026-03-20T09:00:00Z")),
                row(
                        103L,
                        "采购申请待办",
                        "采购",
                        "DETAIL",
                        "PROC-01",
                        "VIEW",
                        "v_procurement_todo",
                        "real-time",
                        "certified",
                        false,
                        false,
                        Instant.parse("2026-03-20T10:00:00Z")),
                row(
                        104L,
                        "库存现量看板",
                        "仓库",
                        "KPI",
                        "WH-01",
                        "VIEW",
                        "v_inventory_current",
                        "real-time",
                        "draft",
                        true,
                        false,
                        Instant.parse("2026-03-20T11:00:00Z"))));

        ReportTemplateCatalogService service = new ReportTemplateCatalogService(templateRepository);
        ReportTemplateCatalogResource resource = new ReportTemplateCatalogResource(sessionService, service);

        ResponseEntity<?> response = resource.listTemplates(null, null, 100, request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isInstanceOf(List.class);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) response.getBody();
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0)).containsEntry("templateCode", "FIN-01");
        assertThat(rows.get(0)).containsEntry("certificationStatus", "certified");
        assertThat(rows.get(0)).containsEntry("published", true);
    }

    @Test
    void listTemplatesShouldFilterByDomainCategoryAndLimit() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(sessionService.resolveUser(request)).thenReturn(Optional.of(buildUser()));
        stubCatalogTemplates(List.of(
                row(201L, "应收总览看板", "财务", "KPI", "FIN-01", "VIEW", "v_monthly_settlement", "real-time", "certified", true, false, Instant.parse("2026-03-20T08:00:00Z")),
                row(202L, "客户欠款排行", "财务", "RANK", "FIN-02", "MART", "mart_finance_ar_rank", "5m", "certified", true, false, Instant.parse("2026-03-20T09:00:00Z")),
                row(203L, "采购申请待办", "采购", "DETAIL", "PROC-01", "VIEW", "v_procurement_todo", "real-time", "certified", true, false, Instant.parse("2026-03-20T10:00:00Z"))));

        ReportTemplateCatalogService service = new ReportTemplateCatalogService(templateRepository);
        ReportTemplateCatalogResource resource = new ReportTemplateCatalogResource(sessionService, service);

        ResponseEntity<?> response = resource.listTemplates("财务", "RANK", 1, request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) response.getBody();
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0)).containsEntry("templateCode", "FIN-02");
    }

    @Test
    void listTemplatesShouldKeepCertifiedRowsWhenLaterDraftNoiseExists() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(sessionService.resolveUser(request)).thenReturn(Optional.of(buildUser()));
        List<AnalyticsReportTemplateRow> rows = new ArrayList<>();
        rows.add(row(
                301L,
                "应收总览看板",
                "财务",
                "KPI",
                "FIN-01",
                "VIEW",
                "v_monthly_settlement",
                "real-time",
                "certified",
                true,
                false,
                Instant.parse("2026-03-20T08:00:00Z")));
        for (int i = 0; i < 20; i++) {
            rows.add(row(
                    400L + i,
                    "草稿报表-" + i,
                    "财务",
                    i % 2 == 0 ? "KPI" : "RANK",
                    "DRAFT-" + i,
                    "VIEW",
                    "v_noise_" + i,
                    "real-time",
                    i % 2 == 0 ? "draft" : "review",
                    false,
                    false,
                    Instant.parse("2026-03-20T09:" + String.format(Locale.ROOT, "%02d", i) + ":00Z")));
        }
        stubCatalogTemplates(rows);

        ReportTemplateCatalogService service = new ReportTemplateCatalogService(templateRepository);
        ReportTemplateCatalogResource resource = new ReportTemplateCatalogResource(sessionService, service);

        ResponseEntity<?> response = resource.listTemplates(null, null, 1, request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> responseRows = (List<Map<String, Object>>) response.getBody();
        assertThat(responseRows).hasSize(1);
        assertThat(responseRows.get(0)).containsEntry("templateCode", "FIN-01");
    }

    private void stubCatalogTemplates(List<AnalyticsReportTemplateRow> rows) {
        when(templateRepository.findCatalogTemplates(any(), any(), any())).thenAnswer(invocation -> {
            String domain = invocation.getArgument(0);
            String category = invocation.getArgument(1);
            Pageable pageable = invocation.getArgument(2);

            return rows.stream()
                    .map(ReportTemplateCatalogResourceTest::toEntity)
                    .filter(template -> !template.isArchived())
                    .filter(AnalyticsReportTemplate::isPublished)
                    .filter(template -> "certified".equalsIgnoreCase(template.getCertificationStatus()))
                    .filter(template -> matches(domain, template.getDomain()))
                    .filter(template -> matches(category, template.getCategory()))
                    .sorted(Comparator.comparing(AnalyticsReportTemplate::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                    .skip(pageable.getOffset())
                    .limit(pageable.getPageSize())
                    .toList();
        });
    }

    private static boolean matches(String requested, String candidate) {
        if (requested == null) {
            return true;
        }
        return candidate != null && candidate.trim().toLowerCase(Locale.ROOT).equals(requested.trim().toLowerCase(Locale.ROOT));
    }

    private static AnalyticsUser buildUser() {
        AnalyticsUser user = new AnalyticsUser();
        user.setId(1L);
        user.setUsername("alice");
        user.setActive(true);
        user.setSuperuser(true);
        return user;
    }

    private static AnalyticsReportTemplateRow row(
            Long id,
            String name,
            String domain,
            String category,
            String templateCode,
            String dataSourceType,
            String targetObject,
            String refreshPolicy,
            String certificationStatus,
            boolean published,
            boolean archived,
            Instant updatedAt) {
        return new AnalyticsReportTemplateRow(id, name, domain, category, templateCode, dataSourceType, targetObject, refreshPolicy, certificationStatus, published, archived, updatedAt);
    }

    private static AnalyticsReportTemplate toEntity(AnalyticsReportTemplateRow row) {
        AnalyticsReportTemplate entity = new AnalyticsReportTemplate();
        entity.setTemplateCode(row.templateCode());
        entity.setName(row.name());
        entity.setDescription("desc-" + row.templateCode());
        entity.setDomain(row.domain());
        entity.setCategory(row.category());
        entity.setDataSourceType(row.dataSourceType());
        entity.setTargetObject(row.targetObject());
        entity.setRefreshPolicy(row.refreshPolicy());
        entity.setCertificationStatus(row.certificationStatus());
        entity.setPublished(row.published());
        entity.setArchived(row.archived());
        entity.setSpecJson("{}");
        entity.setCreatorId(1L);
        try {
            var idField = entity.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, row.id());
            var updatedField = entity.getClass().getDeclaredField("updatedAt");
            updatedField.setAccessible(true);
            updatedField.set(entity, row.updatedAt());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return entity;
    }

    private record AnalyticsReportTemplateRow(
            Long id,
            String name,
            String domain,
            String category,
            String templateCode,
            String dataSourceType,
            String targetObject,
            String refreshPolicy,
            String certificationStatus,
            boolean published,
            boolean archived,
            Instant updatedAt) {}
}
