package com.yuzhi.dts.copilot.ai.service.copilot;

import com.yuzhi.dts.copilot.ai.service.copilot.ConversationPlannerService.ConversationPlan;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FinanceChatAuditTrailService {

    private static final Logger log = LoggerFactory.getLogger(FinanceChatAuditTrailService.class);
    private static final String POLICY_ID = "sprint33-finance-answer-audit-trail";

    private final FinanceAnswerAuditTrailRegistry registry;
    private final FinanceAnswerAuditTrailService auditTrailService;
    private final FinanceReconciliationScorecardSource scorecardSource;

    public FinanceChatAuditTrailService(
            FinanceAnswerAuditTrailRegistry registry,
            FinanceAnswerAuditTrailService auditTrailService) {
        this(registry, auditTrailService, FinanceReconciliationScorecardSource.empty());
    }

    public FinanceChatAuditTrailService(
            FinanceAnswerAuditTrailRegistry registry,
            FinanceAnswerAuditTrailService auditTrailService,
            FinanceReconciliationScorecardSource scorecardSource) {
        this.registry = registry;
        this.auditTrailService = auditTrailService;
        this.scorecardSource = scorecardSource == null
                ? FinanceReconciliationScorecardSource.empty()
                : scorecardSource;
    }

    @Autowired
    public FinanceChatAuditTrailService(
            FinanceAnswerAuditTrailRegistry registry,
            FinanceAnswerAuditTrailService auditTrailService,
            ObjectProvider<FinanceReconciliationScorecardSource> scorecardSourceProvider) {
        this(
                registry,
                auditTrailService,
                scorecardSourceProvider == null
                        ? FinanceReconciliationScorecardSource.empty()
                        : scorecardSourceProvider.getIfAvailable(FinanceReconciliationScorecardSource::empty));
    }

    public Optional<FinanceAnswerAuditTrailService.AuditTrailReport> buildAuditTrail(
            ConversationPlan plan,
            String generatedSql) {
        if (plan == null || !StringUtils.hasText(generatedSql)) {
            return Optional.empty();
        }
        Optional<FinanceAnswerAuditTrailRegistry.AuditTrailBindingPolicy> bindingPolicy = resolveBindingPolicy(plan);
        if (bindingPolicy.isEmpty()) {
            return Optional.empty();
        }
        Optional<FinanceAnswerAuditTrailRegistry.AuditTrailPolicy> policy = registry.policy(POLICY_ID)
                .or(() -> registry.policies().stream().findFirst());
        if (policy.isEmpty()) {
            return Optional.empty();
        }
        FinanceAnswerAuditTrailService.AuditTrailRequest request =
                new FinanceAnswerAuditTrailService.AuditTrailRequest(
                        answerId(plan),
                        plan.routedDomain(),
                        generatedSql,
                        bindingPolicy.get().oracleBindingId(),
                        routeTrace(plan),
                        latestScorecard(bindingPolicy.get().oracleBindingId()).orElse(null));
        return Optional.of(auditTrailService.buildAuditTrail(policy.get(), bindingPolicy.get(), request));
    }

    private Optional<FinanceReconciliationScorecardService.ScorecardReport> latestScorecard(String oracleBindingId) {
        try {
            return scorecardSource.latestScorecard(oracleBindingId);
        } catch (Exception e) {
            log.warn("Failed to load finance scorecard for oracleBindingId={}: {}", oracleBindingId, e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<FinanceAnswerAuditTrailRegistry.AuditTrailBindingPolicy> resolveBindingPolicy(
            ConversationPlan plan) {
        return registry.bindingPolicies().stream()
                .filter(binding -> matches(binding, plan))
                .findFirst();
    }

    private static boolean matches(
            FinanceAnswerAuditTrailRegistry.AuditTrailBindingPolicy binding,
            ConversationPlan plan) {
        String reportCode = text(plan.reportCode());
        if (equalsAny(reportCode, binding.reportCode(), binding.oracleBindingId())) {
            return true;
        }
        if (plan.metricCaliber() != null
                && equalsAny(text(plan.metricCaliber().ontologyRef()), binding.reportCode(), binding.oracleBindingId())) {
            return true;
        }
        String primaryTarget = text(plan.primaryTarget());
        if (binding.adsModels().stream().anyMatch(model -> containsIgnoreCase(primaryTarget, model))) {
            return true;
        }
        return plan.sourceRefs().stream()
                .anyMatch(sourceRef -> binding.adsModels().stream()
                        .anyMatch(model -> containsIgnoreCase(sourceRef, model)));
    }

    private static List<FinanceAnswerAuditTrailService.RouteTraceStep> routeTrace(ConversationPlan plan) {
        return plan.routeTrace().stream()
                .map(step -> new FinanceAnswerAuditTrailService.RouteTraceStep(
                        step.tier(),
                        step.label(),
                        step.status(),
                        step.reason(),
                        step.target()))
                .toList();
    }

    private static String answerId(ConversationPlan plan) {
        if (StringUtils.hasText(plan.reportCode())) {
            return plan.reportCode();
        }
        return text(plan.primaryTarget());
    }

    private static boolean equalsAny(String value, String... candidates) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        for (String candidate : candidates) {
            if (value.equalsIgnoreCase(text(candidate))) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsIgnoreCase(String value, String fragment) {
        return StringUtils.hasText(value)
                && StringUtils.hasText(fragment)
                && value.toLowerCase(java.util.Locale.ROOT).contains(fragment.toLowerCase(java.util.Locale.ROOT));
    }

    private static String text(String value) {
        return value == null ? "" : value.trim();
    }
}
