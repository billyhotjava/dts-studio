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

    public enum PlanMode {
        DIRECT_RESPONSE,
        TEMPLATE_FAST_PATH,
        AGENT_WORKFLOW
    }

    public enum ResponseKind {
        BUSINESS_DIRECT_RESPONSE,
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
