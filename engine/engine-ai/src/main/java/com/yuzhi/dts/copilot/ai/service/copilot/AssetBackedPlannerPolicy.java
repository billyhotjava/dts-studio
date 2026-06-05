package com.yuzhi.dts.copilot.ai.service.copilot;

import com.yuzhi.dts.copilot.ai.service.copilot.BusinessDirectResponseCatalogService.CatalogEntry;
import com.yuzhi.dts.copilot.ai.service.copilot.AgentBiReportCatalogService.ReportCatalogEntry;
import com.yuzhi.dts.copilot.ai.service.copilot.BusinessObjectCatalogService.BusinessObjectEntry;
import com.yuzhi.dts.copilot.ai.service.copilot.ConversationPlannerService.ConversationPlan;
import com.yuzhi.dts.copilot.ai.service.copilot.ConversationPlannerService.ConversationPlan.MetricCaliber;
import com.yuzhi.dts.copilot.ai.service.copilot.ConversationPlannerService.PlanMode;
import com.yuzhi.dts.copilot.ai.service.copilot.ConversationPlannerService.ResponseKind;
import com.yuzhi.dts.copilot.ai.service.copilot.IndicatorMatcherService.Confidence;
import com.yuzhi.dts.copilot.ai.service.copilot.IndicatorMatcherService.IndicatorMatch;
import com.yuzhi.dts.copilot.ai.service.copilot.IndicatorMatcherService.IndicatorMatchResult;
import com.yuzhi.dts.copilot.ai.service.copilot.IntentRouterService.ExtendedRoutingResult;
import com.yuzhi.dts.copilot.ai.service.copilot.IntentRouterService.DataLayer;
import com.yuzhi.dts.copilot.ai.service.copilot.IntentRouterService.RoutingResult;
import com.yuzhi.dts.copilot.ai.service.copilot.TemplateMatcherService.SuggestedQuestion;
import com.yuzhi.dts.copilot.ai.service.copilot.TemplateMatcherService.TemplateMatchResult;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
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

    private static final Set<String> ONTOLOGY_NAVIGATION_KEYWORDS = Set.of(
            "从", "到", "再到", "贯穿", "全流程", "全链路", "链路", "追溯", "溯源", "流转", "关联",
            "对应", "未结算", "还没结算", "结算"
    );

    private static final Set<String> SIGNAL_QUERY_KEYWORDS = Set.of(
            "风险", "异常", "预警", "告警", "需关注", "需要关注", "关注", "坏账", "欠费", "催收"
    );

    private static final String METRIC_FALLBACK_GENERATED = "__fallback_generated__";

    private enum RouteTier {
        TIER_1_PUBLISHED_INDICATOR("指标优先"),
        TIER_2_MART_TEMPLATE("dbt mart/template"),
        TIER_3_ONTOLOGY_OBJECT_GRAPH("本体对象图/信号"),
        TIER_4_GUARDRAIL_FEDERATED("受控联邦查询"),
        TIER_5_DIRECT_DETAIL("业务对象只读明细");

        private final String label;

        RouteTier(String label) {
            this.label = label;
        }

        String label() {
            return label;
        }
    }

    private final IntentRouterService intentRouterService;
    private final TemplateMatcherService templateMatcherService;
    private final SemanticPackService semanticPackService;
    private final OntologyService ontologyService;
    private final BusinessDirectResponseCatalogService directResponseCatalogService;
    private final AgentBiReportCatalogService reportCatalogService;
    private final BusinessObjectCatalogService businessObjectCatalogService;
    private final IndicatorMatcherService indicatorMatcherService;

    @Autowired
    public AssetBackedPlannerPolicy(IntentRouterService intentRouterService,
                                    TemplateMatcherService templateMatcherService,
                                    SemanticPackService semanticPackService,
                                    OntologyService ontologyService,
                                    BusinessDirectResponseCatalogService directResponseCatalogService,
                                    AgentBiReportCatalogService reportCatalogService,
                                    BusinessObjectCatalogService businessObjectCatalogService,
                                    IndicatorMatcherService indicatorMatcherService) {
        this.intentRouterService = intentRouterService;
        this.templateMatcherService = templateMatcherService;
        this.semanticPackService = semanticPackService;
        this.ontologyService = ontologyService;
        this.directResponseCatalogService = directResponseCatalogService;
        this.reportCatalogService = reportCatalogService;
        this.businessObjectCatalogService = businessObjectCatalogService;
        this.indicatorMatcherService = indicatorMatcherService;
    }

    public AssetBackedPlannerPolicy(IntentRouterService intentRouterService,
                                    TemplateMatcherService templateMatcherService,
                                    SemanticPackService semanticPackService,
                                    OntologyService ontologyService,
                                    BusinessDirectResponseCatalogService directResponseCatalogService,
                                    AgentBiReportCatalogService reportCatalogService,
                                    BusinessObjectCatalogService businessObjectCatalogService) {
        this(
                intentRouterService,
                templateMatcherService,
                semanticPackService,
                ontologyService,
                directResponseCatalogService,
                reportCatalogService,
                businessObjectCatalogService,
                null);
    }

    @Override
    public String mode() {
        return "asset";
    }

    @Override
    public ConversationPlan plan(String userQuestion, Map<String, Boolean> martHealthSnapshot) {
        return planInternal(userQuestion, martHealthSnapshot, null);
    }

    @Override
    public ConversationPlan plan(String userQuestion, CopilotChatRequestContext requestContext) {
        CopilotChatRequestContext context = requestContext == null
                ? CopilotChatRequestContext.empty()
                : requestContext;
        return planInternal(userQuestion, context.martHealthSnapshot(), context.assumptionOverrides().get("metric"));
    }

    private ConversationPlan planInternal(
            String userQuestion,
            Map<String, Boolean> martHealthSnapshot,
            String metricOverride) {
        RouteEvaluationContext context = buildRouteEvaluationContext(userQuestion, martHealthSnapshot, metricOverride);
        return assetRouteChain().stream()
                .map(route -> route.resolve(context))
                .flatMap(Optional::stream)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Asset route chain must provide a fallback plan"));
    }

    private RouteEvaluationContext buildRouteEvaluationContext(
            String userQuestion,
            Map<String, Boolean> martHealthSnapshot,
            String metricOverride) {
        RouteTrace routeTrace = new RouteTrace();
        IndicatorMatchResult indicatorMatch = indicatorMatcherService == null
                ? IndicatorMatchResult.none()
                : indicatorMatcherService.match(userQuestion);
        boolean skipIndicatorRoute = shouldSkipIndicatorRoute(metricOverride);
        TemplateMatchResult templateMatch = Optional.ofNullable(templateMatcherService.match(userQuestion))
                .orElse(new TemplateMatchResult(false, null, null, null));
        ExtendedRoutingResult extendedRouting = intentRouterService.routeWithDataLayer(
                userQuestion, martHealthSnapshot == null ? Collections.emptyMap() : martHealthSnapshot);
        if (extendedRouting == null) {
            extendedRouting = new ExtendedRoutingResult(
                    new RoutingResult(null, null, List.of(), 0.0, true),
                    DataLayer.VIEW,
                    null,
                    false,
                    null);
        }
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

        return new RouteEvaluationContext(
                userQuestion,
                metricOverride,
                routeTrace,
                indicatorMatch,
                skipIndicatorRoute,
                templateMatch,
                extendedRouting,
                routing,
                domain,
                primaryTarget,
                secondaryTargets,
                templateCode,
                reportCatalogMatch,
                businessObjectMatch,
                catalogMatch);
    }

    private List<PlanRoute> assetRouteChain() {
        return List.of(
                this::tryPublishedIndicatorRoute,
                this::tryDirectResponseRoute,
                this::tryTemplateSqlRoute,
                this::tryFixedReportTemplateRoute,
                this::tryPreferredReportCatalogRoute,
                this::trySignalQueryRoute,
                this::tryBusinessObjectRoute,
                this::tryOntologyNavigationRoute,
                this::tryReportCatalogRoute,
                this::tryGeneratedReportDraftRoute,
                this::tryFixedReportCandidateRoute,
                this::tryClarificationRoute,
                this::buildFallbackBusinessAnalysisRoute);
    }

    private Optional<ConversationPlan> tryPublishedIndicatorRoute(RouteEvaluationContext context) {
        if (!context.skipIndicatorRoute() && shouldUsePublishedIndicator(context.indicatorMatch())) {
            return Optional.of(buildPublishedIndicatorPlan(context.indicatorMatch(), context.metricOverride())
                    .withRouteTrace(context.routeTrace().hit(
                            RouteTier.TIER_1_PUBLISHED_INDICATOR,
                            "命中 dts-platform 已发布指标",
                            "indicator:" + selectIndicatorMatch(
                                    context.indicatorMatch(),
                                    context.metricOverride()).code())));
        }
        context.routeTrace().miss(
                RouteTier.TIER_1_PUBLISHED_INDICATOR,
                context.skipIndicatorRoute() ? "用户选择生成 SQL，跳过已发布指标" : "未命中高可信已发布指标");
        return Optional.empty();
    }

    private Optional<ConversationPlan> tryDirectResponseRoute(RouteEvaluationContext context) {
        if (context.catalogMatch().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new ConversationPlan(
                    PlanMode.DIRECT_RESPONSE,
                    ResponseKind.BUSINESS_DIRECT_RESPONSE,
                    buildDirectResponse(context.catalogMatch().get()),
                    context.domain(),
                    context.primaryTarget(),
                    context.secondaryTargets(),
                    context.templateCode(),
                    null,
                    context.extendedRouting().dataLayer().name(),
                    context.extendedRouting().martTable(),
                    ""
            ));
    }

    private Optional<ConversationPlan> tryTemplateSqlRoute(RouteEvaluationContext context) {
        TemplateMatchResult templateMatch = context.templateMatch();
        if (!(templateMatch.matched() && StringUtils.hasText(templateMatch.resolvedSql()))) {
            return Optional.empty();
        }
        return Optional.of(new ConversationPlan(
                    PlanMode.TEMPLATE_FAST_PATH,
                    ResponseKind.TEMPLATE_SQL,
                    null,
                    context.domain(),
                    context.primaryTarget(),
                    context.secondaryTargets(),
                    context.templateCode(),
                    templateMatch.resolvedSql(),
                    context.extendedRouting().dataLayer().name(),
                    context.extendedRouting().martTable(),
                    buildBusinessRoutingPrompt(
                            context.domain(),
                            context.primaryTarget(),
                            context.secondaryTargets(),
                            context.extendedRouting(),
                            context.templateCode(),
                            templateMatch.resolvedSql()))
                    .withRouteTrace(context.routeTrace().hit(
                            RouteTier.TIER_2_MART_TEMPLATE,
                            "命中可执行模板 SQL",
                            context.templateCode())));
    }

    private Optional<ConversationPlan> tryFixedReportTemplateRoute(RouteEvaluationContext context) {
        TemplateMatchResult templateMatch = context.templateMatch();
        if (!(templateMatch.matched() && StringUtils.hasText(context.templateCode()))) {
            return Optional.empty();
        }
        return Optional.of(new ConversationPlan(
                    PlanMode.TEMPLATE_FAST_PATH,
                    ResponseKind.FIXED_REPORT,
                    null,
                    context.domain(),
                    context.primaryTarget(),
                    context.secondaryTargets(),
                    context.templateCode(),
                    null,
                    context.extendedRouting().dataLayer().name(),
                    context.extendedRouting().martTable(),
                    buildBusinessRoutingPrompt(
                            context.domain(),
                            context.primaryTarget(),
                            context.secondaryTargets(),
                            context.extendedRouting(),
                            context.templateCode(),
                            null))
                    .withRouteTrace(context.routeTrace().hit(
                            RouteTier.TIER_2_MART_TEMPLATE,
                            "命中固定报表/资产模板",
                            context.templateCode())));
    }

    private Optional<ConversationPlan> tryPreferredReportCatalogRoute(RouteEvaluationContext context) {
        if (context.reportCatalogMatch().isEmpty()
                || !shouldPreferFixedReportCatalog(context.userQuestion(), context.reportCatalogMatch().get())) {
            return Optional.empty();
        }
        return Optional.of(buildReportCatalogPlan(
                    context.reportCatalogMatch().get(),
                    context.domain(),
                    context.secondaryTargets(),
                    context.templateCode(),
                    context.extendedRouting())
                    .withRouteTrace(context.routeTrace().hit(
                            RouteTier.TIER_2_MART_TEMPLATE,
                            "命中固定报表/资产目录",
                            resolveRouteTarget(context.reportCatalogMatch().get()))));
    }

    private Optional<ConversationPlan> trySignalQueryRoute(RouteEvaluationContext context) {
        Optional<SignalQuery> signalQuery = resolveSignalQuery(context.userQuestion(), context.domain());
        if (signalQuery.isPresent()) {
            context.routeTrace().miss(
                    RouteTier.TIER_2_MART_TEMPLATE,
                    "未命中需要优先返回的可执行模板或固定报表资产");
            return Optional.of(buildSignalQueryPlan(
                    signalQuery.get(),
                    context.domain(),
                    context.secondaryTargets(),
                    context.templateCode(),
                    context.extendedRouting())
                    .withRouteTrace(context.routeTrace().hit(
                            RouteTier.TIER_3_ONTOLOGY_OBJECT_GRAPH,
                            "命中 semantic pack signal",
                            "ontology:" + signalQuery.get().domain() + ":signals")));
        }
        return Optional.empty();
    }

    private Optional<ConversationPlan> tryBusinessObjectRoute(RouteEvaluationContext context) {
        Optional<OntologyNavigation> ontologyNavigation = resolveOntologyNavigation(context.userQuestion(), context.domain());

        if (context.businessObjectMatch().isPresent() && ontologyNavigation.isEmpty()) {
            context.routeTrace().miss(
                    RouteTier.TIER_2_MART_TEMPLATE,
                    "未命中需要优先返回的可执行模板或固定报表资产");
            context.routeTrace().missIfAbsent(
                    RouteTier.TIER_3_ONTOLOGY_OBJECT_GRAPH,
                    "未命中本体对象链路或 semantic pack signal");
            context.routeTrace().missIfAbsent(
                    RouteTier.TIER_4_GUARDRAIL_FEDERATED,
                    "未进入泛化联邦分析，优先使用已登记业务对象只读画像");
            return Optional.of(buildBusinessObjectPlan(
                    context.businessObjectMatch().get(),
                    context.domain(),
                    context.secondaryTargets(),
                    context.templateCode(),
                    context.extendedRouting())
                    .withRouteTrace(context.routeTrace().hit(
                            RouteTier.TIER_5_DIRECT_DETAIL,
                            "命中业务对象画像",
                            context.businessObjectMatch().get().primaryTarget())));
        }
        return Optional.empty();
    }

    private Optional<ConversationPlan> tryOntologyNavigationRoute(RouteEvaluationContext context) {
        Optional<OntologyNavigation> ontologyNavigation = resolveOntologyNavigation(context.userQuestion(), context.domain());
        if (ontologyNavigation.isPresent()) {
            context.routeTrace().miss(
                    RouteTier.TIER_2_MART_TEMPLATE,
                    "未命中需要优先返回的可执行模板或固定报表资产");
            return Optional.of(buildOntologyNavigationPlan(
                    ontologyNavigation.get(),
                    context.domain(),
                    context.secondaryTargets(),
                    context.templateCode(),
                    context.extendedRouting())
                    .withRouteTrace(context.routeTrace().hit(
                            RouteTier.TIER_3_ONTOLOGY_OBJECT_GRAPH,
                            "命中对象图链路",
                            "ontology:" + ontologyNavigation.get().domain())));
        }
        return Optional.empty();
    }

    private Optional<ConversationPlan> tryReportCatalogRoute(RouteEvaluationContext context) {
        if (context.reportCatalogMatch().isEmpty()) {
            return Optional.empty();
        }
        ReportCatalogEntry reportCatalogEntry = context.reportCatalogMatch().get();
        return Optional.of(buildReportCatalogPlan(
                    reportCatalogEntry,
                    context.domain(),
                    context.secondaryTargets(),
                    context.templateCode(),
                    context.extendedRouting())
                    .withRouteTrace(buildReportCatalogRouteTrace(context.routeTrace(), reportCatalogEntry)));
    }

    private Optional<ConversationPlan> tryGeneratedReportDraftRoute(RouteEvaluationContext context) {
        if (!isGeneratedReportDraftRequest(context.userQuestion())) {
            return Optional.empty();
        }
        String generatedDataSurface = resolveGeneratedReportDataSurface(context.extendedRouting(), context.primaryTarget());
        context.routeTrace().miss(
                    RouteTier.TIER_2_MART_TEMPLATE,
                    "未命中需要优先返回的可执行模板或固定报表资产");
        context.routeTrace().missIfAbsent(
                    RouteTier.TIER_3_ONTOLOGY_OBJECT_GRAPH,
                    "未命中本体对象链路或 semantic pack signal");
        return Optional.of(new ConversationPlan(
                    PlanMode.AGENT_WORKFLOW,
                    ResponseKind.REPORT_DRAFT,
                    null,
                    context.domain(),
                    context.primaryTarget(),
                    context.secondaryTargets(),
                    context.templateCode(),
                    null,
                    context.extendedRouting().dataLayer().name(),
                    context.extendedRouting().martTable(),
                    buildReportDraftPrompt(
                            context.domain(),
                            context.primaryTarget(),
                            context.secondaryTargets(),
                            context.extendedRouting(),
                            context.templateCode()),
                    generatedDataSurface,
                    "MEDIUM",
                    buildGeneratedReportQualityNotes(generatedDataSurface, context.extendedRouting()),
                    resolveSuggestedDisplayFromQuestion(context.userQuestion()),
                    null)
                    .withRouteTrace(context.routeTrace().hit(
                            RouteTier.TIER_4_GUARDRAIL_FEDERATED,
                            "进入现生成报表草稿路径",
                            context.primaryTarget())));
    }

    private Optional<ConversationPlan> tryFixedReportCandidateRoute(RouteEvaluationContext context) {
        String fixedReportDomain = resolveFixedReportSuggestionDomain(context.userQuestion(), context.domain());
        List<SuggestedQuestion> fixedReportCandidates = resolveFixedReportCandidates(context.userQuestion(), fixedReportDomain);
        if (!fixedReportCandidates.isEmpty()) {
            return Optional.of(new ConversationPlan(
                    PlanMode.DIRECT_RESPONSE,
                    ResponseKind.FIXED_REPORT_CANDIDATES,
                    buildFixedReportCandidatesResponse(fixedReportDomain, fixedReportCandidates),
                    StringUtils.hasText(fixedReportDomain) ? fixedReportDomain : context.domain(),
                    context.primaryTarget(),
                    context.secondaryTargets(),
                    null,
                    null,
                    context.extendedRouting().dataLayer().name(),
                    context.extendedRouting().martTable(),
                    ""
            ));
        }
        return Optional.empty();
    }

    private Optional<ConversationPlan> tryClarificationRoute(RouteEvaluationContext context) {
        if (context.routing() == null || context.routing().needsClarification()) {
            ResponseKind kind = StringUtils.hasText(context.domain())
                    ? ResponseKind.BUSINESS_CLARIFICATION
                    : ResponseKind.GENERIC_ANALYSIS;
            context.routeTrace().miss(
                    RouteTier.TIER_2_MART_TEMPLATE,
                    "未命中需要优先返回的可执行模板或固定报表资产");
            context.routeTrace().missIfAbsent(
                    RouteTier.TIER_3_ONTOLOGY_OBJECT_GRAPH,
                    "未命中本体对象链路或 semantic pack signal");
            return Optional.of(new ConversationPlan(
                    PlanMode.AGENT_WORKFLOW,
                    kind,
                    null,
                    context.domain(),
                    context.primaryTarget(),
                    context.secondaryTargets(),
                    context.templateCode(),
                    null,
                    context.extendedRouting().dataLayer().name(),
                    context.extendedRouting().martTable(),
                    buildPlannerClarificationPrompt(context.domain()))
                    .withRouteTrace(context.routeTrace().hit(
                            RouteTier.TIER_4_GUARDRAIL_FEDERATED,
                            "需要继续澄清或探索",
                            context.primaryTarget())));
        }
        return Optional.empty();
    }

    private Optional<ConversationPlan> buildFallbackBusinessAnalysisRoute(RouteEvaluationContext context) {
        context.routeTrace().miss(
                RouteTier.TIER_2_MART_TEMPLATE,
                "未命中需要优先返回的可执行模板或固定报表资产");
        context.routeTrace().missIfAbsent(
                RouteTier.TIER_3_ONTOLOGY_OBJECT_GRAPH,
                "未命中本体对象链路或 semantic pack signal");
        return Optional.of(new ConversationPlan(
                PlanMode.AGENT_WORKFLOW,
                ResponseKind.BUSINESS_ANALYSIS,
                null,
                context.domain(),
                context.primaryTarget(),
                context.secondaryTargets(),
                context.templateCode(),
                null,
                context.extendedRouting().dataLayer().name(),
                context.extendedRouting().martTable(),
                buildBusinessRoutingPrompt(
                        context.domain(),
                        context.primaryTarget(),
                        context.secondaryTargets(),
                        context.extendedRouting(),
                        context.templateCode(),
                        null))
                .withRouteTrace(context.routeTrace().hit(
                        RouteTier.TIER_4_GUARDRAIL_FEDERATED,
                        "进入通用业务分析路径",
                        context.primaryTarget())));
    }

    private boolean shouldSkipIndicatorRoute(String metricOverride) {
        return METRIC_FALLBACK_GENERATED.equals(metricOverride);
    }

    private boolean shouldUsePublishedIndicator(IndicatorMatchResult result) {
        if (result == null || result.candidates().isEmpty()) {
            return false;
        }
        return result.tier() == Confidence.HIGH || result.tier() == Confidence.MEDIUM;
    }

    private ConversationPlan buildPublishedIndicatorPlan(IndicatorMatchResult result, String metricOverride) {
        IndicatorMatch match = selectIndicatorMatch(result, metricOverride);
        String qualityLevel = result.tier() == Confidence.HIGH ? "HIGH" : "MEDIUM";
        List<String> candidateNames = result.candidates().stream()
                .map(IndicatorMatch::name)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        return new ConversationPlan(
                PlanMode.AGENT_WORKFLOW,
                ResponseKind.PUBLISHED_INDICATOR,
                null,
                match.domain(),
                "indicator:" + match.code(),
                candidateNames,
                null,
                null,
                "PUBLISHED_INDICATOR",
                null,
                buildPublishedIndicatorPrompt(match, result),
                "L3_PUBLISHED_INDICATOR",
                qualityLevel,
                buildPublishedIndicatorQualityNotes(result),
                "table",
                match.code(),
                List.of("platform-indicator:" + match.code()),
                new MetricCaliber(
                        match.name(),
                        match.expressionSql(),
                        match.domain(),
                        match.version(),
                        StringUtils.hasText(match.id()) ? match.id() : match.code()));
    }

    private IndicatorMatch selectIndicatorMatch(IndicatorMatchResult result, String metricOverride) {
        if (StringUtils.hasText(metricOverride)) {
            String requested = metricOverride.trim();
            return result.candidates().stream()
                    .filter(candidate -> requested.equals(candidate.name()) || requested.equals(candidate.code()))
                    .findFirst()
                    .orElse(result.candidates().getFirst());
        }
        return result.candidates().getFirst();
    }

    private String buildPublishedIndicatorPrompt(IndicatorMatch match, IndicatorMatchResult result) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("【平台指标目录】\n");
        prompt.append("- indicator code: ").append(match.code()).append("\n");
        prompt.append("- indicator name: ").append(match.name()).append("\n");
        prompt.append("- category/domain: ").append(match.category()).append("/").append(match.domain()).append("\n");
        prompt.append("- data surface: L3_PUBLISHED_INDICATOR\n");
        prompt.append("- version: ").append(match.version()).append("\n");
        prompt.append("- confidence: ").append(String.format(java.util.Locale.ROOT, "%.2f", match.confidence())).append("\n");
        if (StringUtils.hasText(match.definition())) {
            prompt.append("- definition: ").append(match.definition()).append("\n");
        }
        if (StringUtils.hasText(match.expressionSql())) {
            prompt.append("- expression sql: ").append(match.expressionSql()).append("\n");
        }
        if (!match.matchedSignals().isEmpty()) {
            prompt.append("- matched signals: ").append(String.join(", ", match.matchedSignals())).append("\n");
        }
        if (result.candidates().size() > 1) {
            prompt.append("- candidates: ").append(result.candidates().stream()
                    .map(candidate -> candidate.name() + "(" + candidate.code() + ")")
                    .toList()).append("\n");
        }
        prompt.append("""

                【执行约束】
                - 该问题已命中 dts-platform 已发布指标,口径以平台 definition/expressionSql/version 为准。
                - 优先返回平台指标口径与取值结果;若平台指标服务不可达,必须显式说明并退回现生成 SQL,不能静默伪造权威结果。
                - 不要重新解释为固定报表或 ODS 明细扫描;用户要求改指标时按候选指标重新路由。
                """.trim());
        return prompt.toString().trim();
    }

    private List<String> buildPublishedIndicatorQualityNotes(IndicatorMatchResult result) {
        List<String> notes = new ArrayList<>();
        notes.add("命中 dts-platform 已发布指标,口径以平台指标目录为准。");
        if (result.candidates().size() > 1) {
            notes.add("存在多个指标候选,默认采用最高置信候选,用户可切换指标或退回现生成 SQL。");
        }
        return List.copyOf(notes);
    }

    private String resolveDomain(RoutingResult routing, TemplateMatchResult templateMatch) {
        if (templateMatch.matched()
                && templateMatch.template() != null
                && StringUtils.hasText(templateMatch.template().getDomain())) {
            return templateMatch.template().getDomain();
        }
        if (routing != null && StringUtils.hasText(routing.domain())) {
            return routing.domain();
        }
        return null;
    }

    private String resolvePrimaryTarget(
            RoutingResult routing,
            TemplateMatchResult templateMatch,
            ExtendedRoutingResult extendedRouting) {
        if (templateMatch.matched()
                && templateMatch.template() != null
                && StringUtils.hasText(templateMatch.template().getTargetView())) {
            return templateMatch.template().getTargetView();
        }
        if (extendedRouting != null
                && extendedRouting.dataLayer() == IntentRouterService.DataLayer.MART
                && StringUtils.hasText(extendedRouting.martTable())) {
            return extendedRouting.martTable();
        }
        if (routing != null && StringUtils.hasText(routing.primaryView())) {
            return routing.primaryView();
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

    private Optional<OntologyNavigation> resolveOntologyNavigation(String userQuestion, String domain) {
        if (!StringUtils.hasText(userQuestion) || !containsOntologyNavigationKeyword(userQuestion)) {
            return Optional.empty();
        }
        String semanticDomain = normalizeSemanticDomain(domain);
        if (!StringUtils.hasText(semanticDomain)) {
            return Optional.empty();
        }
        return ontologyService.load(semanticDomain).flatMap(model -> {
            List<String> objectNames = resolveMentionedOntologyObjects(userQuestion, model);
            if (objectNames.size() < 2) {
                return Optional.empty();
            }
            List<OntologyService.JoinPlan> candidates = model.buildJoinPlans(
                    objectNames.getFirst(),
                    objectNames.getLast());
            if (candidates.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new OntologyNavigation(
                    semanticDomain,
                    objectNames,
                    selectJoinPlanCoveringMentions(model, objectNames, candidates),
                    candidates.size()));
        });
    }

    private boolean containsOntologyNavigationKeyword(String userQuestion) {
        return ONTOLOGY_NAVIGATION_KEYWORDS.stream().anyMatch(userQuestion::contains);
    }

    private List<String> resolveMentionedOntologyObjects(
            String userQuestion,
            OntologyService.OntologyModel model) {
        List<ObjectMention> mentions = new ArrayList<>();
        for (SemanticPackService.SemanticObject object : model.objects()) {
            int index = earliestObjectAliasIndex(userQuestion, object.name());
            if (index >= 0) {
                mentions.add(new ObjectMention(object.name(), index));
            }
        }
        mentions.sort((left, right) -> Integer.compare(left.index(), right.index()));

        Set<String> orderedUniqueNames = new LinkedHashSet<>();
        for (ObjectMention mention : mentions) {
            orderedUniqueNames.add(mention.objectName());
        }
        return List.copyOf(orderedUniqueNames);
    }

    private int earliestObjectAliasIndex(String userQuestion, String objectName) {
        int earliest = -1;
        for (String alias : ontologyObjectAliases(objectName)) {
            int index = userQuestion.indexOf(alias);
            if (index >= 0 && (earliest < 0 || index < earliest)) {
                earliest = index;
            }
        }
        return earliest;
    }

    private Set<String> ontologyObjectAliases(String objectName) {
        Set<String> aliases = new LinkedHashSet<>();
        aliases.add(objectName);
        addWithoutSuffix(aliases, objectName, "明细");
        addWithoutSuffix(aliases, objectName, "单");
        if (objectName.contains("报花")) {
            aliases.add("报花");
            aliases.add("报花单");
            aliases.add("租赁报花");
        }
        if (objectName.contains("客户")) {
            aliases.add("客户");
        }
        if (objectName.contains("项目")) {
            aliases.add("项目");
            aliases.add("项目点");
        }
        if (objectName.contains("采购")) {
            aliases.add("采购");
            aliases.add("采购明细");
        }
        if (objectName.contains("结算")) {
            aliases.add("结算");
            aliases.add("结算单");
        }
        aliases.removeIf(alias -> !StringUtils.hasText(alias));
        return aliases;
    }

    private void addWithoutSuffix(Set<String> aliases, String objectName, String suffix) {
        if (objectName.endsWith(suffix) && objectName.length() > suffix.length()) {
            aliases.add(objectName.substring(0, objectName.length() - suffix.length()));
        }
    }

    private OntologyService.JoinPlan selectJoinPlanCoveringMentions(
            OntologyService.OntologyModel model,
            List<String> objectNames,
            List<OntologyService.JoinPlan> candidates) {
        Set<String> requiredViews = new LinkedHashSet<>();
        for (String objectName : objectNames) {
            model.getObject(objectName).map(SemanticPackService.SemanticObject::view).ifPresent(requiredViews::add);
        }
        for (OntologyService.JoinPlan candidate : candidates) {
            if (candidate.sourceRefs().containsAll(requiredViews)) {
                return candidate;
            }
        }
        return candidates.getFirst();
    }

    private ConversationPlan buildOntologyNavigationPlan(
            OntologyNavigation navigation,
            String routedDomain,
            List<String> secondaryTargets,
            String templateCode,
            ExtendedRoutingResult extendedRouting) {
        String domain = StringUtils.hasText(routedDomain) ? routedDomain : navigation.domain();
        return new ConversationPlan(
                PlanMode.AGENT_WORKFLOW,
                ResponseKind.OBJECT_GRAPH_NAVIGATION,
                null,
                domain,
                "ontology:" + navigation.domain(),
                secondaryTargets,
                templateCode,
                null,
                extendedRouting.dataLayer().name(),
                extendedRouting.martTable(),
                buildOntologyNavigationPrompt(navigation, domain, secondaryTargets, extendedRouting, templateCode),
                "L1_ONTOLOGY_GRAPH",
                "MEDIUM",
                buildOntologyNavigationQualityNotes(navigation),
                "table",
                "ontology." + navigation.domain() + ".object_graph.navigation",
                navigation.joinPlan().sourceRefs());
    }

    private String buildOntologyNavigationPrompt(
            OntologyNavigation navigation,
            String domain,
            List<String> secondaryTargets,
            ExtendedRoutingResult extendedRouting,
            String templateCode) {
        StringBuilder prompt = new StringBuilder();
        String routingPrompt = buildBusinessRoutingPrompt(
                domain,
                "ontology:" + navigation.domain(),
                secondaryTargets,
                extendedRouting,
                templateCode,
                null);
        if (StringUtils.hasText(routingPrompt)) {
            prompt.append(routingPrompt).append("\n\n");
        }
        prompt.append("【对象图导航】\n");
        prompt.append("- mentioned objects: ").append(String.join(" -> ", navigation.objectNames())).append("\n");
        prompt.append("- link path: ").append(String.join(" -> ", navigation.joinPlan().linkNames())).append("\n");
        prompt.append("- data surface: L1_ONTOLOGY_GRAPH\n");
        prompt.append("- source refs: ").append(String.join(", ", navigation.joinPlan().sourceRefs())).append("\n");
        if (!navigation.joinPlan().joinHints().isEmpty()) {
            prompt.append("- join hints: ").append(String.join("；", navigation.joinPlan().joinHints())).append("\n");
        }
        if (navigation.candidateCount() > 1) {
            prompt.append("- candidate paths: ").append(navigation.candidateCount()).append("\n");
        }
        prompt.append("""

                【导航 SQL】
                ```sql
                """);
        prompt.append(navigation.joinPlan().sql()).append("\n");
        prompt.append("""
                ```

                【执行约束】
                - 该分支用于跨对象贯穿/追溯问题，不要退回单一业务对象画像或单视图 NL2SQL。
                - 执行前先对 source refs 调用 schema_lookup 校验字段，SQL 只读并保留 LEFT JOIN 以避免丢失孤儿记录。
                - 如果用户追问具体明细，再基于该对象链路补充过滤条件和展示列。
                """.trim());
        return prompt.toString().trim();
    }

    private List<String> buildOntologyNavigationQualityNotes(OntologyNavigation navigation) {
        List<String> notes = new ArrayList<>();
        notes.add("对象图导航基于 semantic pack links 生成 JOIN 链路，执行前需要 schema_lookup 校验字段。");
        if (navigation.joinPlan().preservesOrphans()) {
            notes.add("JOIN 使用 LEFT JOIN 保留可能缺少下游记录的业务对象。");
        }
        if (!navigation.joinPlan().joinHints().isEmpty()) {
            notes.add(String.join("；", navigation.joinPlan().joinHints()));
        }
        if (navigation.candidateCount() > 1) {
            notes.add("存在多条候选最短路径，当前选择覆盖用户提及对象最多的路径。");
        }
        return List.copyOf(notes);
    }

    private boolean shouldPreferFixedReportCatalog(String userQuestion, ReportCatalogEntry entry) {
        if (entry == null || !ResponseKind.FIXED_REPORT.name().equals(entry.responseKind())) {
            return false;
        }
        if (!StringUtils.hasText(userQuestion)) {
            return false;
        }
        return userQuestion.contains("打开")
                || userQuestion.contains("进入")
                || userQuestion.contains("报表")
                || userQuestion.contains("看板")
                || userQuestion.contains("大屏")
                || userQuestion.contains("月报");
    }

    private Optional<SignalQuery> resolveSignalQuery(String userQuestion, String domain) {
        if (!StringUtils.hasText(userQuestion) || !containsSignalKeyword(userQuestion)) {
            return Optional.empty();
        }
        String semanticDomain = normalizeSemanticDomain(domain);
        if (!StringUtils.hasText(semanticDomain)) {
            return Optional.empty();
        }
        return ontologyService.load(semanticDomain).flatMap(model -> {
            List<OntologyService.SignalPlan> allPlans = model.buildSignalPlans();
            if (allPlans.isEmpty()) {
                return Optional.empty();
            }
            List<OntologyService.SignalPlan> matchedPlans = allPlans.stream()
                    .filter(plan -> signalPlanMatchesQuestion(userQuestion, plan))
                    .toList();
            return Optional.of(new SignalQuery(
                    semanticDomain,
                    matchedPlans.isEmpty() ? allPlans : matchedPlans));
        });
    }

    private boolean containsSignalKeyword(String userQuestion) {
        return SIGNAL_QUERY_KEYWORDS.stream().anyMatch(userQuestion::contains);
    }

    private boolean signalPlanMatchesQuestion(String userQuestion, OntologyService.SignalPlan plan) {
        if (userQuestion.contains(plan.signalName()) || userQuestion.contains(plan.objectName())) {
            return true;
        }
        for (String metricName : plan.metricNames()) {
            if (userQuestion.contains(metricName)) {
                return true;
            }
        }
        for (String alias : signalAliases(plan.signalName())) {
            if (userQuestion.contains(alias)) {
                return true;
            }
        }
        return false;
    }

    private Set<String> signalAliases(String signalName) {
        Set<String> aliases = new LinkedHashSet<>();
        aliases.add(signalName);
        addWithoutSuffix(aliases, signalName, "风险");
        addWithoutSuffix(aliases, signalName, "预警");
        addWithoutSuffix(aliases, signalName, "异常");
        addWithoutSuffix(aliases, signalName, "告警");
        aliases.removeIf(alias -> !StringUtils.hasText(alias) || alias.length() < 2);
        return aliases;
    }

    private ConversationPlan buildSignalQueryPlan(
            SignalQuery signalQuery,
            String routedDomain,
            List<String> secondaryTargets,
            String templateCode,
            ExtendedRoutingResult extendedRouting) {
        String domain = StringUtils.hasText(routedDomain) ? routedDomain : signalQuery.domain();
        Set<String> sourceRefs = new LinkedHashSet<>();
        for (OntologyService.SignalPlan plan : signalQuery.plans()) {
            sourceRefs.addAll(plan.sourceRefs());
        }
        return new ConversationPlan(
                PlanMode.AGENT_WORKFLOW,
                ResponseKind.RISK_SIGNAL_QUERY,
                null,
                domain,
                "ontology:" + signalQuery.domain() + ":signals",
                secondaryTargets,
                templateCode,
                null,
                extendedRouting.dataLayer().name(),
                extendedRouting.martTable(),
                buildSignalQueryPrompt(signalQuery, domain, secondaryTargets, extendedRouting, templateCode),
                "L2_ONTOLOGY_SIGNAL",
                "MEDIUM",
                buildSignalQueryQualityNotes(signalQuery),
                "table",
                "ontology." + signalQuery.domain() + ".signals",
                List.copyOf(sourceRefs));
    }

    private String buildSignalQueryPrompt(
            SignalQuery signalQuery,
            String domain,
            List<String> secondaryTargets,
            ExtendedRoutingResult extendedRouting,
            String templateCode) {
        StringBuilder prompt = new StringBuilder();
        String routingPrompt = buildBusinessRoutingPrompt(
                domain,
                "ontology:" + signalQuery.domain() + ":signals",
                secondaryTargets,
                extendedRouting,
                templateCode,
                null);
        if (StringUtils.hasText(routingPrompt)) {
            prompt.append(routingPrompt).append("\n\n");
        }
        prompt.append("【预警查询】\n");
        prompt.append("- data surface: L2_ONTOLOGY_SIGNAL\n");
        for (OntologyService.SignalPlan plan : signalQuery.plans()) {
            prompt.append("- signal: ").append(plan.signalName()).append("\n");
            prompt.append("  severity: ").append(plan.severity()).append("\n");
            prompt.append("  object: ").append(plan.objectName()).append("\n");
            prompt.append("  metrics: ").append(String.join(", ", plan.metricNames())).append("\n");
            prompt.append("  advice: ").append(plan.advice()).append("\n");
            if (!plan.linkedActions().isEmpty()) {
                prompt.append("  linked actions: ").append(String.join(", ", plan.linkedActions())).append("\n");
            }
            prompt.append("  source refs: ").append(String.join(", ", plan.sourceRefs())).append("\n");
            prompt.append("  sql:\n```sql\n").append(plan.sql()).append("\n```\n");
        }
        prompt.append("""

                【执行约束】
                - 该分支用于风险/预警/异常类查询，优先使用 semantic pack signals，而不是退回固定报表目录或单对象画像。
                - 执行前先对 source refs 调用 schema_lookup 校验字段，SQL 只读，并保留 HAVING 中的预警阈值条件。
                - 输出应包含命中对象、指标值、风险等级、建议动作；linked actions 只作为待确认动作草稿，不直接写业务系统。
                """.trim());
        return prompt.toString().trim();
    }

    private List<String> buildSignalQueryQualityNotes(SignalQuery signalQuery) {
        List<String> notes = new ArrayList<>();
        notes.add("预警查询基于 semantic pack signals 生成，执行前需要 schema_lookup 校验字段和指标口径。");
        notes.add("阈值来自 signals.when 条件，结果仅作为业务预警线索，需要业务核验。");
        boolean hasLinkedActions = signalQuery.plans().stream().anyMatch(plan -> !plan.linkedActions().isEmpty());
        if (hasLinkedActions) {
            notes.add("linked actions 只生成待确认草稿，不直接改变业务状态。");
        }
        return List.copyOf(notes);
    }

    private boolean containsAny(String text, String... patterns) {
        for (String pattern : patterns) {
            if (text.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    private ConversationPlan buildReportCatalogPlan(
            ReportCatalogEntry entry,
            String routedDomain,
            List<String> secondaryTargets,
            String templateCode,
            ExtendedRoutingResult extendedRouting) {
        ResponseKind responseKind = ResponseKind.valueOf(entry.responseKind());
        String domain = ResponseKind.FIXED_REPORT == responseKind || !StringUtils.hasText(routedDomain)
                ? entry.domain()
                : routedDomain;
        String resolvedTemplateCode = resolveReportCatalogTemplateCode(entry, templateCode);
        String promptContext = buildReportCatalogPrompt(entry, domain, secondaryTargets, extendedRouting, resolvedTemplateCode);
        PlanMode planMode = ResponseKind.FIXED_REPORT == responseKind
                ? PlanMode.TEMPLATE_FAST_PATH
                : PlanMode.AGENT_WORKFLOW;
        return new ConversationPlan(
                planMode,
                responseKind,
                null,
                domain,
                entry.primaryTarget(),
                secondaryTargets,
                resolvedTemplateCode,
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

    private String resolveReportCatalogTemplateCode(ReportCatalogEntry entry, String templateCode) {
        if (StringUtils.hasText(templateCode)) {
            return templateCode;
        }
        if (entry == null || entry.sourceRefs() == null) {
            return null;
        }
        for (String sourceRef : entry.sourceRefs()) {
            if (StringUtils.hasText(sourceRef) && sourceRef.startsWith("fixed-report:")) {
                String value = sourceRef.substring("fixed-report:".length()).trim();
                if (StringUtils.hasText(value)) {
                    return value;
                }
            }
        }
        return null;
    }

    private String resolveRouteTarget(ReportCatalogEntry entry) {
        String templateCode = resolveReportCatalogTemplateCode(entry, null);
        if (StringUtils.hasText(templateCode)) {
            return templateCode;
        }
        return entry == null ? null : entry.reportCode();
    }

    private List<ConversationPlan.RouteStep> buildReportCatalogRouteTrace(
            RouteTrace routeTrace,
            ReportCatalogEntry entry) {
        ResponseKind responseKind = ResponseKind.valueOf(entry.responseKind());
        if (ResponseKind.FIXED_REPORT == responseKind) {
            return routeTrace.hit(
                    RouteTier.TIER_2_MART_TEMPLATE,
                    "命中固定报表/资产目录",
                    resolveRouteTarget(entry));
        }
        routeTrace.missIfAbsent(
                RouteTier.TIER_2_MART_TEMPLATE,
                "未命中需要优先返回的可执行模板或固定报表资产");
        routeTrace.missIfAbsent(
                RouteTier.TIER_3_ONTOLOGY_OBJECT_GRAPH,
                "未命中本体对象链路或 semantic pack signal");
        if (ResponseKind.BUSINESS_DETAIL == responseKind
                || ResponseKind.BUSINESS_INSIGHT == responseKind) {
            routeTrace.missIfAbsent(
                    RouteTier.TIER_4_GUARDRAIL_FEDERATED,
                    "未进入泛化联邦分析，优先使用目录中的业务只读入口");
            return routeTrace.hit(
                    RouteTier.TIER_5_DIRECT_DETAIL,
                    "命中业务只读目录入口",
                    entry.primaryTarget());
        }
        return routeTrace.hit(
                RouteTier.TIER_4_GUARDRAIL_FEDERATED,
                ResponseKind.ACTION_PROPOSAL == responseKind ? "命中受控动作提案" : "命中现生成报表草稿目录",
                entry.primaryTarget());
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
                - 当前自动预览走 Trino 联邦查询入口，只允许 catalog `postgres` 和 `mysql`；MySQL 业务库表必须写成 `mysql.rs_cloud_flower.<table>`。
                - 不要使用 `PRODUCTION`、`FLOWER_BIZ`、`PRS_*` 这类未授权 catalog/schema/table，也不要使用未确认的 Snowflake 函数。
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
        if (ResponseKind.FIXED_REPORT.name().equals(entry.responseKind())) {
            prompt.append("""

                    【固定报表】
                    - 该问题已命中 L2 固定报表资产，优先返回既有报表/大屏入口和口径说明。
                    - 不要重新生成一份临时报表草稿；如用户继续要求调整，再进入报表草稿生成或编辑流程。
                    """.trim());
        } else if (ResponseKind.REPORT_DRAFT.name().equals(entry.responseKind())) {
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

    private interface PlanRoute {
        Optional<ConversationPlan> resolve(RouteEvaluationContext context);
    }

    private record RouteEvaluationContext(
            String userQuestion,
            String metricOverride,
            RouteTrace routeTrace,
            IndicatorMatchResult indicatorMatch,
            boolean skipIndicatorRoute,
            TemplateMatchResult templateMatch,
            ExtendedRoutingResult extendedRouting,
            RoutingResult routing,
            String domain,
            String primaryTarget,
            List<String> secondaryTargets,
            String templateCode,
            Optional<ReportCatalogEntry> reportCatalogMatch,
            Optional<BusinessObjectEntry> businessObjectMatch,
            Optional<CatalogEntry> catalogMatch) {
        private RouteEvaluationContext {
            secondaryTargets = secondaryTargets == null ? List.of() : List.copyOf(secondaryTargets);
            reportCatalogMatch = reportCatalogMatch == null ? Optional.empty() : reportCatalogMatch;
            businessObjectMatch = businessObjectMatch == null ? Optional.empty() : businessObjectMatch;
            catalogMatch = catalogMatch == null ? Optional.empty() : catalogMatch;
        }
    }

    private record ObjectMention(String objectName, int index) {
    }

    private record OntologyNavigation(
            String domain,
            List<String> objectNames,
            OntologyService.JoinPlan joinPlan,
            int candidateCount) {
    }

    private record SignalQuery(
            String domain,
            List<OntologyService.SignalPlan> plans) {
        private SignalQuery {
            plans = plans == null ? List.of() : List.copyOf(plans);
        }
    }

    private static final class RouteTrace {
        private final List<ConversationPlan.RouteStep> steps = new ArrayList<>();

        void miss(RouteTier tier, String reason) {
            steps.add(new ConversationPlan.RouteStep(tier.name(), tier.label(), "MISS", reason, null));
        }

        void missIfAbsent(RouteTier tier, String reason) {
            boolean alreadyRecorded = steps.stream().anyMatch(step -> tier.name().equals(step.tier()));
            if (!alreadyRecorded) {
                miss(tier, reason);
            }
        }

        List<ConversationPlan.RouteStep> hit(RouteTier tier, String reason, String target) {
            steps.add(new ConversationPlan.RouteStep(tier.name(), tier.label(), "HIT", reason, target));
            return List.copyOf(steps);
        }
    }
}
