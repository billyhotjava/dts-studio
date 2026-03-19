package com.yuzhi.dts.copilot.ai.service.copilot;

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
import java.util.Set;

/**
 * Loads and caches business semantic packs from classpath resources.
 * Semantic packs provide domain-specific context (object dictionaries, synonyms,
 * few-shot examples) for NL2SQL prompt injection.
 */
@Service
public class SemanticPackService {

    private static final Logger log = LoggerFactory.getLogger(SemanticPackService.class);

    private static final String[] PACK_FILES = {
            "semantic-packs/project-fulfillment.json",
            "semantic-packs/field-operations.json"
    };

    private final ObjectMapper objectMapper;
    private Map<String, JsonNode> packs;

    public SemanticPackService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        Map<String, JsonNode> loaded = new LinkedHashMap<>();
        for (String file : PACK_FILES) {
            try (InputStream is = getClass().getClassLoader().getResourceAsStream(file)) {
                if (is == null) {
                    log.warn("Semantic pack resource not found: {}", file);
                    continue;
                }
                byte[] bytes = is.readAllBytes();
                JsonNode node = objectMapper.readTree(new String(bytes, StandardCharsets.UTF_8));
                String domain = node.path("domain").asText(null);
                if (domain == null || domain.isBlank()) {
                    log.warn("Semantic pack {} has no 'domain' field, skipping", file);
                    continue;
                }
                loaded.put(domain, node);
                log.info("Loaded semantic pack: domain={}, file={}", domain, file);
            } catch (Exception e) {
                log.warn("Failed to load semantic pack {}: {}", file, e.getMessage());
            }
        }
        this.packs = Collections.unmodifiableMap(loaded);
        log.info("Semantic packs initialized: {} domain(s) loaded", packs.size());
    }

    /**
     * Get few-shot examples for a domain, formatted as "Q: ... A: ..." strings.
     *
     * @param domain the domain key, e.g. "project" or "flowerbiz"
     * @return list of formatted few-shot strings, empty list if domain not found
     */
    public List<String> getFewShots(String domain) {
        JsonNode pack = packs.get(domain);
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
    public Map<String, String> getSynonyms(String domain) {
        JsonNode pack = packs.get(domain);
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
    public String getContextForDomain(String domain) {
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
        Map<String, String> synonyms = getSynonyms(domain);
        if (!synonyms.isEmpty()) {
            sb.append("【同义词/术语映射】\n");
            for (Map.Entry<String, String> entry : synonyms.entrySet()) {
                sb.append("- ").append(entry.getKey()).append(" -> ").append(entry.getValue()).append("\n");
            }
            sb.append("\n");
        }

        // Few-shot examples
        List<String> fewShots = getFewShots(domain);
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
    public Set<String> getDomains() {
        return packs.keySet();
    }

    private void appendArray(StringBuilder sb, JsonNode arrayNode) {
        for (int i = 0; i < arrayNode.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(arrayNode.get(i).asText(""));
        }
    }
}
