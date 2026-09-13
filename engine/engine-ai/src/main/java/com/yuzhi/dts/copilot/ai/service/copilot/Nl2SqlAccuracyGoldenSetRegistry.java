package com.yuzhi.dts.copilot.ai.service.copilot;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class Nl2SqlAccuracyGoldenSetRegistry {

    private static final Logger log = LoggerFactory.getLogger(Nl2SqlAccuracyGoldenSetRegistry.class);
    private static final String REGISTRY_RESOURCE = "governance/nl2sql-accuracy-golden-set.v1.json";

    private final ObjectMapper objectMapper;
    private Map<String, GoldenCase> cases = Map.of();

    public Nl2SqlAccuracyGoldenSetRegistry(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(REGISTRY_RESOURCE)) {
            if (is == null) {
                log.warn("NL2SQL accuracy golden-set resource not found: {}", REGISTRY_RESOURCE);
                this.cases = Map.of();
                return;
            }
            GoldenSetDocument document = objectMapper.readValue(is, GoldenSetDocument.class);
            Map<String, GoldenCase> loaded = new LinkedHashMap<>();
            for (GoldenCase item : document.cases()) {
                if (!StringUtils.hasText(item.id()) || !item.enabled()) {
                    continue;
                }
                if (loaded.putIfAbsent(item.id(), item) != null) {
                    throw new IllegalStateException("duplicate golden case id: " + item.id());
                }
            }
            this.cases = Map.copyOf(loaded);
            log.info("Loaded {} NL2SQL accuracy golden case(s) from {}", cases.size(), REGISTRY_RESOURCE);
        } catch (Exception e) {
            log.warn("Failed to load NL2SQL accuracy golden-set from {}: {}", REGISTRY_RESOURCE, e.getMessage());
            this.cases = Map.of();
        }
    }

    public List<GoldenCase> cases() {
        return new ArrayList<>(cases.values());
    }

    public Optional<GoldenCase> caseById(String id) {
        return Optional.ofNullable(cases.get(id));
    }

    private static <T> List<T> copyOrEmpty(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private static Map<String, String> copyTextMapOrEmpty(Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        Map<String, String> copy = new LinkedHashMap<>();
        values.forEach((key, value) -> {
            if (StringUtils.hasText(key) && StringUtils.hasText(value)) {
                copy.put(key.trim(), value.trim());
            }
        });
        return copy.isEmpty() ? Map.of() : Map.copyOf(copy);
    }

    private static String textOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private record GoldenSetDocument(String version, List<GoldenCase> cases) {
        private GoldenSetDocument {
            cases = copyOrEmpty(cases);
        }
    }

    public record GoldenCase(
            String id,
            String question,
            String domain,
            String expectedTemplate,
            String expectedTarget,
            String expectedDataSurface,
            String expectedGradeAtLeast,
            List<String> expectedSqlFragments,
            List<String> forbiddenSqlFragments,
            List<String> requiredEvidence,
            Map<String, String> expectedParameters,
            String metamorphicGroup,
            List<String> tags,
            boolean enabled) {
        public GoldenCase {
            id = textOrEmpty(id);
            question = textOrEmpty(question);
            domain = textOrEmpty(domain);
            expectedTemplate = textOrEmpty(expectedTemplate);
            expectedTarget = textOrEmpty(expectedTarget);
            expectedDataSurface = textOrEmpty(expectedDataSurface);
            expectedGradeAtLeast = textOrEmpty(expectedGradeAtLeast);
            expectedSqlFragments = copyOrEmpty(expectedSqlFragments);
            forbiddenSqlFragments = copyOrEmpty(forbiddenSqlFragments);
            requiredEvidence = copyOrEmpty(requiredEvidence);
            expectedParameters = copyTextMapOrEmpty(expectedParameters);
            metamorphicGroup = textOrEmpty(metamorphicGroup);
            tags = copyOrEmpty(tags);
        }
    }
}
