package com.yuzhi.dts.copilot.ai.service.copilot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class BusinessDirectResponseCatalogService {

    private static final Logger log = LoggerFactory.getLogger(BusinessDirectResponseCatalogService.class);
    private static final String RESOURCE_PATH = "planner/business-direct-responses.json";

    private final ObjectMapper objectMapper;
    private List<CatalogEntry> entries = List.of();

    public BusinessDirectResponseCatalogService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(RESOURCE_PATH)) {
            if (is == null) {
                log.warn("Business direct response catalog not found: {}", RESOURCE_PATH);
                entries = List.of();
                return;
            }
            JsonNode root = objectMapper.readTree(new String(is.readAllBytes(), StandardCharsets.UTF_8));
            JsonNode items = root.path("entries");
            if (!items.isArray()) {
                entries = List.of();
                return;
            }
            List<CatalogEntry> loaded = new ArrayList<>();
            for (JsonNode item : items) {
                String code = item.path("code").asText("");
                String responseType = item.path("responseType").asText("");
                List<Pattern> patterns = new ArrayList<>();
                JsonNode intentPatterns = item.path("intentPatterns");
                if (intentPatterns.isArray()) {
                    for (JsonNode patternNode : intentPatterns) {
                        String regex = patternNode.asText("");
                        if (StringUtils.hasText(regex)) {
                            patterns.add(Pattern.compile(regex, Pattern.CASE_INSENSITIVE));
                        }
                    }
                }
                if (StringUtils.hasText(code) && StringUtils.hasText(responseType) && !patterns.isEmpty()) {
                    loaded.add(new CatalogEntry(code, responseType, List.copyOf(patterns)));
                }
            }
            entries = List.copyOf(loaded);
        } catch (Exception e) {
            log.warn("Failed to load business direct response catalog: {}", e.getMessage());
            entries = List.of();
        }
    }

    public Optional<CatalogEntry> findMatch(String userQuestion) {
        if (!StringUtils.hasText(userQuestion)) {
            return Optional.empty();
        }
        String normalized = userQuestion.trim();
        return entries.stream()
                .filter(entry -> entry.patterns().stream().anyMatch(pattern -> pattern.matcher(normalized).find()))
                .findFirst();
    }

    public List<CatalogEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    public record CatalogEntry(String code, String responseType, List<Pattern> patterns) {}
}
