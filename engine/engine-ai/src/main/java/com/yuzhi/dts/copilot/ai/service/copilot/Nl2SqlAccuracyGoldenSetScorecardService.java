package com.yuzhi.dts.copilot.ai.service.copilot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class Nl2SqlAccuracyGoldenSetScorecardService {

    public ScorecardReport score(
            List<Nl2SqlAccuracyGoldenSetRegistry.GoldenCase> goldenCases,
            List<GoldenCaseRun> runs) {
        List<Nl2SqlAccuracyGoldenSetRegistry.GoldenCase> safeCases =
                goldenCases == null ? List.of() : goldenCases;
        Map<String, GoldenCaseRun> runsByCase = runsByCase(runs);
        List<Failure> failures = new ArrayList<>();
        int passedCases = 0;

        for (Nl2SqlAccuracyGoldenSetRegistry.GoldenCase goldenCase : safeCases) {
            GoldenCaseRun run = runsByCase.get(goldenCase.id());
            List<Failure> caseFailures = failures(goldenCase, run);
            if (caseFailures.isEmpty()) {
                passedCases++;
            } else {
                failures.addAll(caseFailures);
            }
        }
        failures.addAll(metamorphicFailures(safeCases, runsByCase));
        failures = failures.stream()
                .sorted(Comparator.comparing(Failure::caseId).thenComparing(Failure::reason))
                .toList();
        boolean passed = failures.isEmpty();
        return new ScorecardReport(
                passed,
                passed ? "PASS" : "FAIL",
                safeCases.size(),
                passedCases,
                passRate(passedCases, safeCases.size()),
                failures);
    }

    private Map<String, GoldenCaseRun> runsByCase(List<GoldenCaseRun> runs) {
        if (runs == null || runs.isEmpty()) {
            return Map.of();
        }
        Map<String, GoldenCaseRun> result = new LinkedHashMap<>();
        for (GoldenCaseRun run : runs) {
            if (run == null || !StringUtils.hasText(run.caseId())) {
                continue;
            }
            result.putIfAbsent(run.caseId(), run);
        }
        return result;
    }

    private List<Failure> failures(
            Nl2SqlAccuracyGoldenSetRegistry.GoldenCase goldenCase,
            GoldenCaseRun run) {
        if (run == null) {
            return List.of(new Failure(goldenCase.id(), "missing golden case run: " + goldenCase.id()));
        }
        List<Failure> failures = new ArrayList<>();
        if (StringUtils.hasText(goldenCase.expectedTemplate())
                && !goldenCase.expectedTemplate().equalsIgnoreCase(run.templateCode())) {
            failures.add(new Failure(goldenCase.id(),
                    "template mismatch: case=" + goldenCase.id()
                            + ", expected=" + goldenCase.expectedTemplate()
                            + ", actual=" + run.templateCode()));
        }
        if (StringUtils.hasText(goldenCase.expectedTarget())
                && !sameText(goldenCase.expectedTarget(), run.targetRelation())) {
            failures.add(new Failure(goldenCase.id(),
                    "target mismatch: case=" + goldenCase.id()
                            + ", expected=" + goldenCase.expectedTarget()
                            + ", actual=" + run.targetRelation()));
        }
        if (StringUtils.hasText(goldenCase.expectedDataSurface())
                && !sameText(goldenCase.expectedDataSurface(), run.dataSurface())) {
            failures.add(new Failure(goldenCase.id(),
                    "data surface mismatch: case=" + goldenCase.id()
                            + ", expected=" + goldenCase.expectedDataSurface()
                            + ", actual=" + run.dataSurface()));
        }
        if (gradeRank(run.grade()) < gradeRank(goldenCase.expectedGradeAtLeast())) {
            failures.add(new Failure(goldenCase.id(),
                    "grade below minimum: case=" + goldenCase.id()
                            + ", expectedAtLeast=" + goldenCase.expectedGradeAtLeast()
                            + ", actual=" + run.grade()));
        }
        if (isL0Profile(run)) {
            failures.add(new Failure(goldenCase.id(),
                    "golden case fell back to L0/profile path: case=" + goldenCase.id()
                            + ", dataSurface=" + run.dataSurface()
                            + ", target=" + run.targetRelation()));
        }
        if (isApplicationMysql(run)) {
            failures.add(new Failure(goldenCase.id(),
                    "golden case fell back to 应用 MySQL path: case=" + goldenCase.id()
                            + ", sql=" + run.sql()
                            + ", target=" + run.targetRelation()));
        }
        for (String fragment : goldenCase.expectedSqlFragments()) {
            if (StringUtils.hasText(fragment) && !containsIgnoreCase(run.sql(), fragment)) {
                failures.add(new Failure(goldenCase.id(),
                        "required SQL fragment missing: case=" + goldenCase.id() + ", fragment=" + fragment));
            }
        }
        for (String fragment : goldenCase.forbiddenSqlFragments()) {
            if (StringUtils.hasText(fragment) && containsIgnoreCase(run.sql(), fragment)) {
                failures.add(new Failure(goldenCase.id(),
                        "forbidden SQL fragment present: case=" + goldenCase.id() + ", fragment=" + fragment));
            }
        }
        for (String evidenceKey : goldenCase.requiredEvidence()) {
            String value = run.evidence().get(evidenceKey);
            if (!StringUtils.hasText(value) || "MISSING".equalsIgnoreCase(value) || "UNKNOWN".equalsIgnoreCase(value)) {
                failures.add(new Failure(goldenCase.id(),
                        "required evidence missing: case=" + goldenCase.id()
                                + ", evidence=" + evidenceKey
                                + ", actual=" + (value == null ? "" : value)));
            }
        }
        return failures;
    }

    private List<Failure> metamorphicFailures(
            List<Nl2SqlAccuracyGoldenSetRegistry.GoldenCase> goldenCases,
            Map<String, GoldenCaseRun> runsByCase) {
        Map<String, List<Nl2SqlAccuracyGoldenSetRegistry.GoldenCase>> groups = new LinkedHashMap<>();
        for (Nl2SqlAccuracyGoldenSetRegistry.GoldenCase goldenCase : goldenCases) {
            if (StringUtils.hasText(goldenCase.metamorphicGroup())) {
                groups.computeIfAbsent(goldenCase.metamorphicGroup(), ignored -> new ArrayList<>()).add(goldenCase);
            }
        }
        List<Failure> failures = new ArrayList<>();
        groups.forEach((group, cases) -> {
            if (cases.size() < 2) {
                return;
            }
            Map<String, String> templates = new LinkedHashMap<>();
            Map<String, String> targets = new LinkedHashMap<>();
            for (Nl2SqlAccuracyGoldenSetRegistry.GoldenCase goldenCase : cases) {
                GoldenCaseRun run = runsByCase.get(goldenCase.id());
                if (run != null) {
                    putOriginalByNormalizedText(templates, run.templateCode());
                    putOriginalByNormalizedText(targets, run.targetRelation());
                }
            }
            if (templates.size() > 1 || targets.size() > 1) {
                failures.add(new Failure(
                        cases.getFirst().id(),
                        "metamorphic drift: group=" + group
                                + ", templates=" + templates.values()
                                + ", targets=" + targets.values()));
            }
        });
        return failures;
    }

    private void putOriginalByNormalizedText(Map<String, String> values, String value) {
        String normalized = text(value);
        if (StringUtils.hasText(normalized)) {
            values.putIfAbsent(normalized, value == null ? "" : value.trim());
        }
    }

    private boolean isL0Profile(GoldenCaseRun run) {
        String text = (run.dataSurface() + " " + run.targetRelation()).toLowerCase(Locale.ROOT);
        return text.contains("l0_business_object_profile") || text.contains(".profile");
    }

    private boolean isApplicationMysql(GoldenCaseRun run) {
        String text = (run.targetRelation() + " " + run.sql()).toLowerCase(Locale.ROOT);
        return text.contains("mysql.rs_cloud_flower.")
                || text.contains("jdbc:mysql:")
                || text.contains("application-mysql")
                || text.contains("应用 mysql")
                || text.contains("应用mysql");
    }

    private boolean sameText(String expected, String actual) {
        return text(expected).equals(text(actual));
    }

    private boolean containsIgnoreCase(String text, String fragment) {
        return text(text).contains(text(fragment));
    }

    private String text(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private int gradeRank(String grade) {
        return switch (text(grade).toUpperCase(Locale.ROOT)) {
            case "HIGH" -> 3;
            case "MEDIUM" -> 2;
            case "LOW" -> 1;
            default -> 0;
        };
    }

    private double passRate(int passed, int total) {
        if (total <= 0) {
            return 0.0d;
        }
        return Math.round((passed * 10000.0d) / total) / 100.0d;
    }

    public record GoldenCaseRun(
            String caseId,
            String templateCode,
            String targetRelation,
            String dataSurface,
            String grade,
            String sql,
            Map<String, String> evidence) {
        public GoldenCaseRun {
            caseId = caseId == null ? "" : caseId.trim();
            templateCode = templateCode == null ? "" : templateCode.trim();
            targetRelation = targetRelation == null ? "" : targetRelation.trim();
            dataSurface = dataSurface == null ? "" : dataSurface.trim();
            grade = grade == null ? "" : grade.trim();
            sql = sql == null ? "" : sql.trim();
            evidence = evidence == null ? Map.of() : Map.copyOf(evidence);
        }
    }

    public record ScorecardReport(
            boolean passed,
            String healthStatus,
            int totalCases,
            int passedCases,
            double passRate,
            List<Failure> failures) {
        public ScorecardReport {
            healthStatus = healthStatus == null ? "" : healthStatus;
            totalCases = Math.max(totalCases, 0);
            passedCases = Math.max(passedCases, 0);
            failures = failures == null ? List.of() : List.copyOf(failures);
        }
    }

    public record Failure(String caseId, String reason) {
        public Failure {
            caseId = caseId == null ? "" : caseId;
            reason = reason == null ? "" : reason;
        }
    }
}
