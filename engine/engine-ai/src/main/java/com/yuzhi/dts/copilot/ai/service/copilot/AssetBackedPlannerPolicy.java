package com.yuzhi.dts.copilot.ai.service.copilot;

import com.yuzhi.dts.copilot.ai.service.copilot.BusinessDirectResponseCatalogService.CatalogEntry;
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
            "报表", "汇总", "明细", "列表", "清单", "排行", "排名", "看板", "台账", "统计"
    );

    private final IntentRouterService intentRouterService;
    private final TemplateMatcherService templateMatcherService;
    private final SemanticPackService semanticPackService;
    private final BusinessDirectResponseCatalogService directResponseCatalogService;

    public AssetBackedPlannerPolicy(IntentRouterService intentRouterService,
                                    TemplateMatcherService templateMatcherService,
                                    SemanticPackService semanticPackService,
                                    BusinessDirectResponseCatalogService directResponseCatalogService) {
        this.intentRouterService = intentRouterService;
        this.templateMatcherService = templateMatcherService;
        this.semanticPackService = semanticPackService;
        this.directResponseCatalogService = directResponseCatalogService;
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
        }
        if (!StringUtils.hasText(routedDomain)) {
            return null;
        }
        return switch (routedDomain) {
            case "settlement" -> "财务";
            case "procurement", "purchase" -> "采购";
            case "warehouse", "inventory", "stock" -> "仓库";
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
