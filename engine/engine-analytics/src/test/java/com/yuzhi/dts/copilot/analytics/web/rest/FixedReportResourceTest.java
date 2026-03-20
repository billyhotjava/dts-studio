package com.yuzhi.dts.copilot.analytics.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.yuzhi.dts.copilot.analytics.domain.AnalyticsReportTemplate;
import com.yuzhi.dts.copilot.analytics.domain.AnalyticsUser;
import com.yuzhi.dts.copilot.analytics.repository.AnalyticsReportTemplateRepository;
import com.yuzhi.dts.copilot.analytics.service.AnalyticsSessionService;
import com.yuzhi.dts.copilot.analytics.service.report.AuthorityQueryService;
import com.yuzhi.dts.copilot.analytics.service.report.ReportExecutionPlanService;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class FixedReportResourceTest {

    private static final String RESOURCE_CLASS = "com.yuzhi.dts.copilot.analytics.web.rest.FixedReportResource";

    @Mock
    private AnalyticsSessionService sessionService;

    @Mock
    private AnalyticsReportTemplateRepository templateRepository;

    private final ReportExecutionPlanService planService = new ReportExecutionPlanService(new AuthorityQueryService());

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.standaloneSetup(newResource()).build();
    }

    @Test
    void runShouldResolveLatestPublishedCertifiedTemplateByTemplateCodeAndReturnMetadata() throws Exception {
        AnalyticsUser user = buildUser("biadmin", true);
        when(sessionService.resolveUser(any(HttpServletRequest.class))).thenReturn(Optional.of(user));
        when(templateRepository.findAllByArchivedFalseOrderByUpdatedAtDesc(any(Pageable.class)))
                .thenReturn(List.of(
                        template(
                                11L,
                                "FIN-AR-OVERVIEW",
                                "应收总览看板-草稿",
                                "财务",
                                "KPI",
                                "VIEW",
                                "authority.finance.receivable_overview_draft",
                                "REALTIME",
                                "{\"params\":[{\"name\":\"asOfDate\"},{\"name\":\"projectId\"}]}",
                                "{\"superuserOnly\":true}",
                                "draft",
                                false,
                                false,
                                Instant.parse("2026-03-20T09:00:00Z")),
                        template(
                                12L,
                                "FIN-AR-OVERVIEW",
                                "应收总览看板",
                                "财务",
                                "KPI",
                                "VIEW",
                                "authority.finance.receivable_overview",
                                "REALTIME",
                                "{\"params\":[{\"name\":\"asOfDate\"},{\"name\":\"projectId\"}]}",
                                "{\"superuserOnly\":true}",
                                "CERTIFIED",
                                true,
                                false,
                                Instant.parse("2026-03-20T08:00:00Z"))));

        mockMvc.perform(
                        post("/api/fixed-reports/FIN-AR-OVERVIEW/run")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"parameters":{"asOfDate":"2026-03-20","projectId":"PRJ-001"}}
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.templateCode").value("FIN-AR-OVERVIEW"))
                .andExpect(jsonPath("$.templateName").value("应收总览看板"))
                .andExpect(jsonPath("$.freshness").value("REALTIME"))
                .andExpect(jsonPath("$.sourceType").value("VIEW"))
                .andExpect(jsonPath("$.targetObject").value("authority.finance.receivable_overview"))
                .andExpect(jsonPath("$.route").value("AUTHORITY_VIEW"))
                .andExpect(jsonPath("$.executionStatus").value("READY"))
                .andExpect(jsonPath("$.supported").value(true));
    }

    @Test
    void runShouldRejectUnknownParameters() throws Exception {
        AnalyticsUser user = buildUser("biadmin", true);
        when(sessionService.resolveUser(any(HttpServletRequest.class))).thenReturn(Optional.of(user));
        when(templateRepository.findAllByArchivedFalseOrderByUpdatedAtDesc(any(Pageable.class)))
                .thenReturn(List.of(template(
                        21L,
                        "FIN-PENDING-RECEIPTS-DETAIL",
                        "待收款明细",
                        "财务",
                        "DETAIL",
                        "VIEW",
                        "authority.finance.pending_receipts_detail",
                        "REALTIME",
                        "{\"params\":[{\"name\":\"projectId\"},{\"name\":\"customerId\"}]}",
                        "{\"roles\":[\"财务专员\"]}",
                        "CERTIFIED",
                        true,
                        false,
                        Instant.parse("2026-03-20T08:00:00Z"))));

        mockMvc.perform(
                        post("/api/fixed-reports/FIN-PENDING-RECEIPTS-DETAIL/run")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"parameters":{"projectId":"PRJ-001","unexpectedParam":"boom"}}
                                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQ_INVALID_ARGUMENT"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("unexpectedParam")));
    }

    @Test
    void runShouldRejectSuperuserOnlyTemplatesForRegularUsers() throws Exception {
        AnalyticsUser user = buildUser("biadmin", false);
        when(sessionService.resolveUser(any(HttpServletRequest.class))).thenReturn(Optional.of(user));
        when(templateRepository.findAllByArchivedFalseOrderByUpdatedAtDesc(any(Pageable.class)))
                .thenReturn(List.of(template(
                        31L,
                        "PROC-PURCHASE-REQUEST-TODO",
                        "采购申请待办",
                        "采购",
                        "DETAIL",
                        "VIEW",
                        "authority.procurement.request_todo",
                        "REALTIME",
                        "{\"params\":[{\"name\":\"requestStatus\"}]}",
                        "{\"superuserOnly\":true}",
                        "CERTIFIED",
                        true,
                        false,
                        Instant.parse("2026-03-20T08:00:00Z"))));

        mockMvc.perform(
                        post("/api/fixed-reports/PROC-PURCHASE-REQUEST-TODO/run")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"parameters":{"requestStatus":"PENDING"}}
                                        """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SEC_FORBIDDEN"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("permission")));
    }

    @Test
    void runShouldReturnNotFoundWhenTemplateCodeHasNoPublishedCertifiedVersion() throws Exception {
        AnalyticsUser user = buildUser("biadmin", true);
        when(sessionService.resolveUser(any(HttpServletRequest.class))).thenReturn(Optional.of(user));
        when(templateRepository.findAllByArchivedFalseOrderByUpdatedAtDesc(any(Pageable.class)))
                .thenReturn(List.of(template(
                        41L,
                        "OPS-UNKNOWN",
                        "运营综合指标中心-草稿",
                        "运营",
                        "KPI",
                        "VIEW",
                        "authority.ops.kpi",
                        "REALTIME",
                        "{\"params\":[{\"name\":\"period\"}]}",
                        "{\"superuserOnly\":true}",
                        "draft",
                        false,
                        false,
                        Instant.parse("2026-03-20T08:00:00Z"))));

        mockMvc.perform(post("/api/fixed-reports/OPS-UNKNOWN/run").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("REPORT_TEMPLATE_NOT_FOUND"));
    }

    private Object newResource() throws Exception {
        Class<?> resourceClass = Class.forName(RESOURCE_CLASS);
        Constructor<?> constructor = resourceClass.getDeclaredConstructor(
                AnalyticsSessionService.class,
                AnalyticsReportTemplateRepository.class,
                ReportExecutionPlanService.class);
        constructor.setAccessible(true);
        return constructor.newInstance(sessionService, templateRepository, planService);
    }

    private static AnalyticsUser buildUser(String username, boolean superuser) {
        AnalyticsUser user = new AnalyticsUser();
        user.setId(superuser ? 1L : 2L);
        user.setUsername(username);
        user.setFirstName("Bi");
        user.setLastName("Admin");
        user.setPasswordHash("secret");
        user.setSuperuser(superuser);
        user.setActive(true);
        return user;
    }

    private static AnalyticsReportTemplate template(
            Long id,
            String templateCode,
            String name,
            String domain,
            String category,
            String dataSourceType,
            String targetObject,
            String refreshPolicy,
            String parameterSchemaJson,
            String permissionPolicyJson,
            String certificationStatus,
            boolean published,
            boolean archived,
            Instant updatedAt) throws Exception {
        AnalyticsReportTemplate template = new AnalyticsReportTemplate();
        template.setTemplateCode(templateCode);
        template.setName(name);
        template.setDescription(name + " description");
        template.setDomain(domain);
        template.setCategory(category);
        template.setDataSourceType(dataSourceType);
        template.setTargetObject(targetObject);
        template.setRefreshPolicy(refreshPolicy);
        template.setParameterSchemaJson(parameterSchemaJson);
        template.setPermissionPolicyJson(permissionPolicyJson);
        template.setCertificationStatus(certificationStatus);
        template.setPublished(published);
        template.setArchived(archived);
        template.setCreatorId(1L);
        template.setSpecJson("{}");
        setField(template, "id", id);
        setField(template, "createdAt", updatedAt);
        setField(template, "updatedAt", updatedAt);
        return template;
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
