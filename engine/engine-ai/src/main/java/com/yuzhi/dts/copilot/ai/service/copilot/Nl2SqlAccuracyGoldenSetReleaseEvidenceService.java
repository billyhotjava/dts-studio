package com.yuzhi.dts.copilot.ai.service.copilot;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class Nl2SqlAccuracyGoldenSetReleaseEvidenceService {

    private static final Pattern JDBC_URL_PATTERN = Pattern.compile("jdbc:[^\\s|]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern SECRET_ASSIGNMENT_PATTERN =
            Pattern.compile("(?i)(password|token|secret)=([^\\s&|]+)");

    private final Clock clock;

    public Nl2SqlAccuracyGoldenSetReleaseEvidenceService() {
        this(Clock.systemUTC());
    }

    Nl2SqlAccuracyGoldenSetReleaseEvidenceService(Clock clock) {
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    public ReleaseEvidence publish(
            String runId,
            Nl2SqlAccuracyGoldenSetScorecardService.ScorecardReport scorecard,
            List<WeakPathSource> weakPathSources) {
        String safeRunId = StringUtils.hasText(runId) ? runId.trim() : "sprint34-nl2sql-accuracy-gate";
        if (scorecard == null) {
            return new ReleaseEvidence(
                    safeRunId,
                    Instant.now(clock),
                    "MISSING_SCORECARD",
                    false,
                    0,
                    0,
                    0.0d,
                    "scorecard report is missing",
                    List.of(),
                    copyWeakPathSources(weakPathSources));
        }
        return new ReleaseEvidence(
                safeRunId,
                Instant.now(clock),
                scorecard.healthStatus(),
                scorecard.passed(),
                scorecard.totalCases(),
                scorecard.passedCases(),
                scorecard.passRate(),
                failureSummary(scorecard.failures()),
                scorecard.failures(),
                copyWeakPathSources(weakPathSources));
    }

    public String toMarkdown(ReleaseEvidence evidence) {
        ReleaseEvidence safe = evidence == null
                ? publish("sprint34-nl2sql-accuracy-gate", null, List.of())
                : evidence;
        StringBuilder markdown = new StringBuilder();
        markdown.append("# Sprint-34 NL2SQL Accuracy Scorecard Evidence\n\n");
        markdown.append("- runId: ").append(safe.runId()).append('\n');
        markdown.append("- generatedAt: ").append(safe.generatedAt()).append('\n');
        markdown.append("- healthStatus: ").append(safe.healthStatus()).append('\n');
        markdown.append("- releaseGatePassed=").append(safe.releaseGatePassed()).append('\n');
        markdown.append("- passRate: ").append(safe.passRate()).append('\n');
        markdown.append("- cases: ").append(safe.passedCases()).append('/').append(safe.totalCases()).append('\n');
        if (StringUtils.hasText(safe.failureSummary())) {
            markdown.append("- failureSummary: ").append(redact(safe.failureSummary())).append('\n');
        }
        if (!safe.failures().isEmpty()) {
            markdown.append("\n## Failures\n");
            for (Nl2SqlAccuracyGoldenSetScorecardService.Failure failure : safe.failures()) {
                markdown.append("- ").append(redact(failure.caseId()))
                        .append(": ").append(redact(failure.reason()))
                        .append('\n');
            }
        }
        if (!safe.weakPathSources().isEmpty()) {
            markdown.append("\n## Weak Path Sources\n");
            for (WeakPathSource source : safe.weakPathSources()) {
                markdown.append("- ").append(redact(source.sourceId()))
                        .append(" | ").append(redact(source.question()))
                        .append(" | ").append(redact(source.dataSurface()))
                        .append(" | count=").append(source.count())
                        .append(" | action=").append(redact(source.suggestedAction()))
                        .append('\n');
            }
        }
        return markdown.toString();
    }

    private String redact(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String redacted = JDBC_URL_PATTERN.matcher(value).replaceAll("[REDACTED_JDBC_URL]");
        return SECRET_ASSIGNMENT_PATTERN.matcher(redacted).replaceAll("$1=[REDACTED]");
    }

    private List<WeakPathSource> copyWeakPathSources(List<WeakPathSource> sources) {
        if (sources == null || sources.isEmpty()) {
            return List.of();
        }
        return sources.stream()
                .filter(item -> item != null && StringUtils.hasText(item.sourceId()))
                .toList();
    }

    private String failureSummary(List<Nl2SqlAccuracyGoldenSetScorecardService.Failure> failures) {
        if (failures == null || failures.isEmpty()) {
            return "";
        }
        return failures.stream()
                .map(failure -> failure.caseId() + ": " + failure.reason())
                .reduce((left, right) -> left + "; " + right)
                .orElse("");
    }

    public record WeakPathSource(
            String sourceId,
            String question,
            String dataSurface,
            int count,
            String suggestedAction) {
        public WeakPathSource {
            sourceId = textOrEmpty(sourceId);
            question = textOrEmpty(question);
            dataSurface = textOrEmpty(dataSurface);
            count = Math.max(count, 0);
            suggestedAction = textOrEmpty(suggestedAction);
        }
    }

    public record ReleaseEvidence(
            String runId,
            Instant generatedAt,
            String healthStatus,
            boolean releaseGatePassed,
            int totalCases,
            int passedCases,
            double passRate,
            String failureSummary,
            List<Nl2SqlAccuracyGoldenSetScorecardService.Failure> failures,
            List<WeakPathSource> weakPathSources) {
        public ReleaseEvidence {
            runId = textOrEmpty(runId);
            generatedAt = generatedAt == null ? Instant.EPOCH : generatedAt;
            healthStatus = textOrEmpty(healthStatus);
            totalCases = Math.max(totalCases, 0);
            passedCases = Math.max(passedCases, 0);
            failureSummary = textOrEmpty(failureSummary);
            failures = failures == null ? List.of() : List.copyOf(failures);
            weakPathSources = weakPathSources == null ? List.of() : List.copyOf(weakPathSources);
        }
    }

    private static String textOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
