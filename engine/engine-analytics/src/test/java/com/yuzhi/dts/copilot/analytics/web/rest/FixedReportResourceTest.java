package com.yuzhi.dts.copilot.analytics.web.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yuzhi.dts.copilot.analytics.domain.AnalyticsReportTemplate;
import com.yuzhi.dts.copilot.analytics.domain.AnalyticsUser;
import com.yuzhi.dts.copilot.analytics.repository.AnalyticsReportTemplateRepository;
import com.yuzhi.dts.copilot.analytics.service.AnalyticsSessionService;
import com.yuzhi.dts.copilot.analytics.service.report.AuthorityQueryService;
import com.yuzhi.dts.copilot.analytics.service.report.FixedReportPageAnchorService;
import com.yuzhi.dts.copilot.analytics.service.report.ReportExecutionPlanService;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class FixedReportResourceTest {

    private final ReportExecutionPlanService planService = new ReportExecutionPlanService(new AuthorityQueryService());
    private final FixedReportPageAnchorService pageAnchorService = new FixedReportPageAnchorService();
    private MutableSessionService sessionService;
    private InMemoryTemplateRepository templateRepository;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        sessionService = new MutableSessionService();
        templateRepository = new InMemoryTemplateRepository();
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new FixedReportResource(sessionService, templateRepository.repository(), planService, pageAnchorService))
                .build();
    }

    @Test
    void runShouldResolveLatestPublishedCertifiedTemplateByTemplateCodeAndReturnMetadata() throws Exception {
        sessionService.setResolvedUser(buildUser("biadmin", true));
        templateRepository.put(template(
                11L,
                "FIN-AR-OVERVIEW",
                "应收总览看板旧版",
                "财务",
                "KPI",
                "VIEW",
                "authority.finance.receivable_overview_v1",
                "REALTIME",
                "{\"params\":[{\"name\":\"asOfDate\"},{\"name\":\"projectId\"}]}",
                "{\"superuserOnly\":true}",
                "CERTIFIED",
                true,
                false,
                Instant.parse("2026-03-19T08:00:00Z")));
        templateRepository.put(template(
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
                Instant.parse("2026-03-20T08:00:00Z")));
        templateRepository.put(template(
                13L,
                "FIN-AR-OVERVIEW",
                "应收总览草稿",
                "财务",
                "KPI",
                "VIEW",
                "authority.finance.receivable_overview_draft",
                "REALTIME",
                "{\"params\":[{\"name\":\"asOfDate\"},{\"name\":\"projectId\"}]}",
                "{\"superuserOnly\":true}",
                "DRAFT",
                true,
                false,
                Instant.parse("2026-03-21T08:00:00Z")));
        templateRepository.put(template(
                14L,
                "FIN-AR-OVERVIEW",
                "应收总览未发布",
                "财务",
                "KPI",
                "VIEW",
                "authority.finance.receivable_overview_unpublished",
                "REALTIME",
                "{\"params\":[{\"name\":\"asOfDate\"},{\"name\":\"projectId\"}]}",
                "{\"superuserOnly\":true}",
                "CERTIFIED",
                false,
                false,
                Instant.parse("2026-03-22T08:00:00Z")));

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
        sessionService.setResolvedUser(buildUser("biadmin", true));
        templateRepository.put(template(
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
                Instant.parse("2026-03-20T08:00:00Z")));

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
    void runShouldRejectParametersWhenSchemaDefinesNoNamedParams() throws Exception {
        sessionService.setResolvedUser(buildUser("biadmin", true));
        templateRepository.put(template(
                22L,
                "FIN-SCHEMA-INVALID",
                "参数定义异常模板",
                "财务",
                "DETAIL",
                "VIEW",
                "authority.finance.invalid_schema",
                "REALTIME",
                "{\"params\":[]}",
                "{\"superuserOnly\":true}",
                "CERTIFIED",
                true,
                false,
                Instant.parse("2026-03-20T08:00:00Z")));

        mockMvc.perform(
                        post("/api/fixed-reports/FIN-SCHEMA-INVALID/run")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"parameters":{"projectId":"PRJ-001"}}
                                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQ_INVALID_ARGUMENT"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("does not define any named params")));
    }

    @Test
    void runShouldRejectSuperuserOnlyTemplatesForRegularUsers() throws Exception {
        sessionService.setResolvedUser(buildUser("biadmin", false));
        templateRepository.put(template(
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
                Instant.parse("2026-03-20T08:00:00Z")));

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
    void runShouldRejectTemplatesWhenRolePolicyDoesNotMatchRequestRoles() throws Exception {
        sessionService.setResolvedUser(buildUser("biadmin", false));
        templateRepository.put(template(
                41L,
                "FIN-PENDING-RECEIPTS-DETAIL",
                "待收款明细",
                "财务",
                "DETAIL",
                "VIEW",
                "authority.finance.pending_receipts_detail",
                "REALTIME",
                "{\"params\":[{\"name\":\"projectId\"}]}",
                "{\"scope\":\"ROLE_AND_ORG\",\"roles\":[\"财务专员\",\"财务经理\"]}",
                "CERTIFIED",
                true,
                false,
                Instant.parse("2026-03-20T08:00:00Z")));

        mockMvc.perform(
                        post("/api/fixed-reports/FIN-PENDING-RECEIPTS-DETAIL/run")
                                .header("X-DTS-Roles", "采购员,仓库管理员")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"parameters":{"projectId":"PRJ-001"}}
                                        """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SEC_FORBIDDEN"));
    }

    @Test
    void runShouldAllowTemplatesWhenRolePolicyMatchesRequestRoles() throws Exception {
        sessionService.setResolvedUser(buildUser("biadmin", false));
        templateRepository.put(template(
                42L,
                "FIN-PENDING-RECEIPTS-DETAIL",
                "待收款明细",
                "财务",
                "DETAIL",
                "VIEW",
                "authority.finance.pending_receipts_detail",
                "REALTIME",
                "{\"params\":[{\"name\":\"projectId\"}]}",
                "{\"scope\":\"ROLE_AND_ORG\",\"roles\":[\"财务专员\",\"财务经理\"]}",
                "CERTIFIED",
                true,
                false,
                Instant.parse("2026-03-20T08:00:00Z")));

        mockMvc.perform(
                        post("/api/fixed-reports/FIN-PENDING-RECEIPTS-DETAIL/run")
                                .header("X-DTS-Roles", "销售主管,财务专员")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"parameters":{"projectId":"PRJ-001"}}
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.templateCode").value("FIN-PENDING-RECEIPTS-DETAIL"));
    }

    @Test
    void runShouldMarkPlaceholderTemplateAsBackingRequiredInsteadOfReady() throws Exception {
        sessionService.setResolvedUser(buildUser("biadmin", true));
        templateRepository.put(template(
                43L,
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
                Instant.parse("2026-03-20T08:00:00Z"),
                """
                {"placeholderReviewRequired":true}
                """));

        mockMvc.perform(
                        post("/api/fixed-reports/PROC-PURCHASE-REQUEST-TODO/run")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"parameters":{"requestStatus":"PENDING"}}
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.templateCode").value("PROC-PURCHASE-REQUEST-TODO"))
                .andExpect(jsonPath("$.supported").value(false))
                .andExpect(jsonPath("$.executionStatus").value("BACKING_REQUIRED"))
                .andExpect(jsonPath("$.placeholderReviewRequired").value(true));
    }

    @Test
    void runShouldExposeLegacyPageAnchorForKnownTemplate() throws Exception {
        sessionService.setResolvedUser(buildUser("biadmin", true));
        templateRepository.put(template(
                44L,
                "FIN-AR-OVERVIEW",
                "财务结算汇总",
                "财务",
                "KPI",
                "VIEW",
                "authority.finance.receivable_overview",
                "REALTIME",
                "{\"params\":[]}",
                "{\"superuserOnly\":true}",
                "CERTIFIED",
                true,
                false,
                Instant.parse("2026-03-20T08:00:00Z")));

        mockMvc.perform(
                        post("/api/fixed-reports/FIN-AR-OVERVIEW/run")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"parameters":{}}
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.legacyPagePath").value("/operate/settlement"))
                .andExpect(jsonPath("$.legacyPageTitle").value("财务结算"));
    }

    @Test
    void runShouldReturnNotFoundWhenTemplateCodeHasNoPublishedCertifiedVersion() throws Exception {
        sessionService.setResolvedUser(buildUser("biadmin", true));

        mockMvc.perform(post("/api/fixed-reports/OPS-UNKNOWN/run").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("REPORT_TEMPLATE_NOT_FOUND"));
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
        return template(
                id,
                templateCode,
                name,
                domain,
                category,
                dataSourceType,
                targetObject,
                refreshPolicy,
                parameterSchemaJson,
                permissionPolicyJson,
                certificationStatus,
                published,
                archived,
                updatedAt,
                "{}");
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
            Instant updatedAt,
            String specJson) throws Exception {
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
        template.setSpecJson(specJson);
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

    private static final class MutableSessionService extends AnalyticsSessionService {

        private Optional<AnalyticsUser> resolvedUser = Optional.empty();

        private MutableSessionService() {
            super(null, null, null);
        }

        void setResolvedUser(AnalyticsUser user) {
            this.resolvedUser = Optional.ofNullable(user);
        }

        @Override
        public Optional<AnalyticsUser> resolveUser(HttpServletRequest request) {
            return resolvedUser;
        }
    }

    private static final class InMemoryTemplateRepository implements InvocationHandler {

        private final List<AnalyticsReportTemplate> templates = new ArrayList<>();
        private final AnalyticsReportTemplateRepository repository;

        private InMemoryTemplateRepository() {
            this.repository = (AnalyticsReportTemplateRepository) Proxy.newProxyInstance(
                    AnalyticsReportTemplateRepository.class.getClassLoader(),
                    new Class<?>[] {AnalyticsReportTemplateRepository.class},
                    this);
        }

        AnalyticsReportTemplateRepository repository() {
            return repository;
        }

        void put(AnalyticsReportTemplate template) {
            templates.add(template);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String name = method.getName();
            if ("findLatestRunnableTemplateByTemplateCode".equals(name)) {
                String templateCode = (String) args[0];
                if (templateCode == null) {
                    return Optional.empty();
                }
                return findRunnableTemplates(templateCode).stream().findFirst();
            }
            if ("findRunnableTemplatesByTemplateCode".equals(name)) {
                String templateCode = (String) args[0];
                Pageable pageable = (Pageable) args[1];
                List<AnalyticsReportTemplate> matches = findRunnableTemplates(templateCode);
                if (pageable == null || pageable.isUnpaged()) {
                    return matches;
                }
                int fromIndex = (int) Math.min(pageable.getOffset(), matches.size());
                int toIndex = Math.min(fromIndex + pageable.getPageSize(), matches.size());
                return matches.subList(fromIndex, toIndex);
            }
            if ("toString".equals(name)) {
                return "InMemoryTemplateRepository";
            }
            if ("hashCode".equals(name)) {
                return System.identityHashCode(this);
            }
            if ("equals".equals(name)) {
                return proxy == args[0];
            }
            throw new UnsupportedOperationException("Unexpected repository method: " + name);
        }

        private List<AnalyticsReportTemplate> findRunnableTemplates(String templateCode) {
            if (templateCode == null) {
                return List.of();
            }
            return templates.stream()
                    .filter(template -> template.getTemplateCode() != null)
                    .filter(template -> template.getTemplateCode()
                            .toLowerCase(Locale.ROOT)
                            .equals(templateCode))
                    .filter(AnalyticsReportTemplate::isPublished)
                    .filter(template -> !template.isArchived())
                    .filter(template -> "certified".equalsIgnoreCase(String.valueOf(template.getCertificationStatus())))
                    .sorted(Comparator.comparing(
                            AnalyticsReportTemplate::getUpdatedAt,
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .toList();
        }
    }
}
