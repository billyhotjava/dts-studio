package com.yuzhi.dts.copilot.ai.service.copilot;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class FinanceSignoffBaselineService {

    private static final BigDecimal ZERO_CENTS = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    public SignoffBaselineReport buildBaseline(
            FinanceSignoffBaselineRegistry.SignoffBaselinePolicy policy,
            FinanceReconciliationScorecardService.ScorecardReport scorecardReport,
            List<EvidenceRecord> evidenceRecords,
            List<SignatureRecord> signatures) {
        FinanceSignoffBaselineRegistry.SignoffBaselinePolicy safePolicy = policy == null
                ? new FinanceSignoffBaselineRegistry.SignoffBaselinePolicy("", "", "", "", List.of(), List.of(), "", "")
                : policy;
        FinanceReconciliationScorecardService.ScorecardReport safeScorecard = scorecardReport == null
                ? new FinanceReconciliationScorecardService.ScorecardReport(false, false, "MISSING_SCORECARD", 0, 0,
                        ZERO_CENTS, ZERO_CENTS, List.of(), List.of(), List.of(), "scorecard report is missing")
                : scorecardReport;
        List<EvidenceRecord> safeEvidenceRecords = evidenceRecords == null ? List.of() : List.copyOf(evidenceRecords);
        List<SignatureRecord> safeSignatures = signatures == null ? List.of() : List.copyOf(signatures);

        List<EvidenceStatus> evidenceStatuses = evidenceStatuses(safePolicy.requiredEvidence(), safeEvidenceRecords);
        List<String> missingEvidence = evidenceStatuses.stream()
                .filter(status -> !"PASS".equals(status.status()))
                .map(EvidenceStatus::evidenceId)
                .toList();
        List<String> missingRoles = missingSignatureRoles(safePolicy.requiredSignatureRoles(), safeSignatures);
        String signoffStatus = signoffStatus(safeScorecard, missingEvidence, missingRoles);
        String failureMessage = failureMessage(signoffStatus, safeScorecard, missingEvidence, missingRoles);
        boolean engineeringReady = safeScorecard.passed() && missingEvidence.isEmpty();
        boolean accepted = engineeringReady && missingRoles.isEmpty();

        SignoffBaselineReport report = new SignoffBaselineReport(
                safePolicy.id(),
                safePolicy.title(),
                safePolicy.accountPeriod(),
                safePolicy.scorecardPolicyId(),
                engineeringReady,
                accepted,
                signoffStatus,
                failureMessage,
                safeScorecard.healthStatus(),
                safeScorecard.passRate(),
                safeScorecard.maxDifference(),
                evidenceStatuses,
                safePolicy.requiredSignatureRoles(),
                safeSignatures,
                "");
        return report.withBaselineMarkdown(markdown(report));
    }

    public List<FinanceReconciliationScorecardService.ReconciliationFailure> acceptedBaselineFailures(
            SignoffBaselineReport report,
            List<FinanceReconciliationScorecardService.ReconciliationFailure> candidateFailures) {
        if (report == null || !report.accepted() || !"SIGNED".equals(report.signoffStatus())) {
            return List.of();
        }
        return candidateFailures == null ? List.of() : List.copyOf(candidateFailures);
    }

    private static List<EvidenceStatus> evidenceStatuses(
            List<FinanceSignoffBaselineRegistry.RequiredEvidence> requiredEvidence,
            List<EvidenceRecord> evidenceRecords) {
        Map<String, EvidenceRecord> recordsById = new LinkedHashMap<>();
        for (EvidenceRecord record : evidenceRecords) {
            recordsById.put(record.evidenceId(), record);
        }
        List<EvidenceStatus> statuses = new ArrayList<>();
        for (FinanceSignoffBaselineRegistry.RequiredEvidence required : requiredEvidence) {
            EvidenceRecord record = recordsById.get(required.id());
            if (record == null) {
                statuses.add(new EvidenceStatus(
                        required.id(),
                        required.feature(),
                        required.label(),
                        required.command(),
                        required.evidencePath(),
                        "MISSING"));
                continue;
            }
            statuses.add(new EvidenceStatus(
                    required.id(),
                    required.feature(),
                    required.label(),
                    firstPresent(record.command(), required.command()),
                    firstPresent(record.evidencePath(), required.evidencePath()),
                    normalizeStatus(record.status())));
        }
        return List.copyOf(statuses);
    }

    private static List<String> missingSignatureRoles(
            List<FinanceSignoffBaselineRegistry.RequiredSignatureRole> requiredRoles,
            List<SignatureRecord> signatures) {
        Set<String> signedRoles = new LinkedHashSet<>();
        for (SignatureRecord signature : signatures) {
            if (!signature.signedAt().isBlank()) {
                signedRoles.add(signature.role());
            }
        }
        return requiredRoles.stream()
                .map(FinanceSignoffBaselineRegistry.RequiredSignatureRole::role)
                .filter(role -> !signedRoles.contains(role))
                .toList();
    }

    private static String signoffStatus(
            FinanceReconciliationScorecardService.ScorecardReport scorecard,
            List<String> missingEvidence,
            List<String> missingRoles) {
        if (!scorecard.passed()) {
            return "SCORECARD_FAILED";
        }
        if (!missingEvidence.isEmpty()) {
            return "EVIDENCE_INCOMPLETE";
        }
        if (!missingRoles.isEmpty()) {
            return "PENDING_SIGNATURE";
        }
        return "SIGNED";
    }

    private static String failureMessage(
            String signoffStatus,
            FinanceReconciliationScorecardService.ScorecardReport scorecard,
            List<String> missingEvidence,
            List<String> missingRoles) {
        return switch (signoffStatus) {
            case "SCORECARD_FAILED" -> "Finance signoff baseline failed: scorecard="
                    + scorecard.healthStatus()
                    + ", reason=" + scorecard.failureMessage();
            case "EVIDENCE_INCOMPLETE" -> "Finance signoff baseline failed: missing evidence="
                    + String.join(",", missingEvidence);
            case "PENDING_SIGNATURE" -> "Finance signoff baseline pending: missing required signature="
                    + String.join(",", missingRoles);
            default -> "";
        };
    }

    private static String markdown(SignoffBaselineReport report) {
        StringBuilder markdown = new StringBuilder();
        markdown.append("# ").append(firstPresent(report.title(), "Sprint-33 财务签字基线")).append("\n\n");
        markdown.append("**账期**: ").append(report.accountPeriod()).append("\n");
        markdown.append("**Scorecard**: ").append(report.scorecardPolicyId())
                .append(" / ").append(report.scorecardHealthStatus())
                .append(" / passRate=").append(amountText(report.scorecardPassRate()))
                .append(" / maxDifference=").append(amountText(report.scorecardMaxDifference()))
                .append("\n");
        markdown.append("**签字状态**: ").append(report.signoffStatus()).append("\n");
        markdown.append("**漂移基线采信**: ").append(baselineAdoptionText(report)).append("\n\n");
        markdown.append("## IT 证据\n\n");
        markdown.append("| Feature | 证据 | 状态 | 重跑命令 | 日志 |\n");
        markdown.append("|---------|------|------|----------|------|\n");
        for (EvidenceStatus evidence : report.requiredEvidence()) {
            markdown.append("| ")
                    .append(evidence.feature()).append(" | ")
                    .append(evidence.label()).append(" | ")
                    .append(evidence.status()).append(" | `")
                    .append(evidence.command()).append("` | `")
                    .append(evidence.evidencePath()).append("` |\n");
        }
        markdown.append("\n## 签字\n\n");
        markdown.append("| 角色 | 名称 | 时间 | 备注 |\n");
        markdown.append("|------|------|------|------|\n");
        Map<String, SignatureRecord> signaturesByRole = new LinkedHashMap<>();
        for (SignatureRecord signature : report.signatures()) {
            signaturesByRole.put(signature.role(), signature);
        }
        for (FinanceSignoffBaselineRegistry.RequiredSignatureRole role : report.requiredSignatureRoles()) {
            SignatureRecord signature = signaturesByRole.get(role.role());
            markdown.append("| ")
                    .append(role.label()).append(" | ")
                    .append(signature == null ? "" : signature.signerName()).append(" | ")
                    .append(signature == null ? "" : signature.signedAt()).append(" | ")
                    .append(signature == null ? "" : signature.comment()).append(" |\n");
        }
        if (!report.failureMessage().isEmpty()) {
            markdown.append("\n## 未完成项\n\n");
            markdown.append("- ").append(report.failureMessage()).append("\n");
        }
        return markdown.toString();
    }

    private static String baselineAdoptionText(SignoffBaselineReport report) {
        if (report.accepted() && "SIGNED".equals(report.signoffStatus())) {
            return "已采信。已签字基线可作为 scorecard 已接受差异基线。";
        }
        return "未采信。当前仅代表工程证据包已成文，缺少财务/审计签字时不得作为 scorecard 已接受差异基线。";
    }

    private static String normalizeStatus(String status) {
        String normalized = textOrEmpty(status).trim().toUpperCase(java.util.Locale.ROOT);
        return normalized.isEmpty() ? "MISSING" : normalized;
    }

    private static BigDecimal cents(BigDecimal amount) {
        return amount == null ? ZERO_CENTS : amount.setScale(2, RoundingMode.HALF_UP);
    }

    private static String amountText(BigDecimal amount) {
        return cents(amount).toPlainString();
    }

    private static String firstPresent(String value, String fallback) {
        return value == null || value.isBlank() ? textOrEmpty(fallback) : value;
    }

    private static String textOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private static <T> List<T> copyOrEmpty(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    public record EvidenceRecord(
            String evidenceId,
            String feature,
            String command,
            String evidencePath,
            String status) {
        public EvidenceRecord {
            evidenceId = textOrEmpty(evidenceId);
            feature = textOrEmpty(feature);
            command = textOrEmpty(command);
            evidencePath = textOrEmpty(evidencePath);
            status = normalizeStatus(status);
        }
    }

    public record SignatureRecord(
            String role,
            String signerName,
            String signedAt,
            String comment) {
        public SignatureRecord {
            role = textOrEmpty(role);
            signerName = textOrEmpty(signerName);
            signedAt = textOrEmpty(signedAt);
            comment = textOrEmpty(comment);
        }
    }

    public record EvidenceStatus(
            String evidenceId,
            String feature,
            String label,
            String command,
            String evidencePath,
            String status) {
        public EvidenceStatus {
            evidenceId = textOrEmpty(evidenceId);
            feature = textOrEmpty(feature);
            label = textOrEmpty(label);
            command = textOrEmpty(command);
            evidencePath = textOrEmpty(evidencePath);
            status = normalizeStatus(status);
        }
    }

    public record SignoffBaselineReport(
            String baselineId,
            String title,
            String accountPeriod,
            String scorecardPolicyId,
            boolean engineeringReady,
            boolean accepted,
            String signoffStatus,
            String failureMessage,
            String scorecardHealthStatus,
            BigDecimal scorecardPassRate,
            BigDecimal scorecardMaxDifference,
            List<EvidenceStatus> requiredEvidence,
            List<FinanceSignoffBaselineRegistry.RequiredSignatureRole> requiredSignatureRoles,
            List<SignatureRecord> signatures,
            String baselineMarkdown) {
        public SignoffBaselineReport {
            baselineId = textOrEmpty(baselineId);
            title = textOrEmpty(title);
            accountPeriod = textOrEmpty(accountPeriod);
            scorecardPolicyId = textOrEmpty(scorecardPolicyId);
            signoffStatus = textOrEmpty(signoffStatus);
            failureMessage = textOrEmpty(failureMessage);
            scorecardHealthStatus = textOrEmpty(scorecardHealthStatus);
            scorecardPassRate = cents(scorecardPassRate);
            scorecardMaxDifference = cents(scorecardMaxDifference);
            requiredEvidence = copyOrEmpty(requiredEvidence);
            requiredSignatureRoles = copyOrEmpty(requiredSignatureRoles);
            signatures = copyOrEmpty(signatures);
            baselineMarkdown = textOrEmpty(baselineMarkdown);
        }

        private SignoffBaselineReport withBaselineMarkdown(String baselineMarkdown) {
            return new SignoffBaselineReport(
                    baselineId,
                    title,
                    accountPeriod,
                    scorecardPolicyId,
                    engineeringReady,
                    accepted,
                    signoffStatus,
                    failureMessage,
                    scorecardHealthStatus,
                    scorecardPassRate,
                    scorecardMaxDifference,
                    requiredEvidence,
                    requiredSignatureRoles,
                    signatures,
                    baselineMarkdown);
        }
    }
}
