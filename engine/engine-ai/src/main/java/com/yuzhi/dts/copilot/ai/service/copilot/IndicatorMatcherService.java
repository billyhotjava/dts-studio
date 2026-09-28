package com.yuzhi.dts.copilot.ai.service.copilot;

import com.yuzhi.dts.copilot.ai.service.platform.IndicatorCatalogEntry;
import com.yuzhi.dts.copilot.ai.service.platform.IndicatorCatalogStore;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class IndicatorMatcherService {

    public static final double HIGH_CONFIDENCE_THRESHOLD = 0.80d;
    public static final double MEDIUM_CONFIDENCE_THRESHOLD = 0.45d;

    private final IndicatorCatalogStore catalogStore;
    private final SemanticPackService semanticPackService;
    private final IndicatorRoutingMetricsService metricsService;

    @Autowired
    public IndicatorMatcherService(
            IndicatorCatalogStore catalogStore,
            SemanticPackService semanticPackService,
            IndicatorRoutingMetricsService metricsService) {
        this.catalogStore = catalogStore;
        this.semanticPackService = semanticPackService;
        this.metricsService = metricsService == null
                ? IndicatorRoutingMetricsService.noop()
                : metricsService;
    }

    public IndicatorMatcherService(
            IndicatorCatalogStore catalogStore,
            SemanticPackService semanticPackService) {
        this(catalogStore, semanticPackService, IndicatorRoutingMetricsService.noop());
    }

    public IndicatorMatchResult match(String userQuestion) {
        if (!StringUtils.hasText(userQuestion) || !catalogStore.isReady()) {
            IndicatorMatchResult result = IndicatorMatchResult.none();
            metricsService.recordMatch(result.tier(), result.candidates().size());
            return result;
        }
        String normalizedQuestion = normalize(userQuestion);
        List<ScoredMatch> scored = new ArrayList<>();
        for (IndicatorCatalogEntry entry : catalogStore.all()) {
            if (!isPublished(entry)) {
                continue;
            }
            Score score = score(entry, normalizedQuestion);
            if (score.value() > 0) {
                scored.add(new ScoredMatch(toMatch(entry, score), score.value()));
            }
        }
        List<IndicatorMatch> candidates = scored.stream()
                .sorted(Comparator.comparingInt(ScoredMatch::score).reversed()
                        .thenComparing(match -> match.match().name()))
                .map(ScoredMatch::match)
                .toList();
        IndicatorMatchResult result = new IndicatorMatchResult(candidates, resolveTier(candidates));
        metricsService.recordMatch(result.tier(), result.candidates().size());
        return result;
    }

    private Score score(IndicatorCatalogEntry entry, String normalizedQuestion) {
        LinkedHashSet<String> signals = new LinkedHashSet<>();
        String expandedQuestion = expandWithSynonyms(entry, normalizedQuestion, signals);
        int score = 0;

        if (contains(expandedQuestion, entry.name())) {
            score += 6;
            signals.add("name:" + entry.name());
        }
        if (contains(expandedQuestion, entry.code())) {
            score += 6;
            signals.add("code:" + entry.code());
        }
        for (String tag : entry.tags()) {
            if (contains(expandedQuestion, tag)) {
                score += 3;
                signals.add("tag:" + tag);
            }
        }
        for (String dimension : entry.dimensionFields()) {
            if (contains(expandedQuestion, dimension)) {
                score += 2;
                signals.add("dimension:" + dimension);
            }
        }
        if (contains(expandedQuestion, entry.category())) {
            score += 1;
            signals.add("category:" + entry.category());
        }
        if (contains(expandedQuestion, entry.domain())) {
            score += 1;
            signals.add("domain:" + entry.domain());
        }
        if (containsAnyKeyword(expandedQuestion, entry.definition())) {
            score += 1;
            signals.add("definition");
        }
        return new Score(score, List.copyOf(signals));
    }

    private String expandWithSynonyms(
            IndicatorCatalogEntry entry,
            String normalizedQuestion,
            LinkedHashSet<String> signals) {
        if (!StringUtils.hasText(entry.domain())) {
            return normalizedQuestion;
        }
        Map<String, String> synonyms = semanticPackService.getSynonyms(entry.domain());
        if (synonyms.isEmpty()) {
            return normalizedQuestion;
        }
        StringBuilder expanded = new StringBuilder(normalizedQuestion);
        synonyms.forEach((term, mapped) -> {
            if (contains(normalizedQuestion, term) && StringUtils.hasText(mapped)) {
                expanded.append(' ').append(normalize(mapped));
                signals.add("synonym:" + term + "->" + mapped);
            }
        });
        return expanded.toString();
    }

    private IndicatorMatch toMatch(IndicatorCatalogEntry entry, Score score) {
        boolean strong = score.signals().stream()
                .anyMatch(signal -> signal.startsWith("name:") || signal.startsWith("code:"));
        double confidence = Math.min(0.96d, score.value() / 10.0d + (strong ? 0.25d : 0.05d));
        return new IndicatorMatch(
                entry.id(),
                entry.code(),
                entry.name(),
                entry.category(),
                entry.domain(),
                entry.definition(),
                entry.expressionSql(),
                entry.version(),
                confidence,
                score.signals());
    }

    private Confidence resolveTier(List<IndicatorMatch> candidates) {
        if (candidates.isEmpty()) {
            return Confidence.NONE;
        }
        if (candidates.size() == 1 && candidates.getFirst().confidence() >= HIGH_CONFIDENCE_THRESHOLD) {
            return Confidence.HIGH;
        }
        if (candidates.getFirst().confidence() >= MEDIUM_CONFIDENCE_THRESHOLD) {
            return Confidence.MEDIUM;
        }
        return Confidence.LOW;
    }

    private static boolean isPublished(IndicatorCatalogEntry entry) {
        String status = normalize(entry.status());
        return !StringUtils.hasText(status) || "已发布".equals(status) || "published".equals(status);
    }

    private static boolean contains(String normalizedText, String candidate) {
        String normalizedCandidate = normalize(candidate);
        return StringUtils.hasText(normalizedText)
                && StringUtils.hasText(normalizedCandidate)
                && normalizedText.contains(normalizedCandidate);
    }

    private static boolean containsAnyKeyword(String normalizedQuestion, String text) {
        if (!StringUtils.hasText(normalizedQuestion) || !StringUtils.hasText(text)) {
            return false;
        }
        for (String token : normalize(text).split("[\\s,;；，、]+")) {
            if (token.length() >= 2 && normalizedQuestion.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private record Score(int value, List<String> signals) {
    }

    private record ScoredMatch(IndicatorMatch match, int score) {
    }

    public enum Confidence {
        HIGH,
        MEDIUM,
        LOW,
        NONE
    }

    public record IndicatorMatch(
            String id,
            String code,
            String name,
            String category,
            String domain,
            String definition,
            String expressionSql,
            String version,
            double confidence,
            List<String> matchedSignals
    ) {
        public IndicatorMatch {
            matchedSignals = matchedSignals == null ? List.of() : List.copyOf(matchedSignals);
        }
    }

    public record IndicatorMatchResult(
            List<IndicatorMatch> candidates,
            Confidence tier
    ) {
        public IndicatorMatchResult {
            candidates = candidates == null ? List.of() : List.copyOf(candidates);
            tier = tier == null ? Confidence.NONE : tier;
        }

        public static IndicatorMatchResult none() {
            return new IndicatorMatchResult(List.of(), Confidence.NONE);
        }
    }
}
