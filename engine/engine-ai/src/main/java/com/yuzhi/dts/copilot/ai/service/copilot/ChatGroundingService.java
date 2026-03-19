package com.yuzhi.dts.copilot.ai.service.copilot;

import com.yuzhi.dts.copilot.ai.service.copilot.IntentRouterService.RoutingResult;
import com.yuzhi.dts.copilot.ai.service.copilot.TemplateMatcherService.TemplateMatchResult;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Compiles sprint-10 business grounding assets into a compact prompt block
 * that can be injected into the shipped chat runtime.
 */
@Service
public class ChatGroundingService {

    private static final Set<String> FRIENDLY_GUIDANCE_INPUTS = Set.of(
            "hi", "hello", "hey", "hey there", "help",
            "你好", "您好", "嗨", "哈喽", "在吗", "在不在", "帮忙", "帮助");

    private final IntentRouterService intentRouterService;
    private final TemplateMatcherService templateMatcherService;
    private final SemanticPackService semanticPackService;

    public ChatGroundingService(
            IntentRouterService intentRouterService,
            TemplateMatcherService templateMatcherService,
            SemanticPackService semanticPackService) {
        this.intentRouterService = intentRouterService;
        this.templateMatcherService = templateMatcherService;
        this.semanticPackService = semanticPackService;
    }

    public GroundingContext buildContext(String userQuestion) {
        if (isFriendlyGuidanceInput(userQuestion)) {
            return new GroundingContext(
                    true,
                    buildFriendlyGuidanceMessage(),
                    null,
                    null,
                    List.of(),
                    null,
                    null,
                    ""
            );
        }

        TemplateMatchResult templateMatch = templateMatcherService.match(userQuestion);
        RoutingResult routing = intentRouterService.route(userQuestion);

        if ((routing == null || routing.needsClarification()) && !templateMatch.matched()) {
            return new GroundingContext(
                    true,
                    intentRouterService.generateClarificationMessage(),
                    null,
                    null,
                    List.of(),
                    null,
                    null,
                    "");
        }

        String domain = resolveDomain(routing, templateMatch);
        String primaryView = resolvePrimaryView(routing, templateMatch);
        List<String> secondaryViews = routing != null && routing.secondaryViews() != null
                ? routing.secondaryViews()
                : List.of();
        String templateCode = templateMatch.matched() && templateMatch.template() != null
                ? templateMatch.template().getTemplateCode()
                : null;

        StringBuilder prompt = new StringBuilder();
        prompt.append("【业务路由】\n");
        if (StringUtils.hasText(domain)) {
            prompt.append("- routed domain: ").append(domain).append("\n");
        }
        if (StringUtils.hasText(primaryView)) {
            prompt.append("- primary view: ").append(primaryView).append("\n");
        }
        if (!secondaryViews.isEmpty()) {
            prompt.append("- secondary views: ").append(String.join(", ", secondaryViews)).append("\n");
        }

        String semanticDomain = normalizeSemanticDomain(domain);
        String semanticContext = semanticPackService.getContextForDomain(semanticDomain);
        if (StringUtils.hasText(semanticContext)) {
            prompt.append("\n").append(semanticContext.trim()).append("\n");
        }

        if (templateMatch.matched() && StringUtils.hasText(templateMatch.resolvedSql())) {
            prompt.append("\n【预制模板参考】\n");
            if (StringUtils.hasText(templateCode)) {
                prompt.append("- template code: ").append(templateCode).append("\n");
            }
            prompt.append(templateMatch.resolvedSql().trim()).append("\n");
        }

        return new GroundingContext(
                false,
                null,
                domain,
                primaryView,
                secondaryViews,
                templateCode,
                templateMatch.matched() ? templateMatch.resolvedSql() : null,
                prompt.toString().trim());
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

    private String resolvePrimaryView(RoutingResult routing, TemplateMatchResult templateMatch) {
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

    private String buildFriendlyGuidanceMessage() {
        return """
                你好，我可以帮你查询园林项目的业务数据。
                你可以直接问具体问题，比如：
                1. 本月加花最多的项目是哪个？
                2. 哪些项目的养护任务还没完成？
                3. 当前在服项目一共有多少个？
                """.trim();
    }

    public record GroundingContext(
            boolean needsClarification,
            String clarificationMessage,
            String domain,
            String primaryView,
            List<String> secondaryViews,
            String templateCode,
            String resolvedSql,
            String promptContext
    ) {}
}
