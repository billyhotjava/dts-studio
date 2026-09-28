package com.yuzhi.dts.copilot.ai.service.copilot;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class OntologyService {

    private final SemanticPackService semanticPackService;

    public OntologyService(SemanticPackService semanticPackService) {
        this.semanticPackService = semanticPackService;
    }

    public Optional<OntologyModel> load(String domain) {
        return semanticPackService.getPack(domain).map(OntologyModel::from);
    }

    public static final class OntologyModel {

        private final Map<String, SemanticPackService.SemanticObject> objectIndex;
        private final Map<String, List<SemanticPackService.OntologyLink>> linkGraph;
        private final Map<String, List<SemanticPackService.OntologyMetric>> metricIndex;
        private final Map<String, List<SemanticPackService.OntologySignal>> signalIndex;
        private final Map<String, List<SemanticPackService.OntologyAction>> actionIndex;

        private OntologyModel(
                Map<String, SemanticPackService.SemanticObject> objectIndex,
                Map<String, List<SemanticPackService.OntologyLink>> linkGraph,
                Map<String, List<SemanticPackService.OntologyMetric>> metricIndex,
                Map<String, List<SemanticPackService.OntologySignal>> signalIndex,
                Map<String, List<SemanticPackService.OntologyAction>> actionIndex) {
            this.objectIndex = objectIndex;
            this.linkGraph = linkGraph;
            this.metricIndex = metricIndex;
            this.signalIndex = signalIndex;
            this.actionIndex = actionIndex;
        }

        private static OntologyModel from(SemanticPackService.SemanticPack pack) {
            Map<String, SemanticPackService.SemanticObject> objectIndex = new LinkedHashMap<>();
            for (SemanticPackService.SemanticObject object : pack.objects()) {
                objectIndex.put(object.name(), object);
            }
            return new OntologyModel(
                    Collections.unmodifiableMap(objectIndex),
                    groupLinks(pack.links()),
                    groupMetrics(pack.metrics()),
                    groupSignals(pack.signals()),
                    groupActions(pack.actions()));
        }

        public Optional<SemanticPackService.SemanticObject> getObject(String name) {
            return Optional.ofNullable(objectIndex.get(name));
        }

        public List<SemanticPackService.SemanticObject> objects() {
            return List.copyOf(objectIndex.values());
        }

        public List<SemanticPackService.OntologyLink> neighbors(String objectName) {
            return linkGraph.getOrDefault(objectName, List.of());
        }

        public List<SemanticPackService.OntologyMetric> metricsOf(String objectName) {
            return metricIndex.getOrDefault(objectName, List.of());
        }

        public List<SemanticPackService.OntologySignal> signalsOf(String objectName) {
            return signalIndex.getOrDefault(objectName, List.of());
        }

        public List<SemanticPackService.OntologyAction> actionsOf(String objectName) {
            return actionIndex.getOrDefault(objectName, List.of());
        }

        public Optional<SemanticPackService.OntologyAction> getAction(String actionName) {
            return actionIndex.values().stream()
                    .flatMap(List::stream)
                    .filter(action -> action.name().equals(actionName))
                    .findFirst();
        }

        public Optional<SignalPlan> buildSignalPlan(String signalName) {
            return allSignals().stream()
                    .filter(signal -> signal.name().equals(signalName))
                    .findFirst()
                    .flatMap(this::buildSignalPlan);
        }

        public List<SignalPlan> buildSignalPlans() {
            List<SignalPlan> plans = new ArrayList<>();
            for (SemanticPackService.OntologySignal signal : allSignals()) {
                buildSignalPlan(signal).ifPresent(plans::add);
            }
            return List.copyOf(plans);
        }

        public List<SignalEvaluation> evaluateSignals(Map<String, BigDecimal> metricValues) {
            if (metricValues == null || metricValues.isEmpty()) {
                return List.of();
            }
            List<SignalEvaluation> evaluations = new ArrayList<>();
            for (SemanticPackService.OntologySignal signal : allSignals()) {
                if (matchesCondition(signal.when(), metricValues)) {
                    evaluations.add(new SignalEvaluation(
                            signal.name(),
                            signal.object(),
                            signal.severity(),
                            signal.advice(),
                            signal.linkedActions()));
                }
            }
            return List.copyOf(evaluations);
        }

        public Optional<JoinPlan> buildJoinPlan(String fromObject, String toObject) {
            List<JoinPlan> candidates = buildJoinPlans(fromObject, toObject);
            if (candidates.size() != 1) {
                return Optional.empty();
            }
            return Optional.of(candidates.getFirst());
        }

        public List<JoinPlan> buildJoinPlans(String fromObject, String toObject) {
            if (!objectIndex.containsKey(fromObject) || !objectIndex.containsKey(toObject)) {
                return List.of();
            }
            List<List<SemanticPackService.OntologyLink>> paths = findPaths(fromObject, toObject);
            List<JoinPlan> plans = new ArrayList<>();
            for (List<SemanticPackService.OntologyLink> path : paths) {
                buildJoinPlan(fromObject, path).ifPresent(plans::add);
            }
            return List.copyOf(plans);
        }

        private Optional<SignalPlan> buildSignalPlan(SemanticPackService.OntologySignal signal) {
            SemanticPackService.SemanticObject object = objectIndex.get(signal.object());
            if (object == null) {
                return Optional.empty();
            }
            List<String> metricNames = metricNamesInCondition(signal.when());
            if (metricNames.isEmpty()) {
                return Optional.empty();
            }
            String sql = buildSignalSql(signal, object, metricNames);
            return Optional.of(new SignalPlan(
                    signal.name(),
                    signal.object(),
                    signal.severity(),
                    signal.advice(),
                    signal.linkedActions(),
                    sql,
                    List.of(object.view()),
                    metricNames));
        }

        private String buildSignalSql(
                SemanticPackService.OntologySignal signal,
                SemanticPackService.SemanticObject object,
                List<String> metricNames) {
            StringBuilder sql = new StringBuilder("SELECT ");
            List<String> groupColumns = object.keyDimensions().isEmpty()
                    ? List.of()
                    : object.keyDimensions();
            for (int i = 0; i < groupColumns.size(); i++) {
                if (i > 0) {
                    sql.append(", ");
                }
                sql.append("s0.").append(quoteIdentifier(groupColumns.get(i)));
            }
            if (!groupColumns.isEmpty()) {
                sql.append(", ");
            }
            sql.append("'").append(escapeSqlLiteral(signal.name())).append("' AS signal_name, ")
                    .append("'").append(escapeSqlLiteral(signal.severity())).append("' AS severity\n")
                    .append("FROM ").append(object.view()).append(" s0\n");
            if (!groupColumns.isEmpty()) {
                sql.append("GROUP BY ");
                for (int i = 0; i < groupColumns.size(); i++) {
                    if (i > 0) {
                        sql.append(", ");
                    }
                    sql.append("s0.").append(quoteIdentifier(groupColumns.get(i)));
                }
                sql.append("\n");
            }
            sql.append("HAVING ").append(compileSignalCondition(signal.when(), metricNames));
            return sql.toString();
        }

        private String compileSignalCondition(String condition, List<String> metricNames) {
            String compiled = condition;
            for (String metricName : metricNames) {
                Optional<SemanticPackService.OntologyMetric> metric = metricByName(metricName);
                if (metric.isPresent()) {
                    compiled = compiled.replace(metricName, "(" + metric.get().expr() + ")");
                }
            }
            return compiled;
        }

        private List<String> metricNamesInCondition(String condition) {
            List<MetricMention> mentions = new ArrayList<>();
            for (SemanticPackService.OntologyMetric metric : allMetrics()) {
                int index = condition.indexOf(metric.name());
                if (index >= 0) {
                    mentions.add(new MetricMention(metric.name(), index));
                }
            }
            mentions.sort((left, right) -> Integer.compare(left.index(), right.index()));
            List<String> names = new ArrayList<>();
            for (MetricMention mention : mentions) {
                names.add(mention.name());
            }
            return List.copyOf(names);
        }

        private Optional<SemanticPackService.OntologyMetric> metricByName(String metricName) {
            return allMetrics().stream()
                    .filter(metric -> metric.name().equals(metricName))
                    .findFirst();
        }

        private List<SemanticPackService.OntologyMetric> allMetrics() {
            return metricIndex.values().stream().flatMap(List::stream).toList();
        }

        private List<SemanticPackService.OntologySignal> allSignals() {
            return signalIndex.values().stream().flatMap(List::stream).toList();
        }

        private static boolean matchesCondition(String condition, Map<String, BigDecimal> metricValues) {
            for (String clause : condition.split("(?i)\\s+AND\\s+")) {
                if (!matchesClause(clause.trim(), metricValues)) {
                    return false;
                }
            }
            return true;
        }

        private static boolean matchesClause(String clause, Map<String, BigDecimal> metricValues) {
            Matcher matcher = Pattern.compile("^(.+?)\\s*(>=|<=|>|<|=)\\s*(-?\\d+(?:\\.\\d+)?)$").matcher(clause);
            if (!matcher.matches()) {
                return false;
            }
            BigDecimal actual = metricValues.get(matcher.group(1).trim());
            if (actual == null) {
                return false;
            }
            BigDecimal expected = new BigDecimal(matcher.group(3));
            int comparison = actual.compareTo(expected);
            return switch (matcher.group(2)) {
                case ">" -> comparison > 0;
                case ">=" -> comparison >= 0;
                case "<" -> comparison < 0;
                case "<=" -> comparison <= 0;
                case "=" -> comparison == 0;
                default -> false;
            };
        }

        private List<List<SemanticPackService.OntologyLink>> findPaths(String fromObject, String toObject) {
            Queue<PathState> queue = new ArrayDeque<>();
            Set<String> rootVisited = new LinkedHashSet<>();
            rootVisited.add(fromObject);
            queue.add(new PathState(fromObject, List.of(), rootVisited));
            List<List<SemanticPackService.OntologyLink>> paths = new ArrayList<>();
            int shortestLength = -1;

            while (!queue.isEmpty()) {
                PathState current = queue.remove();
                if (shortestLength >= 0 && current.path().size() > shortestLength) {
                    continue;
                }
                if (current.objectName().equals(toObject)) {
                    shortestLength = current.path().size();
                    paths.add(current.path());
                    continue;
                }
                for (SemanticPackService.OntologyLink link : neighbors(current.objectName())) {
                    if (!objectIndex.containsKey(link.to()) || current.visitedObjects().contains(link.to())) {
                        continue;
                    }
                    List<SemanticPackService.OntologyLink> nextPath = new ArrayList<>(current.path());
                    nextPath.add(link);
                    Set<String> nextVisited = new LinkedHashSet<>(current.visitedObjects());
                    nextVisited.add(link.to());
                    queue.add(new PathState(link.to(), List.copyOf(nextPath), Collections.unmodifiableSet(nextVisited)));
                }
            }
            return List.copyOf(paths);
        }

        private Optional<JoinPlan> buildJoinPlan(
                String fromObject,
                List<SemanticPackService.OntologyLink> path) {
            SemanticPackService.SemanticObject root = objectIndex.get(fromObject);
            StringBuilder sql = new StringBuilder("SELECT *\nFROM ")
                    .append(root.view())
                    .append(" o0");
            Set<String> sourceRefs = new LinkedHashSet<>();
            sourceRefs.add(root.view());
            List<String> joinHints = new ArrayList<>();
            List<String> linkNames = new ArrayList<>();

            for (int i = 0; i < path.size(); i++) {
                SemanticPackService.OntologyLink link = path.get(i);
                SemanticPackService.SemanticObject target = objectIndex.get(link.to());
                if (target == null) {
                    return Optional.empty();
                }
                String leftAlias = "o" + i;
                String rightAlias = "o" + (i + 1);
                sql.append("\nLEFT JOIN ")
                        .append(target.view())
                        .append(" ")
                        .append(rightAlias)
                        .append(" ON ");
                appendJoinCondition(sql, link, leftAlias, rightAlias, i);
                sourceRefs.add(target.view());
                linkNames.add(link.name());
                if (!link.joinHint().isBlank()) {
                    joinHints.add(link.name() + ": " + link.joinHint());
                }
            }

            return Optional.of(new JoinPlan(
                    sql.toString(),
                    List.copyOf(sourceRefs),
                    List.copyOf(linkNames),
                    true,
                    List.copyOf(joinHints)));
        }

        private static Map<String, List<SemanticPackService.OntologyLink>> groupLinks(
                List<SemanticPackService.OntologyLink> links) {
            Map<String, List<SemanticPackService.OntologyLink>> grouped = new LinkedHashMap<>();
            for (SemanticPackService.OntologyLink link : links) {
                grouped.computeIfAbsent(link.from(), ignored -> new ArrayList<>()).add(link);
            }
            return deepUnmodifiable(grouped);
        }

        private static Map<String, List<SemanticPackService.OntologyMetric>> groupMetrics(
                List<SemanticPackService.OntologyMetric> metrics) {
            Map<String, List<SemanticPackService.OntologyMetric>> grouped = new LinkedHashMap<>();
            for (SemanticPackService.OntologyMetric metric : metrics) {
                grouped.computeIfAbsent(metric.object(), ignored -> new ArrayList<>()).add(metric);
            }
            return deepUnmodifiable(grouped);
        }

        private static Map<String, List<SemanticPackService.OntologySignal>> groupSignals(
                List<SemanticPackService.OntologySignal> signals) {
            Map<String, List<SemanticPackService.OntologySignal>> grouped = new LinkedHashMap<>();
            for (SemanticPackService.OntologySignal signal : signals) {
                grouped.computeIfAbsent(signal.object(), ignored -> new ArrayList<>()).add(signal);
            }
            return deepUnmodifiable(grouped);
        }

        private static Map<String, List<SemanticPackService.OntologyAction>> groupActions(
                List<SemanticPackService.OntologyAction> actions) {
            Map<String, List<SemanticPackService.OntologyAction>> grouped = new LinkedHashMap<>();
            for (SemanticPackService.OntologyAction action : actions) {
                grouped.computeIfAbsent(action.object(), ignored -> new ArrayList<>()).add(action);
            }
            return deepUnmodifiable(grouped);
        }

        private static <T> Map<String, List<T>> deepUnmodifiable(Map<String, List<T>> grouped) {
            Map<String, List<T>> copy = new LinkedHashMap<>();
            for (Map.Entry<String, List<T>> entry : grouped.entrySet()) {
                copy.put(entry.getKey(), List.copyOf(entry.getValue()));
            }
            return Collections.unmodifiableMap(copy);
        }

        private static void appendJoinCondition(
                StringBuilder sql,
                SemanticPackService.OntologyLink link,
                String leftAlias,
                String rightAlias,
                int joinIndex) {
            if (usesJsonArrayExpansion(link)) {
                String jsonAlias = "j" + joinIndex;
                sql.append("EXISTS (SELECT 1 FROM jsonb_array_elements_text(")
                        .append(rightAlias)
                        .append(".")
                        .append(quoteIdentifier(link.toKey()))
                        .append("::jsonb) AS ")
                        .append(jsonAlias)
                        .append("(value) WHERE ")
                        .append(jsonAlias)
                        .append(".value = ")
                        .append(leftAlias)
                        .append(".")
                        .append(quoteIdentifier(link.fromKey()))
                        .append("::text)");
                return;
            }
            sql.append(leftAlias)
                    .append(".")
                    .append(quoteIdentifier(link.fromKey()))
                    .append(" = ")
                    .append(rightAlias)
                    .append(".")
                    .append(quoteIdentifier(link.toKey()));
        }

        private static boolean usesJsonArrayExpansion(SemanticPackService.OntologyLink link) {
            return "biz_ids_json".equals(link.toKey()) || link.joinHint().contains("JSON");
        }

        private static String quoteIdentifier(String identifier) {
            return "\"" + identifier.replace("\"", "\"\"") + "\"";
        }

        private static String escapeSqlLiteral(String value) {
            return value.replace("'", "''");
        }

        private record MetricMention(String name, int index) {
        }

        private record PathState(
                String objectName,
                List<SemanticPackService.OntologyLink> path,
                Set<String> visitedObjects) {
        }
    }

    public record JoinPlan(
            String sql,
            List<String> sourceRefs,
            List<String> linkNames,
            boolean preservesOrphans,
            List<String> joinHints) {
    }

    public record SignalPlan(
            String signalName,
            String objectName,
            String severity,
            String advice,
            List<String> linkedActions,
            String sql,
            List<String> sourceRefs,
            List<String> metricNames) {
        public SignalPlan {
            linkedActions = linkedActions == null ? List.of() : List.copyOf(linkedActions);
            sourceRefs = sourceRefs == null ? List.of() : List.copyOf(sourceRefs);
            metricNames = metricNames == null ? List.of() : List.copyOf(metricNames);
        }
    }

    public record SignalEvaluation(
            String signalName,
            String objectName,
            String severity,
            String advice,
            List<String> linkedActions) {
        public SignalEvaluation {
            linkedActions = linkedActions == null ? List.of() : List.copyOf(linkedActions);
        }
    }
}
