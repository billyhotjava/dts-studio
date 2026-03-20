package com.yuzhi.dts.copilot.ai.service.copilot;

import com.yuzhi.dts.copilot.ai.service.copilot.IntentRouterService.ExtendedRoutingResult;
import com.yuzhi.dts.copilot.ai.service.copilot.IntentRouterService.RoutingResult;
import com.yuzhi.dts.copilot.ai.service.copilot.TemplateMatcherService.TemplateMatchResult;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ConversationPlannerService {

    private static final Set<String> FRIENDLY_GUIDANCE_INPUTS = Set.of(
            "hi", "hello", "hey", "hey there", "help",
            "你好", "您好", "嗨", "哈喽", "在吗", "在不在", "帮忙", "帮助");

    private static final Set<String> ASSISTANT_META_INPUTS = Set.of(
            "你是谁", "你能做什么", "你可以做什么", "你是什么模型",
            "what can you do", "who are you", "what model are you");

    private static final Set<String> METADATA_EXPLORATION_KEYWORDS = Set.of(
            "所有表", "全部表", "有哪些表", "什么表", "表结构", "字段", "schema", "元数据", "数据表", "表名");

    private final IntentRouterService intentRouterService;
    private final TemplateMatcherService templateMatcherService;
    private final SemanticPackService semanticPackService;

    public ConversationPlannerService(
            IntentRouterService intentRouterService,
            TemplateMatcherService templateMatcherService,
            SemanticPackService semanticPackService) {
        this.intentRouterService = intentRouterService;
        this.templateMatcherService = templateMatcherService;
        this.semanticPackService = semanticPackService;
    }

    public ConversationPlan plan(String userQuestion) {
        return plan(userQuestion, Collections.emptyMap());
    }

    public ConversationPlan plan(String userQuestion, Map<String, Boolean> martHealthSnapshot) {
        if (isFriendlyGuidanceInput(userQuestion)) {
            return new ConversationPlan(
                    PlanMode.DIRECT_RESPONSE,
                    ResponseKind.GREETING_GUIDANCE,
                    buildFriendlyGuidanceMessage(),
                    null,
                    null,
                    List.of(),
                    null,
                    null,
                    "VIEW",
                    null,
                    "");
        }
        if (isAssistantMetaInput(userQuestion)) {
            return new ConversationPlan(
                    PlanMode.DIRECT_RESPONSE,
                    ResponseKind.ASSISTANT_META,
                    buildAssistantMetaMessage(),
                    null,
                    null,
                    List.of(),
                    null,
                    null,
                    "VIEW",
                    null,
                    "");
        }
        if (isMetadataExplorationInput(userQuestion)) {
            return new ConversationPlan(
                    PlanMode.AGENT_WORKFLOW,
                    ResponseKind.SCHEMA_EXPLORATION,
                    null,
                    null,
                    null,
                    List.of(),
                    null,
                    null,
                    "VIEW",
                    null,
                    buildMetadataExplorationPrompt());
        }

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

    private boolean isFriendlyGuidanceInput(String userQuestion) {
        if (!StringUtils.hasText(userQuestion)) {
            return false;
        }
        String normalized = userQuestion.trim().toLowerCase(Locale.ROOT);
        if (FRIENDLY_GUIDANCE_INPUTS.contains(normalized)) {
            return true;
        }
        return normalized.matches("^(hi|hello|hey)(\\s+there)?[!?.]*$")
                || normalized.matches("^(你好|您好|嗨|哈喽|在吗|在不在)[！!。.?？]*$");
    }

    private boolean isAssistantMetaInput(String userQuestion) {
        if (!StringUtils.hasText(userQuestion)) {
            return false;
        }
        String normalized = userQuestion.trim().toLowerCase(Locale.ROOT);
        if (ASSISTANT_META_INPUTS.contains(normalized)) {
            return true;
        }
        return normalized.contains("你是什么模型")
                || normalized.contains("我想问下你是什么模型")
                || normalized.contains("你是谁")
                || normalized.contains("你能做什么")
                || normalized.contains("你可以做什么")
                || normalized.contains("what model are you")
                || normalized.contains("who are you")
                || normalized.contains("what can you do");
    }

    private boolean isMetadataExplorationInput(String userQuestion) {
        if (!StringUtils.hasText(userQuestion)) {
            return false;
        }
        String normalized = userQuestion.trim().toLowerCase(Locale.ROOT);
        return METADATA_EXPLORATION_KEYWORDS.stream().anyMatch(normalized::contains)
                || normalized.matches(".*(查询|查看|列出|展示).*(表|字段|schema).*")
                || normalized.matches(".*(数据库|数据源|库).*(有哪些|所有|全部).*(表|字段).*");
    }

    private String buildFriendlyGuidanceMessage() {
        return """
                你好，我可以帮你查询园林项目的业务数据。
                你可以直接问具体问题，比如：
                1. 本月加花最多的项目是哪个？
                2. 哪些项目的养护任务还没完成？
                3. 当前在服项目一共有多少个？
                """.trim();
    }

    private String buildAssistantMetaMessage() {
        return """
                我是 DTS Copilot，当前接入的是系统已配置的大模型服务。
                我主要帮你处理园林业务数据问题，比如项目履约、报花业务、任务执行、养护和结算分析。
                你可以继续直接问业务问题，例如：
                1. 当前在服项目一共有多少个？
                2. 本月加花最多的项目是哪个？
                3. 哪些项目的养护任务还没完成？
                """.trim();
    }

    private String buildMetadataExplorationPrompt() {
        return """
                【元数据探索】
                - 当前问题是在查看数据源的表结构或字段信息，不要回到业务范围澄清。
                - 优先调用 schema_lookup 工具。
                - 如果用户在问“所有表/有哪些表”，调用 schema_lookup 且不要传 table_name。
                - 如果用户指定了某张表，再调用 schema_lookup 查看字段明细。
                - 先返回真实表名或字段信息，再补简短说明。
                """.trim();
    }

    public enum PlanMode {
        DIRECT_RESPONSE,
        TEMPLATE_FAST_PATH,
        AGENT_WORKFLOW
    }

    public enum ResponseKind {
        GREETING_GUIDANCE,
        ASSISTANT_META,
        SCHEMA_EXPLORATION,
        BUSINESS_ANALYSIS,
        BUSINESS_CLARIFICATION,
        GENERIC_ANALYSIS,
        TEMPLATE_SQL
    }

    public record ConversationPlan(
            PlanMode mode,
            ResponseKind responseKind,
            String directResponse,
            String routedDomain,
            String primaryTarget,
            List<String> secondaryTargets,
            String templateCode,
            String resolvedSql,
            String dataLayer,
            String martTable,
            String promptContext
    ) {}
}
