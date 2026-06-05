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

@Service
public class FinanceAmountColumnAlignmentRegistry {

    private static final Logger log = LoggerFactory.getLogger(FinanceAmountColumnAlignmentRegistry.class);
    private static final String REGISTRY_RESOURCE = "governance/finance-amount-column-alignment.v1.json";

    private final ObjectMapper objectMapper;
    private Map<String, AmountTier> tiers = Map.of();

    public FinanceAmountColumnAlignmentRegistry(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(REGISTRY_RESOURCE)) {
            if (is == null) {
                log.warn("Finance amount column alignment resource not found: {}", REGISTRY_RESOURCE);
                this.tiers = Map.of();
                return;
            }
            AmountColumnDocument document = objectMapper.readValue(is, AmountColumnDocument.class);
            Map<String, AmountTier> loaded = new LinkedHashMap<>();
            for (AmountTier tier : document.tiers()) {
                loaded.put(tier.id(), tier);
            }
            this.tiers = java.util.Collections.unmodifiableMap(loaded);
            log.info("Loaded {} finance amount column tier(s) from {}", tiers.size(), REGISTRY_RESOURCE);
        } catch (Exception e) {
            log.warn("Failed to load finance amount column alignment from {}: {}", REGISTRY_RESOURCE, e.getMessage());
            this.tiers = Map.of();
        }
    }

    public List<AmountTier> tiers() {
        return new ArrayList<>(tiers.values());
    }

    public Optional<AmountTier> tier(String id) {
        return Optional.ofNullable(tiers.get(id));
    }

    public List<AmountTier> tiersForTerm(String term) {
        String normalizedTerm = normalize(term);
        if (normalizedTerm.isEmpty()) {
            return List.of();
        }
        return tiers.values().stream()
                .filter(tier -> tier.matchesTerm(normalizedTerm))
                .toList();
    }

    static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static <T> List<T> copyOrEmpty(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private static String textOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private record AmountColumnDocument(String version, List<AmountTier> tiers) {
        private AmountColumnDocument {
            tiers = copyOrEmpty(tiers);
        }
    }

    public record AmountTier(
            String id,
            String semanticName,
            String sourceColumn,
            String apiField,
            String martField,
            List<String> labels,
            List<String> ambiguousTerms,
            String notes) {

        public AmountTier {
            id = textOrEmpty(id);
            semanticName = textOrEmpty(semanticName);
            sourceColumn = textOrEmpty(sourceColumn);
            apiField = textOrEmpty(apiField);
            martField = textOrEmpty(martField);
            labels = copyOrEmpty(labels);
            ambiguousTerms = copyOrEmpty(ambiguousTerms);
            notes = textOrEmpty(notes);
        }

        public boolean acceptsField(String selectedField) {
            String normalizedField = normalize(selectedField);
            return normalizedField.equals(normalize(sourceColumn))
                    || normalizedField.equals(normalize(apiField))
                    || normalizedField.equals(normalize(martField));
        }

        private boolean matchesTerm(String normalizedTerm) {
            if (normalize(semanticName).equals(normalizedTerm)) {
                return true;
            }
            return labels.stream().anyMatch(label -> normalize(label).equals(normalizedTerm))
                    || ambiguousTerms.stream().anyMatch(term -> normalize(term).equals(normalizedTerm));
        }
    }
}
