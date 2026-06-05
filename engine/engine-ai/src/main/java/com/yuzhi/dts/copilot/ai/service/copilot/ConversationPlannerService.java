package com.yuzhi.dts.copilot.ai.service.copilot;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ConversationPlannerService {

    private static final Logger log = LoggerFactory.getLogger(ConversationPlannerService.class);

    private final Map<String, PlannerPolicy> policies;
    private final String plannerMode;

    public ConversationPlannerService(
            List<PlannerPolicy> policies,
            @Value("${copilot.chat.planner.mode:asset}") String plannerMode) {
        this.policies = policies.stream().collect(
                java.util.stream.Collectors.toMap(PlannerPolicy::mode, policy -> policy, (left, right) -> left));
        this.plannerMode = plannerMode;
    }

    public ConversationPlan plan(String userQuestion) {
        return plan(userQuestion, Collections.emptyMap());
    }

    public ConversationPlan plan(String userQuestion, Map<String, Boolean> martHealthSnapshot) {
        PlannerPolicy policy = policies.get(plannerMode);
        if (policy == null) {
            log.warn("Planner policy '{}' not found, fallback to asset", plannerMode);
            policy = policies.get("asset");
        }
        if (policy == null) {
            throw new IllegalStateException("No planner policy is available");
        }
        return policy.plan(userQuestion, martHealthSnapshot == null ? Collections.emptyMap() : martHealthSnapshot);
    }

    public ConversationPlan plan(String userQuestion, CopilotChatRequestContext requestContext) {
        PlannerPolicy policy = policies.get(plannerMode);
        if (policy == null) {
            log.warn("Planner policy '{}' not found, fallback to asset", plannerMode);
            policy = policies.get("asset");
        }
        if (policy == null) {
            throw new IllegalStateException("No planner policy is available");
        }
        return policy.plan(userQuestion, requestContext == null ? CopilotChatRequestContext.empty() : requestContext);
    }

    public enum PlanMode {
        DIRECT_RESPONSE,
        TEMPLATE_FAST_PATH,
        AGENT_WORKFLOW
    }

    public enum ResponseKind {
        BUSINESS_DIRECT_RESPONSE,
        FIXED_REPORT,
        FIXED_REPORT_CANDIDATES,
        SCHEMA_EXPLORATION,
        BUSINESS_ANALYSIS,
        BUSINESS_CLARIFICATION,
        GENERIC_ANALYSIS,
        TEMPLATE_SQL,
        REPORT_DRAFT,
        OBJECT_GRAPH_NAVIGATION,
        RISK_SIGNAL_QUERY,
        BUSINESS_DETAIL,
        BUSINESS_INSIGHT,
        ACTION_PROPOSAL,
        PUBLISHED_INDICATOR
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
            String promptContext,
            String dataSurface,
            String qualityLevel,
            List<String> qualityNotes,
            String suggestedDisplay,
            String reportCode,
            List<String> sourceRefs,
            MetricCaliber metricCaliber,
            List<RouteStep> routeTrace
    ) {
        public record MetricCaliber(
                String name,
                String formula,
                String domain,
                String version,
                String ontologyRef) {
        }

        public record RouteStep(
                String tier,
                String label,
                String status,
                String reason,
                String target) {
        }

        public ConversationPlan(
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
                String promptContext,
                String dataSurface,
                String qualityLevel,
                List<String> qualityNotes,
                String suggestedDisplay,
                String reportCode
        ) {
            this(
                    mode,
                    responseKind,
                    directResponse,
                    routedDomain,
                    primaryTarget,
                    secondaryTargets,
                    templateCode,
                    resolvedSql,
                    dataLayer,
                    martTable,
                    promptContext,
                    dataSurface,
                    qualityLevel,
                    qualityNotes,
                    suggestedDisplay,
                    reportCode,
                    List.of(),
                    null,
                    List.of());
        }

        public ConversationPlan(
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
                String promptContext,
                String dataSurface,
                String qualityLevel,
                List<String> qualityNotes,
                String suggestedDisplay,
                String reportCode,
                List<String> sourceRefs
        ) {
            this(
                    mode,
                    responseKind,
                    directResponse,
                    routedDomain,
                    primaryTarget,
                    secondaryTargets,
                    templateCode,
                    resolvedSql,
                    dataLayer,
                    martTable,
                    promptContext,
                    dataSurface,
                    qualityLevel,
                    qualityNotes,
                    suggestedDisplay,
                    reportCode,
                    sourceRefs,
                    null,
                    List.of());
        }

        public ConversationPlan(
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
                String promptContext,
                String dataSurface,
                String qualityLevel,
                List<String> qualityNotes,
                String suggestedDisplay,
                String reportCode,
                List<String> sourceRefs,
                MetricCaliber metricCaliber
        ) {
            this(
                    mode,
                    responseKind,
                    directResponse,
                    routedDomain,
                    primaryTarget,
                    secondaryTargets,
                    templateCode,
                    resolvedSql,
                    dataLayer,
                    martTable,
                    promptContext,
                    dataSurface,
                    qualityLevel,
                    qualityNotes,
                    suggestedDisplay,
                    reportCode,
                    sourceRefs,
                    metricCaliber,
                    List.of());
        }

        public ConversationPlan(
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
        ) {
            this(
                    mode,
                    responseKind,
                    directResponse,
                    routedDomain,
                    primaryTarget,
                    secondaryTargets,
                    templateCode,
                    resolvedSql,
                    dataLayer,
                    martTable,
                    promptContext,
                    null,
                    null,
                    List.of(),
                    null,
                    null,
                    List.of(),
                    null,
                    List.of());
        }

        public ConversationPlan {
            secondaryTargets = secondaryTargets == null ? List.of() : List.copyOf(secondaryTargets);
            qualityNotes = qualityNotes == null ? List.of() : List.copyOf(qualityNotes);
            sourceRefs = sourceRefs == null ? List.of() : List.copyOf(sourceRefs);
            routeTrace = routeTrace == null ? List.of() : List.copyOf(routeTrace);
        }

        public ConversationPlan withRouteTrace(List<RouteStep> routeTrace) {
            return new ConversationPlan(
                    mode,
                    responseKind,
                    directResponse,
                    routedDomain,
                    primaryTarget,
                    secondaryTargets,
                    templateCode,
                    resolvedSql,
                    dataLayer,
                    martTable,
                    promptContext,
                    dataSurface,
                    qualityLevel,
                    qualityNotes,
                    suggestedDisplay,
                    reportCode,
                    sourceRefs,
                    metricCaliber,
                    routeTrace);
        }
    }
}
