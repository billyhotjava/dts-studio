package com.yuzhi.dts.copilot.ai.service.copilot;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class FinanceAnswerAuditTrailService {

    private static final BigDecimal ZERO_CENTS = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final Pattern SENSITIVE_COMMENT_PATTERN = Pattern.compile(
            "/\\*[^*]*(?:\\*(?!/)[^*]*)*\\*/|--[^\\n]*",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern JDBC_PATTERN = Pattern.compile("(?i)jdbc:[^\\s,;)]*");
    private static final Pattern SECRET_ASSIGNMENT_PATTERN = Pattern.compile(
            "(?i)\\b(password|passwd|token|secret|authorization|access_key)\\s*=\\s*('([^']*)'|\"([^\"]*)\"|[^\\s,;)]*)");

    private final CaliberRuleRegistry caliberRuleRegistry;
    private final FinanceInvariantRegistry financeInvariantRegistry;
    private final FinanceOracleRegistry financeOracleRegistry;

    public FinanceAnswerAuditTrailService(
            CaliberRuleRegistry caliberRuleRegistry,
            FinanceInvariantRegistry financeInvariantRegistry,
            FinanceOracleRegistry financeOracleRegistry) {
        this.caliberRuleRegistry = caliberRuleRegistry;
        this.financeInvariantRegistry = financeInvariantRegistry;
        this.financeOracleRegistry = financeOracleRegistry;
    }

    public AuditTrailReport buildAuditTrail(
            FinanceAnswerAuditTrailRegistry.AuditTrailPolicy policy,
            FinanceAnswerAuditTrailRegistry.AuditTrailBindingPolicy bindingPolicy,
            AuditTrailRequest request) {
        FinanceAnswerAuditTrailRegistry.AuditTrailPolicy safePolicy = policy == null
                ? new FinanceAnswerAuditTrailRegistry.AuditTrailPolicy("", List.of(), List.of(), "")
                : policy;
        FinanceAnswerAuditTrailRegistry.AuditTrailBindingPolicy safeBindingPolicy = bindingPolicy == null
                ? new FinanceAnswerAuditTrailRegistry.AuditTrailBindingPolicy("", "", List.of(), List.of(), List.of(), List.of())
                : bindingPolicy;
        AuditTrailRequest safeRequest = request == null
                ? new AuditTrailRequest("", "", "", "", List.of(), null)
                : request;

        String sanitizedSql = sanitizeSql(safeRequest.generatedSql());
        List<AppliedCaliberRule> appliedRules = appliedRules(safeBindingPolicy.caliberRuleIds());
        List<AppliedInvariant> appliedInvariants = appliedInvariants(safeBindingPolicy.invariantIds());
        Optional<FinanceOracleRegistry.OracleBinding> oracleBinding = financeOracleRegistry.binding(safeBindingPolicy.oracleBindingId());
        List<LineageNode> lineage = lineage(safeRequest.answerId(), safeBindingPolicy, oracleBinding);
        OracleAuditStatus oracleStatus = oracleStatus(oracleBinding, safeRequest.scorecardReport());
        List<RouteTraceStep> routeTrace = safeRequest.routeTrace();
        String failureMessage = firstContractFailure(safePolicy, safeBindingPolicy, safeRequest);
        if (failureMessage.isEmpty()) {
            failureMessage = firstFailure(safePolicy, sanitizedSql, appliedRules, appliedInvariants, lineage, oracleStatus, routeTrace);
        }

        return new AuditTrailReport(
                failureMessage.isEmpty(),
                failureMessage,
                safePolicy.requiredSections(),
                sanitizedSql,
                appliedRules,
                appliedInvariants,
                lineage,
                oracleStatus,
                routeTrace);
    }

    private static String sanitizeSql(String sql) {
        if (sql == null || sql.isBlank()) {
            return "";
        }
        String withoutSensitiveComments = SENSITIVE_COMMENT_PATTERN.matcher(sql)
                .replaceAll(match -> containsSensitiveMarker(match.group()) ? " " : match.group());
        String withoutJdbc = JDBC_PATTERN.matcher(withoutSensitiveComments).replaceAll("[redacted-jdbc-url]");
        return SECRET_ASSIGNMENT_PATTERN.matcher(withoutJdbc)
                .replaceAll("$1=[redacted]")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static boolean containsSensitiveMarker(String value) {
        String lower = value == null ? "" : value.toLowerCase(java.util.Locale.ROOT);
        return lower.contains("jdbc:")
                || lower.contains("password")
                || lower.contains("passwd")
                || lower.contains("token")
                || lower.contains("secret")
                || lower.contains("authorization")
                || lower.contains("access_key");
    }

    private List<AppliedCaliberRule> appliedRules(List<String> ruleIds) {
        Map<String, CaliberRuleRegistry.CaliberRule> byId = new LinkedHashMap<>();
        for (CaliberRuleRegistry.CaliberRule rule : caliberRuleRegistry.rules()) {
            byId.put(rule.id(), rule);
        }
        List<AppliedCaliberRule> appliedRules = new ArrayList<>();
        for (String ruleId : ruleIds) {
            CaliberRuleRegistry.CaliberRule rule = byId.get(ruleId);
            if (rule != null) {
                appliedRules.add(new AppliedCaliberRule(
                        rule.id(),
                        rule.description(),
                        rule.severity(),
                        rule.guardrailText(),
                        rule.appliesTo()));
            }
        }
        return List.copyOf(appliedRules);
    }

    private List<AppliedInvariant> appliedInvariants(List<String> invariantIds) {
        Map<String, FinanceInvariantRegistry.FinanceInvariant> byId = new LinkedHashMap<>();
        for (FinanceInvariantRegistry.FinanceInvariant invariant : financeInvariantRegistry.invariants()) {
            byId.put(invariant.id(), invariant);
        }
        List<AppliedInvariant> appliedInvariants = new ArrayList<>();
        for (String invariantId : invariantIds) {
            FinanceInvariantRegistry.FinanceInvariant invariant = byId.get(invariantId);
            if (invariant != null) {
                appliedInvariants.add(new AppliedInvariant(
                        invariant.id(),
                        invariant.statement(),
                        invariant.severity(),
                        invariant.sourceRuleIds(),
                        invariant.sourceRefs()));
            }
        }
        return List.copyOf(appliedInvariants);
    }

    private static List<LineageNode> lineage(
            String answerId,
            FinanceAnswerAuditTrailRegistry.AuditTrailBindingPolicy bindingPolicy,
            Optional<FinanceOracleRegistry.OracleBinding> oracleBinding) {
        List<LineageNode> nodes = new ArrayList<>();
        if (!answerId.isBlank()) {
            nodes.add(new LineageNode("RESULT", answerId, "finance-answer", List.of()));
        }
        for (String adsModel : bindingPolicy.adsModels()) {
            nodes.add(new LineageNode("ADS_MODEL", adsModel, "auditable-result-model", bindingPolicy.lineageRefs()));
        }
        oracleBinding.ifPresent(binding -> {
            for (String sourceTable : binding.sourceTables()) {
                nodes.add(new LineageNode("SOURCE_TABLE", sourceTable, "adminapi-source", binding.adminWebEvidence()));
            }
            for (FinanceOracleRegistry.OracleEndpoint endpoint : binding.endpoints()) {
                nodes.add(new LineageNode("ORACLE_ENDPOINT", endpoint.signature(), binding.oracleLevel(), binding.adminWebEvidence()));
            }
            if (!textOrEmpty(binding.ledger().voucherTable()).isBlank()) {
                nodes.add(new LineageNode("VOUCHER_LEDGER", binding.ledger().voucherTable(), "voucher-header", binding.adminWebEvidence()));
            }
            if (!textOrEmpty(binding.ledger().itemTable()).isBlank()) {
                nodes.add(new LineageNode("VOUCHER_LEDGER", binding.ledger().itemTable(), "voucher-item", binding.adminWebEvidence()));
            }
        });
        return List.copyOf(nodes);
    }

    private static OracleAuditStatus oracleStatus(
            Optional<FinanceOracleRegistry.OracleBinding> oracleBinding,
            FinanceReconciliationScorecardService.ScorecardReport scorecardReport) {
        if (oracleBinding.isEmpty()) {
            return new OracleAuditStatus("", "", "", "", false, "MISSING_ORACLE", ZERO_CENTS, "oracle binding is missing");
        }
        FinanceOracleRegistry.OracleBinding binding = oracleBinding.get();
        if (scorecardReport == null) {
            return new OracleAuditStatus(
                    binding.id(),
                    binding.reportName(),
                    binding.oracleLevel(),
                    binding.chain(),
                    false,
                    "MISSING_SCORECARD",
                    ZERO_CENTS,
                    "reconciliation scorecard is missing");
        }
        return new OracleAuditStatus(
                binding.id(),
                binding.reportName(),
                binding.oracleLevel(),
                binding.chain(),
                scorecardReport.passed(),
                scorecardReport.healthStatus(),
                scorecardReport.maxDifference(),
                scorecardReport.failureMessage());
    }

    private static String firstContractFailure(
            FinanceAnswerAuditTrailRegistry.AuditTrailPolicy policy,
            FinanceAnswerAuditTrailRegistry.AuditTrailBindingPolicy bindingPolicy,
            AuditTrailRequest request) {
        if (!domainAllowed(policy.domains(), request.domain())) {
            return "Finance answer audit trail failed: reason=domain not covered"
                    + ", policyId=" + policy.id()
                    + ", domain=" + request.domain();
        }
        if (!request.oracleBindingId().isBlank()
                && !request.oracleBindingId().equals(bindingPolicy.oracleBindingId())) {
            return "Finance answer audit trail failed: reason=oracle binding mismatch"
                    + ", expected=" + bindingPolicy.oracleBindingId()
                    + ", actual=" + request.oracleBindingId();
        }
        return "";
    }

    private static boolean domainAllowed(List<String> policyDomains, String requestDomain) {
        if (policyDomains.isEmpty()) {
            return true;
        }
        String normalizedRequestDomain = normalize(requestDomain);
        return policyDomains.stream()
                .map(FinanceAnswerAuditTrailService::normalize)
                .anyMatch(normalizedRequestDomain::equals);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static String firstFailure(
            FinanceAnswerAuditTrailRegistry.AuditTrailPolicy policy,
            String sanitizedSql,
            List<AppliedCaliberRule> appliedRules,
            List<AppliedInvariant> appliedInvariants,
            List<LineageNode> lineage,
            OracleAuditStatus oracleStatus,
            List<RouteTraceStep> routeTrace) {
        for (String section : policy.requiredSections()) {
            String normalized = section == null ? "" : section.trim();
            switch (normalized) {
                case "sql" -> {
                    if (sanitizedSql.isBlank()) {
                        return "Finance answer audit trail failed: reason=missing sanitized sql";
                    }
                }
                case "caliberRules" -> {
                    if (appliedRules.isEmpty() && appliedInvariants.isEmpty()) {
                        return "Finance answer audit trail failed: reason=missing caliber rules";
                    }
                }
                case "lineage" -> {
                    if (lineage.isEmpty()) {
                        return "Finance answer audit trail failed: reason=missing lineage";
                    }
                }
                case "oracleStatus" -> {
                    if (!oracleStatus.covered()) {
                        return "Finance answer audit trail failed: reason=missing oracle reconciliation status"
                                + ", oracleBindingId=" + oracleStatus.bindingId()
                                + ", status=" + oracleStatus.healthStatus();
                    }
                }
                case "routeTrace" -> {
                    if (routeTrace.isEmpty()) {
                        return "Finance answer audit trail failed: reason=missing route trace";
                    }
                }
                default -> {
                    return "Finance answer audit trail failed: reason=unknown required section, section=" + normalized;
                }
            }
        }
        return "";
    }

    private static BigDecimal cents(BigDecimal amount) {
        return amount == null ? ZERO_CENTS : amount.setScale(2, RoundingMode.HALF_UP);
    }

    private static String textOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private static <T> List<T> copyOrEmpty(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    public record AuditTrailRequest(
            String answerId,
            String domain,
            String generatedSql,
            String oracleBindingId,
            List<RouteTraceStep> routeTrace,
            FinanceReconciliationScorecardService.ScorecardReport scorecardReport) {
        public AuditTrailRequest {
            answerId = textOrEmpty(answerId);
            domain = textOrEmpty(domain);
            generatedSql = textOrEmpty(generatedSql);
            oracleBindingId = textOrEmpty(oracleBindingId);
            routeTrace = copyOrEmpty(routeTrace);
        }
    }

    public record RouteTraceStep(
            String tier,
            String label,
            String status,
            String reason,
            String target) {
        public RouteTraceStep {
            tier = textOrEmpty(tier);
            label = textOrEmpty(label);
            status = textOrEmpty(status);
            reason = textOrEmpty(reason);
            target = textOrEmpty(target);
        }
    }

    public record AppliedCaliberRule(
            String ruleId,
            String description,
            String severity,
            String guardrailText,
            List<String> appliesTo) {
        public AppliedCaliberRule {
            ruleId = textOrEmpty(ruleId);
            description = textOrEmpty(description);
            severity = textOrEmpty(severity);
            guardrailText = textOrEmpty(guardrailText);
            appliesTo = copyOrEmpty(appliesTo);
        }
    }

    public record AppliedInvariant(
            String invariantId,
            String statement,
            String severity,
            List<String> sourceRuleIds,
            List<String> sourceRefs) {
        public AppliedInvariant {
            invariantId = textOrEmpty(invariantId);
            statement = textOrEmpty(statement);
            severity = textOrEmpty(severity);
            sourceRuleIds = copyOrEmpty(sourceRuleIds);
            sourceRefs = copyOrEmpty(sourceRefs);
        }
    }

    public record LineageNode(
            String level,
            String name,
            String role,
            List<String> refs) {
        public LineageNode {
            level = textOrEmpty(level);
            name = textOrEmpty(name);
            role = textOrEmpty(role);
            refs = copyOrEmpty(refs);
        }
    }

    public record OracleAuditStatus(
            String bindingId,
            String reportName,
            String oracleLevel,
            String chain,
            boolean covered,
            String healthStatus,
            BigDecimal maxDifference,
            String failureMessage) {
        public OracleAuditStatus {
            bindingId = textOrEmpty(bindingId);
            reportName = textOrEmpty(reportName);
            oracleLevel = textOrEmpty(oracleLevel);
            chain = textOrEmpty(chain);
            healthStatus = textOrEmpty(healthStatus);
            maxDifference = cents(maxDifference);
            failureMessage = textOrEmpty(failureMessage);
        }
    }

    public record AuditTrailReport(
            boolean passed,
            String failureMessage,
            List<String> sections,
            String sanitizedSql,
            List<AppliedCaliberRule> appliedRules,
            List<AppliedInvariant> appliedInvariants,
            List<LineageNode> lineage,
            OracleAuditStatus oracleStatus,
            List<RouteTraceStep> routeTrace) {
        public AuditTrailReport {
            failureMessage = textOrEmpty(failureMessage);
            sections = copyOrEmpty(sections);
            sanitizedSql = textOrEmpty(sanitizedSql);
            appliedRules = copyOrEmpty(appliedRules);
            appliedInvariants = copyOrEmpty(appliedInvariants);
            lineage = copyOrEmpty(lineage);
            oracleStatus = oracleStatus == null
                    ? new OracleAuditStatus("", "", "", "", false, "MISSING_ORACLE", ZERO_CENTS, "")
                    : oracleStatus;
            routeTrace = copyOrEmpty(routeTrace);
        }
    }
}
