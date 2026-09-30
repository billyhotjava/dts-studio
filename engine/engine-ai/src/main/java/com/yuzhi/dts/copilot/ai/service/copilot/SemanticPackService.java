package com.yuzhi.dts.copilot.ai.service.copilot;

import com.yuzhi.dts.copilot.ai.service.pack.PackBackedJsonRegistry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Loads and caches active business semantic packs from the Pack registry.
 * Semantic packs provide domain-specific context (object dictionaries, synonyms,
 * few-shot examples) for NL2SQL prompt injection.
 */
@Service
public class SemanticPackService extends PackBackedJsonRegistry {

    private static final Logger log = LoggerFactory.getLogger(SemanticPackService.class);

    private static final String[] PACK_FILES = {
            "semantic-packs/project-fulfillment.json",
            "semantic-packs/field-operations.json",
            "semantic-packs/procurement.json",
            "semantic-packs/warehouse.json",
            "semantic-packs/finance.json",
            "semantic-packs/flowerbiz.json"
    };

    private final ObjectMapper objectMapper;
    private Map<String, JsonNode> packs;
    private Map<String, SemanticPack> semanticPacks;

    public SemanticPackService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public synchronized void init() {
        if (!hasManagedPackReader()) ensurePackResources(this::loadFromPack);
    }

    private void loadFromPack() {
        Map<String, JsonNode> loaded = new LinkedHashMap<>();
        var snapshot = packSnapshot();
        for (var asset : snapshot.assets()) {
            if (asset.kind().equals("ontology")) {
                usePackAsset(asset);
                JsonNode node = asset.json();
                if (loaded.putIfAbsent(node.path("domain").asText(), node.deepCopy()) != null)
                    throw new IllegalStateException("Duplicate active ontology domain");
            }
        }
        if (loaded.isEmpty() && packResources().useLegacy(snapshot)) {
            for (String file : PACK_FILES) {
                try (InputStream stream = openPackResource(file)) {
                    if (stream == null) throw new IllegalStateException("Missing legacy semantic pack: " + file);
                    JsonNode node = objectMapper.readTree(stream);
                    loaded.put(node.path("domain").asText(), node);
                } catch (java.io.IOException e) {
                    throw new IllegalStateException("Invalid legacy semantic pack", e);
                }
            }
        }
        for (var asset : snapshot.assets()) {
            if (!asset.kind().equals("actions")) continue;
            usePackAsset(asset);
            JsonNode action = asset.json();
            List<JsonNode> owners = loaded.values().stream().filter(pack -> {
                if (action.hasNonNull("domain") && !action.path("domain").asText().equals(pack.path("domain").asText())) return false;
                for (JsonNode object : pack.path("objects")) {
                    if (object.path("name").asText().equals(action.path("object").asText())) return true;
                }
                return false;
            }).toList();
            if (owners.size() != 1) throw new IllegalStateException("Action requires exactly one owning ontology");
            var owner = (com.fasterxml.jackson.databind.node.ObjectNode) owners.getFirst();
            var actions = owner.withArray("actions");
            for (JsonNode existing : actions) {
                if (existing.path("name").asText().equals(action.path("name").asText()))
                    throw new IllegalStateException("Duplicate ontology action");
            }
            actions.add(action.deepCopy());
        }
        Map<String, SemanticPack> typed = new LinkedHashMap<>();
        loaded.forEach((domain, node) -> typed.put(domain, parsePack(node)));
        this.packs = Collections.unmodifiableMap(loaded);
        this.semanticPacks = Collections.unmodifiableMap(typed);
        log.info("Semantic packs refreshed: generation={}, domains={}", snapshot.generation(), loaded.size());
    }

    public synchronized JsonNode getDocument(String domain) {
        ensurePackResources(this::loadFromPack);
        JsonNode document = packs.get(domain);
        return document == null ? objectMapper.createObjectNode() : document.deepCopy();
    }

    /**
     * Get few-shot examples for a domain, formatted as "Q: ... A: ..." strings.
     *
     * @param domain the domain key, e.g. "project" or "flowerbiz"
     * @return list of formatted few-shot strings, empty list if domain not found
     */
    public synchronized List<String> getFewShots(String domain) {
        ensurePackResources(this::loadFromPack);
        return fewShots(packs.get(domain));
    }

    private List<String> fewShots(JsonNode pack) {
        if (pack == null) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        JsonNode fewShots = pack.path("fewShots");
        if (fewShots.isArray()) {
            for (JsonNode shot : fewShots) {
                String question = shot.path("question").asText("");
                String sql = shot.path("sql").asText("");
                if (!question.isEmpty() && !sql.isEmpty()) {
                    result.add("Q: " + question + "\nA: " + sql);
                }
            }
        }
        return result;
    }

    /**
     * Get synonym mappings for a domain.
     *
     * @param domain the domain key
     * @return map of term to field/condition description, empty map if domain not found
     */
    public synchronized Map<String, String> getSynonyms(String domain) {
        ensurePackResources(this::loadFromPack);
        return synonyms(packs.get(domain));
    }

    private Map<String, String> synonyms(JsonNode pack) {
        if (pack == null) {
            return Collections.emptyMap();
        }
        Map<String, String> result = new LinkedHashMap<>();
        JsonNode synonyms = pack.path("synonyms");
        if (synonyms.isArray()) {
            for (JsonNode syn : synonyms) {
                String term = syn.path("term").asText("");
                String field = syn.path("field").asText("");
                if (!term.isEmpty() && !field.isEmpty()) {
                    result.put(term, field);
                }
            }
        }
        return result;
    }

    /**
     * Get full context for NL2SQL prompt injection, combining object descriptions,
     * synonyms, and few-shot examples into a readable text block for the LLM.
     *
     * @param domain the domain key
     * @return formatted context string, or empty string if domain not found
     */
    public synchronized String getContextForDomain(String domain) {
        ensurePackResources(this::loadFromPack);
        JsonNode pack = packs.get(domain);
        if (pack == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        // Domain description
        String description = pack.path("description").asText("");
        if (!description.isEmpty()) {
            sb.append("【主题域】").append(description).append("\n\n");
        }

        // Object descriptions
        JsonNode objects = pack.path("objects");
        if (objects.isArray() && !objects.isEmpty()) {
            sb.append("【业务对象】\n");
            for (JsonNode obj : objects) {
                String name = obj.path("name").asText("");
                String view = obj.path("view").asText("");
                String desc = obj.path("description").asText("");
                sb.append("- ").append(name).append(" (").append(view).append("): ").append(desc).append("\n");

                JsonNode dims = obj.path("keyDimensions");
                if (dims.isArray() && !dims.isEmpty()) {
                    sb.append("  维度: ");
                    appendArray(sb, dims);
                    sb.append("\n");
                }
                JsonNode measures = obj.path("keyMeasures");
                if (measures.isArray() && !measures.isEmpty()) {
                    sb.append("  度量: ");
                    appendArray(sb, measures);
                    sb.append("\n");
                }
                JsonNode filters = obj.path("commonFilters");
                if (filters.isArray() && !filters.isEmpty()) {
                    sb.append("  常用筛选: ");
                    appendArray(sb, filters);
                    sb.append("\n");
                }
            }
            sb.append("\n");
        }

        // Synonyms
        Map<String, String> synonyms = synonyms(pack);
        if (!synonyms.isEmpty()) {
            sb.append("【同义词/术语映射】\n");
            for (Map.Entry<String, String> entry : synonyms.entrySet()) {
                sb.append("- ").append(entry.getKey()).append(" -> ").append(entry.getValue()).append("\n");
            }
            sb.append("\n");
        }

        List<String> guardrails = resolveGuardrails(pack);
        if (!guardrails.isEmpty()) {
            sb.append("【口径护栏】\n");
            for (String guardrail : guardrails) {
                sb.append("- ").append(guardrail).append("\n");
            }
            sb.append("\n");
        }

        // Few-shot examples
        List<String> fewShots = fewShots(pack);
        if (!fewShots.isEmpty()) {
            sb.append("【示例查询】\n");
            for (String shot : fewShots) {
                sb.append(shot).append("\n\n");
            }
        }

        return sb.toString();
    }

    /**
     * Get all loaded domain keys.
     *
     * @return set of domain names
     */
    public synchronized Set<String> getDomains() {
        ensurePackResources(this::loadFromPack);
        return packs.keySet();
    }

    public synchronized Optional<SemanticPack> getPack(String domain) {
        ensurePackResources(this::loadFromPack);
        return Optional.ofNullable(semanticPacks.get(domain));
    }

    static SemanticPack parsePack(JsonNode pack) {
        return new SemanticPack(
                pack.path("domain").asText(""),
                pack.path("description").asText(""),
                parseObjects(pack.path("objects")),
                parseSynonyms(pack.path("synonyms")),
                parseFewShots(pack.path("fewShots")),
                resolveGuardrails(pack),
                parseLinks(pack.path("links")),
                parseMetrics(pack.path("metrics")),
                parseSignals(pack.path("signals")),
                parseActions(pack.path("actions")));
    }

    private static List<SemanticObject> parseObjects(JsonNode objects) {
        if (!objects.isArray()) {
            return List.of();
        }
        List<SemanticObject> parsed = new ArrayList<>();
        for (JsonNode object : objects) {
            String name = object.path("name").asText("");
            String view = object.path("view").asText("");
            if (name.isBlank() || view.isBlank()) {
                continue;
            }
            parsed.add(new SemanticObject(
                    name,
                    view,
                    object.path("description").asText(""),
                    parseTextArray(object.path("keyDimensions")),
                    parseTextArray(object.path("keyMeasures")),
                    parseTextArray(object.path("commonFilters")),
                    object.path("defaultTimeField").asText("")));
        }
        return List.copyOf(parsed);
    }

    private static Map<String, String> parseSynonyms(JsonNode synonyms) {
        if (!synonyms.isArray()) {
            return Map.of();
        }
        Map<String, String> parsed = new LinkedHashMap<>();
        for (JsonNode synonym : synonyms) {
            String term = synonym.path("term").asText("");
            String field = synonym.path("field").asText("");
            if (!term.isBlank() && !field.isBlank()) {
                parsed.put(term, field);
            }
        }
        return Collections.unmodifiableMap(parsed);
    }

    private static List<FewShot> parseFewShots(JsonNode fewShots) {
        if (!fewShots.isArray()) {
            return List.of();
        }
        List<FewShot> parsed = new ArrayList<>();
        for (JsonNode shot : fewShots) {
            String question = shot.path("question").asText("");
            String sql = shot.path("sql").asText("");
            if (!question.isBlank() && !sql.isBlank()) {
                parsed.add(new FewShot(question, sql));
            }
        }
        return List.copyOf(parsed);
    }

    private static List<OntologyLink> parseLinks(JsonNode links) {
        if (!links.isArray()) {
            return List.of();
        }
        List<OntologyLink> parsed = new ArrayList<>();
        for (JsonNode link : links) {
            if (missingAny(link, "name", "from", "to", "fromKey", "toKey", "cardinality")) {
                warnInvalidOntologyEntry("links", link);
                continue;
            }
            parsed.add(new OntologyLink(
                    link.path("name").asText(),
                    link.path("from").asText(),
                    link.path("to").asText(),
                    link.path("fromKey").asText(),
                    link.path("toKey").asText(),
                    link.path("cardinality").asText(),
                    link.path("joinHint").asText(""),
                    link.path("note").asText("")));
        }
        return List.copyOf(parsed);
    }

    private static List<OntologyMetric> parseMetrics(JsonNode metrics) {
        if (!metrics.isArray()) {
            return List.of();
        }
        List<OntologyMetric> parsed = new ArrayList<>();
        for (JsonNode metric : metrics) {
            if (missingAny(metric, "name", "object", "expr", "caliber")) {
                warnInvalidOntologyEntry("metrics", metric);
                continue;
            }
            parsed.add(new OntologyMetric(
                    metric.path("name").asText(),
                    metric.path("object").asText(),
                    metric.path("expr").asText(),
                    metric.path("unit").asText(""),
                    metric.path("format").asText(""),
                    metric.path("caliber").asText()));
        }
        return List.copyOf(parsed);
    }

    private static List<OntologySignal> parseSignals(JsonNode signals) {
        if (!signals.isArray()) {
            return List.of();
        }
        List<OntologySignal> parsed = new ArrayList<>();
        for (JsonNode signal : signals) {
            if (missingAny(signal, "name", "object", "severity", "when", "advice")) {
                warnInvalidOntologyEntry("signals", signal);
                continue;
            }
            parsed.add(new OntologySignal(
                    signal.path("name").asText(),
                    signal.path("object").asText(),
                    signal.path("severity").asText(),
                    signal.path("when").asText(),
                    signal.path("advice").asText(),
                    parseTextArray(signal.path("linkedActions"))));
        }
        return List.copyOf(parsed);
    }

    private static List<OntologyAction> parseActions(JsonNode actions) {
        if (!actions.isArray()) {
            return List.of();
        }
        List<OntologyAction> parsed = new ArrayList<>();
        for (JsonNode action : actions) {
            boolean referenced = action.path("target").isObject();
            JsonNode endpoint = action.path(referenced ? "target" : "endpoint");
            String service = endpoint.path(referenced ? "serviceRef" : "service").asText();
            String draft = referenced ? endpoint.path("draft").path("path").asText() : endpoint.path("draft").asText();
            String commit = referenced ? endpoint.path("commit").path("path").asText() : endpoint.path("commit").asText();
            if (missingAny(action, "name", "object", "intent") || service.isBlank() || draft.isBlank() || commit.isBlank()
                    || !action.path("params").isArray()
                    || (!referenced && (missingAny(action, "approval", "guard") || !action.has("audit")))) {
                warnInvalidOntologyEntry("actions", action);
                continue;
            }
            parsed.add(new OntologyAction(
                    action.path("name").asText(), action.path("object").asText(), action.path("intent").asText(),
                    new OntologyActionEndpoint(service, draft, commit,
                            referenced ? endpoint.path("draft").path("method").asText() : "POST",
                            referenced ? endpoint.path("commit").path("method").asText() : "POST"),
                    parseActionParams(action.path("params")),
                    referenced ? "human" : action.path("approval").asText(),
                    referenced || action.path("audit").asBoolean(false),
                    referenced ? action.path("approval").path("requiredRole").asText() : action.path("guard").asText()));
        }
        return List.copyOf(parsed);
    }

    private static List<OntologyActionParam> parseActionParams(JsonNode params) {
        if (!params.isArray()) {
            return List.of();
        }
        List<OntologyActionParam> parsed = new ArrayList<>();
        for (JsonNode param : params) {
            if (missingAny(param, "name", "source") || !param.has("required")) {
                warnInvalidOntologyEntry("actions.params", param);
                continue;
            }
            parsed.add(new OntologyActionParam(
                    param.path("name").asText(),
                    param.path("source").asText(),
                    param.path("required").asBoolean(false)));
        }
        return List.copyOf(parsed);
    }

    private static boolean missingAny(JsonNode node, String... fields) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return true;
        }
        for (String field : fields) {
            if (!node.hasNonNull(field) || node.path(field).asText("").isBlank()) {
                return true;
            }
        }
        return false;
    }

    private static List<String> parseTextArray(JsonNode arrayNode) {
        if (!arrayNode.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (JsonNode item : arrayNode) {
            String value = item.asText("");
            if (!value.isBlank()) {
                values.add(value);
            }
        }
        return List.copyOf(values);
    }

    private static List<String> resolveGuardrails(JsonNode pack) {
        List<String> guardrails = new ArrayList<>();
        Set<String> seenRuleIds = new java.util.LinkedHashSet<>();
        appendGuardrails(guardrails, seenRuleIds, pack.path("generatedGuardrails").path("rules"));
        appendGuardrails(guardrails, seenRuleIds, pack.path("guardrails"));
        return List.copyOf(guardrails);
    }

    private static void appendGuardrails(List<String> guardrails, Set<String> seenRuleIds, JsonNode arrayNode) {
        if (!arrayNode.isArray()) {
            return;
        }
        for (JsonNode item : arrayNode) {
            String value = item.asText("");
            if (value.isBlank()) {
                continue;
            }
            String ruleId = extractRuleId(value);
            if (!ruleId.isBlank() && !seenRuleIds.add(ruleId)) {
                continue;
            }
            guardrails.add(value);
        }
    }

    private static String extractRuleId(String value) {
        if (!value.startsWith("[CAL-")) {
            return "";
        }
        int end = value.indexOf(']');
        if (end <= 1) {
            return "";
        }
        return value.substring(1, end);
    }

    private static void warnInvalidOntologyEntry(String section, JsonNode node) {
        log.warn("Skipping invalid semantic-pack ontology entry in {}: {}", section, node);
    }

    private void appendArray(StringBuilder sb, JsonNode arrayNode) {
        for (int i = 0; i < arrayNode.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(arrayNode.get(i).asText(""));
        }
    }

    public record SemanticPack(
            String domain,
            String description,
            List<SemanticObject> objects,
            Map<String, String> synonyms,
            List<FewShot> fewShots,
            List<String> guardrails,
            List<OntologyLink> links,
            List<OntologyMetric> metrics,
            List<OntologySignal> signals,
            List<OntologyAction> actions) {
    }

    public record SemanticObject(
            String name,
            String view,
            String description,
            List<String> keyDimensions,
            List<String> keyMeasures,
            List<String> commonFilters,
            String defaultTimeField) {
    }

    public record FewShot(String question, String sql) {
    }

    public record OntologyLink(
            String name,
            String from,
            String to,
            String fromKey,
            String toKey,
            String cardinality,
            String joinHint,
            String note) {
    }

    public record OntologyMetric(
            String name,
            String object,
            String expr,
            String unit,
            String format,
            String caliber) {
    }

    public record OntologySignal(
            String name,
            String object,
            String severity,
            String when,
            String advice,
            List<String> linkedActions) {
    }

    public record OntologyAction(
            String name,
            String object,
            String intent,
            OntologyActionEndpoint endpoint,
            List<OntologyActionParam> params,
            String approval,
            boolean audit,
            String guard) {
    }

    public record OntologyActionEndpoint(String service, String draft, String commit, String draftMethod, String commitMethod) {
        public OntologyActionEndpoint(String service, String draft, String commit) {
            this(service, draft, commit, "POST", "POST");
        }
    }

    public record OntologyActionParam(String name, String source, boolean required) {
    }
}
