package com.yuzhi.dts.copilot.ai.service.copilot;

import com.yuzhi.dts.copilot.ai.service.copilot.IntentRouterService.RoutingResult;
import com.yuzhi.dts.copilot.ai.service.copilot.TemplateMatcherService.TemplateMatchResult;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Compiles sprint-10 business grounding assets into a compact prompt block
 * that can be injected into the shipped chat runtime.
 */
@Service
public class ChatGroundingService {

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
