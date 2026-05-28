package com.yuzhi.dts.copilot.ai.service.copilot;

import com.yuzhi.dts.copilot.ai.service.copilot.BusinessDirectResponseCatalogService.CatalogEntry;
import com.yuzhi.dts.copilot.ai.service.copilot.AgentBiReportCatalogService.ReportCatalogEntry;
import com.yuzhi.dts.copilot.ai.service.copilot.BusinessObjectCatalogService.BusinessObjectEntry;
import com.yuzhi.dts.copilot.ai.service.copilot.ConversationPlannerService.ConversationPlan;
import com.yuzhi.dts.copilot.ai.service.copilot.ConversationPlannerService.PlanMode;
import com.yuzhi.dts.copilot.ai.service.copilot.ConversationPlannerService.ResponseKind;
import com.yuzhi.dts.copilot.ai.service.copilot.IntentRouterService.ExtendedRoutingResult;
import com.yuzhi.dts.copilot.ai.service.copilot.IntentRouterService.RoutingResult;
import com.yuzhi.dts.copilot.ai.service.copilot.TemplateMatcherService.SuggestedQuestion;
import com.yuzhi.dts.copilot.ai.service.copilot.TemplateMatcherService.TemplateMatchResult;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AssetBackedPlannerPolicy implements PlannerPolicy {

    private static final Set<String> GENERIC_REPORT_KEYWORDS = Set.of(
            "报表", "汇总", "明细", "列表", "清单", "排行", "排名", "看板", "大屏", "台账", "统计"
    );

    private static final Set<String> GENERATED_REPORT_KEYWORDS = Set.of(
            "生成", "创建", "新建", "新的", "做一张", "做一个", "出一张", "出一个", "定制", "自定义", "临时"
    );

    private final IntentRouterService intentRouterService;
    private final TemplateMatcherService templateMatcherService;
    private final SemanticPackService semanticPackService;
    private final BusinessDirectResponseCatalogService directResponseCatalogService;
    private final AgentBiReportCatalogService reportCatalogService;
    private final BusinessObjectCatalogService businessObjectCatalogService;

    public AssetBackedPlannerPolicy(IntentRouterService intentRouterService,
                                    TemplateMatcherService templateMatcherService,
                                    SemanticPackService semanticPackService,
                                    BusinessDirectResponseCatalogService directResponseCatalogService,
                                    AgentBiReportCatalogService reportCatalogService,
                                    BusinessObjectCatalogService businessObjectCatalogService) {
        this.intentRouterService = intentRouterService;
        this.templateMatcherService = templateMatcherService;
        this.semanticPackService = semanticPackService;
        this.directResponseCatalogService = directResponseCatalogService;
        this.reportCatalogService = reportCatalogService;
        this.businessObjectCatalogService = businessObjectCatalogService;
    }

    @Override
    public String mode() {
        return "asset";
    }

    @Override
    public ConversationPlan plan(String userQuestion, Map<String, Boolean> martHealthSnapshot) {
        TemplateMatchResult templateMatch = templateMatcherService.match(userQuestion);
        ExtendedRoutingResult extendedRouting = intentRouterService.routeWithDataLayer(
                userQuestion, martHealthSnapshot == null ? Collections.emptyMap() : martHealthSnapshot);
        RoutingResult routing = extendedRouting.baseResult();

        String domain = resolveDomain(routing, templateMatch);
        String primaryTarget = resolvePrimaryTarget(routing, templateMatch, extendedRouting);
        List<String> secondaryTargets = routing != null && routing.secondaryViews() != null
                ? routing.secondaryViews()
                : List.of();
        String templateCode = templateMatch.matched() && templateMatch.template() != null
                ? templateMatch.template().getTemplateCode()
                : null;
        Optional<ReportCatalogEntry> reportCatalogMatch = reportCatalogService.findBestMatch(userQuestion, domain);
        Optional<BusinessObjectEntry> businessObjectMatch = businessObjectCatalogService.findBestMatch(userQuestion, domain);

        Optional<CatalogEntry> catalogMatch = directResponseCatalogService.findMatch(userQuestion);
        if (catalogMatch.isPresent()) {
            return new ConversationPlan(
                    PlanMode.DIRECT_RESPONSE,
                    ResponseKind.BUSINESS_DIRECT_RESPONSE,
                    buildDirectResponse(catalogMatch.get()),
                    domain,
                    primaryTarget,
                    secondaryTargets,
                    templateCode,
                    null,
                    extendedRouting.dataLayer().name(),
                    extendedRouting.martTable(),
                    ""
            );
        }

        if (templateMatch.matched() && StringUtils.hasText(templateMatch.resolvedSql())) {
            return new ConversationPlan(
                    PlanMode.TEMPLATE_FAST_PATH,
                    ResponseKind.TEMPLATE_SQL,
                    null,
                    domain,
                    primaryTarget,
                    secondaryTargets,
                    templateCode,
                    templateMatch.resolvedSql(),
                    extendedRouting.dataLayer().name(),
                    extendedRouting.martTable(),
                    buildBusinessRoutingPrompt(domain, primaryTarget, secondaryTargets, extendedRouting, templateCode,
                            templateMatch.resolvedSql()));
        }

        if (templateMatch.matched() && StringUtils.hasText(templateCode)) {
            return new ConversationPlan(
                    PlanMode.TEMPLATE_FAST_PATH,
                    ResponseKind.FIXED_REPORT,
                    null,
                    domain,
                    primaryTarget,
                    secondaryTargets,
                    templateCode,
                    null,
                    extendedRouting.dataLayer().name(),
                    extendedRouting.martTable(),
                    buildBusinessRoutingPrompt(domain, primaryTarget, secondaryTargets, extendedRouting, templateCode,
                            null));
        }

        if (businessObjectMatch.isPresent()) {
            return buildBusinessObjectPlan(
                    businessObjectMatch.get(),
                    domain,
                    secondaryTargets,
                    templateCode,
                    extendedRouting);
        }

        if (reportCatalogMatch.isPresent() && !isFixedReportCatalogEntry(reportCatalogMatch.get())) {
            return buildReportCatalogPlan(
                    reportCatalogMatch.get(),
                    domain,
                    secondaryTargets,
                    templateCode,
                    extendedRouting);
        }

        if (isGeneratedReportDraftRequest(userQuestion)) {
            String generatedDataSurface = resolveGeneratedReportDataSurface(extendedRouting, primaryTarget);
            return new ConversationPlan(
                    PlanMode.AGENT_WORKFLOW,
                    ResponseKind.REPORT_DRAFT,
                    null,
                    domain,
                    primaryTarget,
                    secondaryTargets,
                    templateCode,
                    null,
                    extendedRouting.dataLayer().name(),
                    extendedRouting.martTable(),
                    buildReportDraftPrompt(domain, primaryTarget, secondaryTargets, extendedRouting, templateCode),
                    generatedDataSurface,
                    "MEDIUM",
                    buildGeneratedReportQualityNotes(generatedDataSurface, extendedRouting),
                    resolveSuggestedDisplayFromQuestion(userQuestion),
                    null);
        }

        String fixedReportDomain = resolveFixedReportSuggestionDomain(userQuestion, domain);
        List<SuggestedQuestion> fixedReportCandidates = resolveFixedReportCandidates(userQuestion, fixedReportDomain);
        if (!fixedReportCandidates.isEmpty()) {
            return new ConversationPlan(
                    PlanMode.DIRECT_RESPONSE,
                    ResponseKind.FIXED_REPORT_CANDIDATES,
                    buildFixedReportCandidatesResponse(fixedReportDomain, fixedReportCandidates),
                    StringUtils.hasText(fixedReportDomain) ? fixedReportDomain : domain,
                    primaryTarget,
                    secondaryTargets,
                    null,
                    null,
                    extendedRouting.dataLayer().name(),
                    extendedRouting.martTable(),
                    ""
            );
        }

        if (routing == null || routing.needsClarification()) {
            ResponseKind kind = StringUtils.hasText(domain)
                    ? ResponseKind.BUSINESS_CLARIFICATION
                    : ResponseKind.GENERIC_ANALYSIS;
            return new ConversationPlan(
                    PlanMode.AGENT_WORKFLOW,
                    kind,
                    null,
                    domain,
                    primaryTarget,
                    secondaryTargets,
                    templateCode,
                    null,
                    extendedRouting.dataLayer().name(),
                    extendedRouting.martTable(),
                    buildPlannerClarificationPrompt(domain));
        }

        return new ConversationPlan(
                PlanMode.AGENT_WORKFLOW,
                ResponseKind.BUSINESS_ANALYSIS,
                null,
                domain,
                primaryTarget,
                secondaryTargets,
                templateCode,
                null,
                extendedRouting.dataLayer().name(),
                extendedRouting.martTable(),
                buildBusinessRoutingPrompt(domain, primaryTarget, secondaryTargets, extendedRouting, templateCode, null));
    }

    private String resolveDomain(RoutingResult routing, TemplateMatchResult templateMatch) {
        if (routing != null && StringUtils.hasText(routing.domain())) {
            return routing.domain();
        }
        if (templateMatch.matched() && templateMatch.template() != null) {
            return templateMatch.template().getDomain();
        }
        return null;
    }

    private String resolvePrimaryTarget(
            RoutingResult routing,
            TemplateMatchResult templateMatch,
            ExtendedRoutingResult extendedRouting) {
        if (extendedRouting != null
                && extendedRouting.dataLayer() == IntentRouterService.DataLayer.MART
                && StringUtils.hasText(extendedRouting.martTable())) {
            return extendedRouting.martTable();
        }
        if (routing != null && StringUtils.hasText(routing.primaryView())) {
            return routing.primaryView();
        }
        if (templateMatch.matched() && templateMatch.template() != null) {
            return templateMatch.template().getTargetView();
        }
        return null;
    }

    private String normalizeSemanticDomain(String domain) {
        if (!StringUtils.hasText(domain)) {
            return "";
        }
        return switch (domain) {
            case "settlement", "green" -> "project";
            case "task", "curing", "pendulum" -> "flowerbiz";
            default -> domain;
        };
    }

    private String buildDirectResponse(CatalogEntry entry) {
        if (!"BUSINESS_SCOPE_OVERVIEW".equals(entry.responseType())) {
            return "";
        }
        StringBuilder sb = new StringBuilder("当前已沉淀的业务分析范围包括：\n");
        for (String domain : semanticPackService.getDomains()) {
            String context = semanticPackService.getContextForDomain(domain);
            String firstLine = context.lines().findFirst().orElse("").replace("【主题域】", "").trim();
            sb.append("- ").append(firstLine.isEmpty() ? domain : firstLine).append("\n");
        }
        List<SuggestedQuestion> suggestions = templateMatcherService.getSuggestedQuestions(6);
        if (!suggestions.isEmpty()) {
            sb.append("\n可以直接这样问：\n");
            for (SuggestedQuestion suggestion : suggestions) {
                sb.append("- ").append(suggestion.question()).append("\n");
            }
        }
        return sb.toString().trim();
    }

    private String buildBusinessRoutingPrompt(
            String domain,
            String primaryTarget,
            List<String> secondaryTargets,
            ExtendedRoutingResult extendedRouting,
            String templateCode,
            String resolvedSql) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("【业务路由】\n");
        if (StringUtils.hasText(domain)) {
            prompt.append("- routed domain: ").append(domain).append("\n");
        }
        if (StringUtils.hasText(primaryTarget)) {
            prompt.append("- primary view: ").append(primaryTarget).append("\n");
        }
        if (!secondaryTargets.isEmpty()) {
            prompt.append("- secondary views: ").append(String.join(", ", secondaryTargets)).append("\n");
        }
        prompt.append("- data layer: ").append(extendedRouting.dataLayer().name()).append("\n");
        if (StringUtils.hasText(extendedRouting.martTable())) {
            prompt.append("- mart table: ").append(extendedRouting.martTable()).append("\n");
        }
        if (extendedRouting.fallbackApplied() && StringUtils.hasText(extendedRouting.fallbackReason())) {
            prompt.append("- fallback: ").append(extendedRouting.fallbackReason()).append("\n");
        }

        String semanticContext = semanticPackService.getContextForDomain(normalizeSemanticDomain(domain));
        if (StringUtils.hasText(semanticContext)) {
            prompt.append("\n").append(semanticContext.trim()).append("\n");
        }

        if (StringUtils.hasText(templateCode) && StringUtils.hasText(resolvedSql)) {
            prompt.append("\n【预制模板参考】\n");
            prompt.append("- template code: ").append(templateCode).append("\n");
            prompt.append(resolvedSql.trim()).append("\n");
        }
        return prompt.toString().trim();
    }

    private String buildPlannerClarificationPrompt(String domain) {
        StringBuilder prompt = new StringBuilder("""
                【planner-first 提示】
                - 当前问题可能缺少统计口径或业务范围，但不要直接返回固定的业务范围清单。
                - 优先结合当前数据源、schema_lookup 工具和现有上下文缩小问题范围。
                - 如果经过 schema 探索后仍无法确定，再用一句简洁问题追问最关键的缺失条件。
                - 不要输出编号式的固定澄清模板。
                """.trim());
        if (StringUtils.hasText(domain)) {
            prompt.append("\n- tentative domain: ").append(domain);
        }
        return prompt.toString();
    }

    private List<SuggestedQuestion> resolveFixedReportCandidates(String userQuestion, String fixedReportDomain) {
        if (!StringUtils.hasText(fixedReportDomain) || !isGenericReportQuestion(userQuestion)) {
            return List.of();
        }
        return templateMatcherService.getFixedReportSuggestionsByDomain(fixedReportDomain, 3);
    }

    private boolean isGenericReportQuestion(String userQuestion) {
        if (!StringUtils.hasText(userQuestion)) {
            return false;
        }
        return GENERIC_REPORT_KEYWORDS.stream().anyMatch(userQuestion::contains);
    }

    private boolean isGeneratedReportDraftRequest(String userQuestion) {
        if (!StringUtils.hasText(userQuestion)) {
            return false;
        }
        return GENERIC_REPORT_KEYWORDS.stream().anyMatch(userQuestion::contains)
                && GENERATED_REPORT_KEYWORDS.stream().anyMatch(userQuestion::contains);
    }

    private String buildReportDraftPrompt(
            String domain,
            String primaryTarget,
            List<String> secondaryTargets,
            ExtendedRoutingResult extendedRouting,
            String templateCode) {
        String routingPrompt = buildBusinessRoutingPrompt(
                domain,
                primaryTarget,
                secondaryTargets,
                extendedRouting,
                templateCode,
                null);
        StringBuilder prompt = new StringBuilder();
        if (StringUtils.hasText(routingPrompt)) {
            prompt.append(routingPrompt).append("\n\n");
        }
        prompt.append("""
                【报表草稿生成】
                - 用户明确要求生成新的报表/图表/看板草稿，不要改走固定报表候选清单。
                - 目标是创建可保存的分析草稿：先给出安全 SQL，再给出推荐图表形态和口径说明。
                - 优先使用 MART/ADS/DWS 聚合表；PRS/flowerbiz 主题优先使用 xycyl_ads_* 与 xycyl_dws_*，避免默认下钻 ODS。
                - 先对目标 dbt 模型调用 schema_lookup，确认字段存在；SQL 只能使用 schema_lookup 返回字段，不要猜测旧 ODS 字段。
                - SQL 必须是只读 SELECT 或 WITH...SELECT，并控制返回列数和行数。
                - 回复中必须包含一个 ```sql 代码块，前后说明要足够让用户确认口径后保存草稿。
                """.trim());
        return prompt.toString().trim();
    }

    private String resolveGeneratedReportDataSurface(ExtendedRoutingResult extendedRouting, String primaryTarget) {
        if (StringUtils.hasText(primaryTarget)) {
            String normalized = primaryTarget.toLowerCase();
            if (normalized.contains("xycyl_ads_") || normalized.contains("xycyl_dws_")
                    || normalized.contains("_ads_") || normalized.contains("_dws_")
                    || normalized.startsWith("mart.")) {
                return "L1_DBT_MART";
            }
            if (normalized.startsWith("/") || normalized.startsWith("authority.")) {
                return "L0_ADMINAPI_READONLY";
            }
        }
        if (extendedRouting.dataLayer() == IntentRouterService.DataLayer.MART) {
            return "L1_DBT_MART";
        }
        return "L1_SEMANTIC_VIEW";
    }

    private List<String> buildGeneratedReportQualityNotes(
            String dataSurface,
            ExtendedRoutingResult extendedRouting) {
        List<String> notes = new java.util.ArrayList<>();
        if ("L1_DBT_MART".equals(dataSurface)) {
            notes.add("优先使用主题汇总层生成报表，适合趋势、排行和汇总分析。");
        } else if ("L0_ADMINAPI_READONLY".equals(dataSurface)) {
            notes.add("业务明细和当前状态以 adminapi 只读边界为准，不直接写业务系统。");
        } else {
            notes.add("通用报表草稿基于语义视图生成，保存前需要确认过滤条件和业务口径。");
        }
        if (extendedRouting.fallbackApplied() && StringUtils.hasText(extendedRouting.fallbackReason())) {
            notes.add("主题层不可用时已回退：" + extendedRouting.fallbackReason());
        }
        return notes;
    }

    private String resolveSuggestedDisplayFromQuestion(String userQuestion) {
        if (!StringUtils.hasText(userQuestion)) {
            return "table";
        }
        if (containsAny(userQuestion, "趋势", "走势", "月度", "按月", "按日", "变化", "环比", "同比")) {
            return "line";
        }
        if (containsAny(userQuestion, "排行", "排名", "top", "最多", "最高", "最低")) {
            return "bar";
        }
        if (containsAny(userQuestion, "占比", "比例", "构成", "分布")) {
            return "pie";
        }
        if (containsAny(userQuestion, "总览", "指标", "kpi", "总数", "合计")) {
            return "scalar";
        }
        return "table";
    }

    private boolean containsAny(String text, String... patterns) {
        for (String pattern : patterns) {
            if (text.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    private boolean isFixedReportCatalogEntry(ReportCatalogEntry entry) {
        return ResponseKind.FIXED_REPORT.name().equals(entry.responseKind());
    }

    private ConversationPlan buildReportCatalogPlan(
            ReportCatalogEntry entry,
            String routedDomain,
            List<String> secondaryTargets,
            String templateCode,
            ExtendedRoutingResult extendedRouting) {
        ResponseKind responseKind = ResponseKind.valueOf(entry.responseKind());
        String domain = StringUtils.hasText(routedDomain) ? routedDomain : entry.domain();
        String promptContext = buildReportCatalogPrompt(entry, domain, secondaryTargets, extendedRouting, templateCode);
        return new ConversationPlan(
                PlanMode.AGENT_WORKFLOW,
                responseKind,
                null,
                domain,
                entry.primaryTarget(),
                secondaryTargets,
                templateCode,
                null,
                extendedRouting.dataLayer().name(),
                extendedRouting.martTable(),
                promptContext,
                entry.dataSurface(),
                entry.qualityLevel(),
                entry.qualityNotes(),
                entry.defaultDisplay(),
                entry.reportCode(),
                entry.sourceRefs());
    }

    private ConversationPlan buildBusinessObjectPlan(
            BusinessObjectEntry entry,
            String routedDomain,
            List<String> secondaryTargets,
            String templateCode,
            ExtendedRoutingResult extendedRouting) {
        ResponseKind responseKind = ResponseKind.valueOf(entry.responseKind());
        String domain = StringUtils.hasText(routedDomain) ? routedDomain : entry.domain();
        String promptContext = buildBusinessObjectPrompt(entry, domain, secondaryTargets, extendedRouting, templateCode);
        return new ConversationPlan(
                PlanMode.AGENT_WORKFLOW,
                responseKind,
                null,
                domain,
                entry.primaryTarget(),
                secondaryTargets,
                templateCode,
                null,
                extendedRouting.dataLayer().name(),
                extendedRouting.martTable(),
                promptContext,
                entry.dataSurface(),
                entry.qualityLevel(),
                entry.qualityNotes(),
                "table",
                entry.reportCode(),
                entry.sourceRefs());
    }

    private String buildBusinessObjectPrompt(
            BusinessObjectEntry entry,
            String domain,
            List<String> secondaryTargets,
            ExtendedRoutingResult extendedRouting,
            String templateCode) {
        StringBuilder prompt = new StringBuilder();
        prompt.append(buildBusinessRoutingPrompt(
                domain,
                entry.primaryTarget(),
                secondaryTargets,
                extendedRouting,
                templateCode,
                null));
        prompt.append("\n\n【业务对象目录】\n");
        prompt.append("- object code: ").append(entry.objectCode()).append("\n");
        prompt.append("- page path: ").append(entry.pagePath()).append("\n");
        prompt.append("- data surface: ").append(entry.dataSurface()).append("\n");
        prompt.append("- quality level: ").append(entry.qualityLevel()).append("\n");
        prompt.append("- readonly: ").append(entry.readOnly()).append("\n");
        prompt.append("- key fields: ").append(String.join(", ", entry.keyFields())).append("\n");
        prompt.append("- supported questions: ").append(String.join(", ", entry.supportedQuestions())).append("\n");
        if (!entry.sourceRefs().isEmpty()) {
            prompt.append("- source refs: ").append(String.join(", ", entry.sourceRefs())).append("\n");
        }
        if (!entry.qualityNotes().isEmpty()) {
            prompt.append("- quality notes: ").append(String.join("；", entry.qualityNotes())).append("\n");
        }
        prompt.append("""

                【业务对象问答】
                - 该问题命中业务对象，不要把裸 ODS 表当成默认经营报表资产。
                - 优先使用字段画像和业务对象目录回答字段分布、状态分布、TOP 值、时间范围和明细定位。
                - 只有用户需要具体明细或画像缺失时，才通过只读 ODS / DTS ODS 面查询，并先调用 schema_lookup 确认字段。
                - 输出结构化表格，摘要表固定表头为 `| 指标 | 结果 | 说明 |`，表格后补充业务页面路径和数据质量提示。
                - 不允许执行业务写操作；需要改变业务状态时只生成动作提案。
                """.trim());
        return prompt.toString().trim();
    }

    private String buildReportCatalogPrompt(
            ReportCatalogEntry entry,
            String domain,
            List<String> secondaryTargets,
            ExtendedRoutingResult extendedRouting,
            String templateCode) {
        StringBuilder prompt = new StringBuilder();
        prompt.append(buildBusinessRoutingPrompt(
                domain,
                entry.primaryTarget(),
                secondaryTargets,
                extendedRouting,
                templateCode,
                null));
        prompt.append("\n\n【Agent BI 报表目录】\n");
        prompt.append("- report code: ").append(entry.reportCode()).append("\n");
        prompt.append("- data surface: ").append(entry.dataSurface()).append("\n");
        prompt.append("- quality level: ").append(entry.qualityLevel()).append("\n");
        prompt.append("- suggested display: ").append(entry.defaultDisplay()).append("\n");
        if (!entry.sourceRefs().isEmpty()) {
            prompt.append("- source refs: ").append(String.join(", ", entry.sourceRefs())).append("\n");
        }
        if (!entry.qualityNotes().isEmpty()) {
            prompt.append("- quality notes: ").append(String.join("；", entry.qualityNotes())).append("\n");
        }
        if (ResponseKind.REPORT_DRAFT.name().equals(entry.responseKind())) {
            prompt.append("""

                    【报表草稿生成】
                    - 这是目录命中的自然语言导报表请求，即使用户没有显式说“生成报表”，也要返回可保存的 REPORT_DRAFT。
                    - SQL 必须只读，优先访问 data surface 指定的 dbt ADS/DWS 表。
                    - 先对 data surface 指定的 dbt 模型调用 schema_lookup；SQL 只能使用 schema_lookup 返回字段，不得绕过 MART/ADS/DWS 直接查 ODS。
                    - 默认时间窗口如用户未指定，使用 2025-05-01 至当前日期。
                    - 回复中必须包含一个 ```sql 代码块、推荐图表类型、口径说明和数据质量提示。
                    - 指标摘要和口径说明统一使用标准 Markdown 表格，报表摘要表固定表头为 `| 指标 | 结果 | 说明 |`。
                    - 不要只输出指标说明表，必须包含 ```sql 代码块；没有 SQL 时不要声称已生成报表草稿。
                    """.trim());
        } else if (ResponseKind.BUSINESS_DETAIL.name().equals(entry.responseKind())) {
            prompt.append("""

                    【业务明细查询】
                    - 该问题应走 L0 adminapi 只读边界，不要自行拼业务库写操作。
                    - 输出明细查询建议、业务页深链或只读接口参数，不要汇总成趋势图。
                    """.trim());
        } else if (ResponseKind.ACTION_PROPOSAL.name().equals(entry.responseKind())) {
            prompt.append("""

                    【受控动作提案】
                    - 不得直接调用业务写接口。
                    - 只生成动作提案、证据、风险说明和待用户确认的参数草稿。
                    - 高风险或会改变业务状态的动作必须等待人工确认。
                    """.trim());
        } else if (ResponseKind.BUSINESS_INSIGHT.name().equals(entry.responseKind())) {
            prompt.append("""

                    【业务洞察】
                    - 先输出证据报表，再输出建议。
                    - 若质量等级不是 HIGH，建议必须标注“需业务核验”。
                    """.trim());
        }
        return prompt.toString().trim();
    }

    private String resolveFixedReportSuggestionDomain(String userQuestion, String routedDomain) {
        if (StringUtils.hasText(userQuestion)) {
            if (userQuestion.contains("财务")) {
                return "财务";
            }
            if (userQuestion.contains("采购")) {
                return "采购";
            }
            if (userQuestion.contains("仓库") || userQuestion.contains("库存")) {
                return "仓库";
            }
            if (userQuestion.contains("PRS") || userQuestion.contains("报花")
                    || userQuestion.contains("租赁") || userQuestion.contains("租摆")) {
                return "flowerbiz";
            }
        }
        if (!StringUtils.hasText(routedDomain)) {
            return null;
        }
        return switch (routedDomain) {
            case "settlement" -> "财务";
            case "procurement", "purchase" -> "采购";
            case "warehouse", "inventory", "stock" -> "仓库";
            case "flowerbiz", "flower", "curing", "pendulum" -> "flowerbiz";
            default -> null;
        };
    }

    private String buildFixedReportCandidatesResponse(String domain, List<SuggestedQuestion> suggestions) {
        StringBuilder sb = new StringBuilder("当前更适合先查看已沉淀的固定报表");
        if (StringUtils.hasText(domain)) {
            sb.append("（").append(domain).append("）");
        }
        sb.append("，可以先试这几个：\n");
        for (SuggestedQuestion suggestion : suggestions) {
            sb.append("- ").append(suggestion.question()).append("\n");
        }
        sb.append("\n如果这些都不符合，再继续进入探索式分析。");
        return sb.toString().trim();
    }
}
