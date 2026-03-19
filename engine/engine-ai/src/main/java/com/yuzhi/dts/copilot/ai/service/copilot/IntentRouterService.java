package com.yuzhi.dts.copilot.ai.service.copilot;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuzhi.dts.copilot.ai.domain.Nl2SqlRoutingRule;
import com.yuzhi.dts.copilot.ai.repository.Nl2SqlRoutingRuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Intent routing engine for NL2SQL.
 * Matches user questions against keyword-based rules to determine
 * which database views should be used for SQL generation.
 */
@Service
public class IntentRouterService {

    private static final Logger log = LoggerFactory.getLogger(IntentRouterService.class);

    private static final double HIGH_CONFIDENCE_THRESHOLD = 0.3;
    private static final double MEDIUM_CONFIDENCE_THRESHOLD = 0.15;
    private static final Set<String> GENERIC_KEYWORDS = Set.of(
            "项目", "项目点", "客户", "合同", "任务", "收入", "费用", "正常", "停用", "执行", "维护");

    private final Nl2SqlRoutingRuleRepository routingRuleRepository;
    private final ObjectMapper objectMapper;

    /** Simple field cache for active rules. */
    private volatile List<Nl2SqlRoutingRule> cachedRules;
    private volatile long cacheTimestamp;
    private static final long CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutes

    public IntentRouterService(Nl2SqlRoutingRuleRepository routingRuleRepository,
                               ObjectMapper objectMapper) {
        this.routingRuleRepository = routingRuleRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Routing result containing the matched domain, views, and confidence.
     */
    public record RoutingResult(
            String domain,
            String primaryView,
            List<String> secondaryViews,
            double confidence,
            boolean needsClarification
    ) {}

    /**
     * Route a user question to the appropriate domain and views.
     *
     * @param userQuestion the natural language question from the user
     * @return routing result with matched domain, views, and confidence
     */
    public RoutingResult route(String userQuestion) {
        if (userQuestion == null || userQuestion.isBlank()) {
            return new RoutingResult(null, null, Collections.emptyList(), 0.0, true);
        }

        List<Nl2SqlRoutingRule> rules = loadActiveRules();
        if (rules.isEmpty()) {
            log.warn("No active routing rules found");
            return new RoutingResult(null, null, Collections.emptyList(), 0.0, true);
        }

        // Score each domain
        List<DomainScore> scores = new ArrayList<>();
        for (Nl2SqlRoutingRule rule : rules) {
            List<String> keywords = parseJsonArray(rule.getKeywords());
            int matchedCount = 0;
            List<String> matchedKeywords = new ArrayList<>();
            for (String keyword : keywords) {
                if (userQuestion.contains(keyword)) {
                    matchedCount++;
                    matchedKeywords.add(keyword);
                }
            }
            if (matchedCount > 0) {
                double score = (double) matchedCount / keywords.size();
                scores.add(new DomainScore(rule, score, matchedCount, matchedKeywords));
            }
        }

        if (scores.isEmpty()) {
            log.debug("No keyword matches found for question: {}", userQuestion);
            return new RoutingResult(null, null, Collections.emptyList(), 0.0, true);
        }

        // Sort by matched count descending (absolute hits matter more than ratio)
        scores.sort(Comparator.comparingInt(DomainScore::matchedCount).reversed()
                .thenComparing(Comparator.comparingDouble(DomainScore::score).reversed()));

        DomainScore top = scores.get(0);
        boolean singleGenericHit = top.matchedCount() == 1
                && top.matchedKeywords().stream().allMatch(GENERIC_KEYWORDS::contains)
                && top.score() < HIGH_CONFIDENCE_THRESHOLD;

        // Special rule: if settlement domain matched, force settlement isolation
        if ("settlement".equals(top.rule().getDomain()) && !singleGenericHit) {
            return new RoutingResult(
                    "settlement",
                    "v_monthly_settlement",
                    Collections.emptyList(),
                    top.score(),
                    false  // settlement match is always actionable
            );
        }

        // Determine effective confidence based on multiple signals
        boolean singleDomainMatched = scores.size() == 1;
        boolean clearWinner = scores.size() > 1
                && top.matchedCount() > scores.get(1).matchedCount();
        boolean lowSignal = top.matchedCount() == 1 && top.score() < MEDIUM_CONFIDENCE_THRESHOLD;
        if (singleGenericHit || (lowSignal && !singleDomainMatched && !clearWinner)) {
            return new RoutingResult(
                    top.rule().getDomain(),
                    top.rule().getPrimaryView(),
                    parseJsonArray(top.rule().getSecondaryViews()),
                    top.score(),
                    true
            );
        }

        // High confidence: only one domain matched, or clear winner with 2+ hits
        if ((singleDomainMatched && !singleGenericHit) || (clearWinner && top.score() >= MEDIUM_CONFIDENCE_THRESHOLD)) {
            return new RoutingResult(
                    top.rule().getDomain(),
                    top.rule().getPrimaryView(),
                    parseJsonArray(top.rule().getSecondaryViews()),
                    top.score(),
                    false
            );
        }

        // Medium confidence: single keyword hit on one domain (still actionable)
        if ((clearWinner || top.matchedCount() >= 1) && !singleGenericHit) {
            List<String> combinedSecondary = new ArrayList<>(parseJsonArray(top.rule().getSecondaryViews()));
            if (scores.size() > 1) {
                DomainScore second = scores.get(1);
                if (top.matchedCount() == second.matchedCount()) {
                    combinedSecondary.add(second.rule().getPrimaryView());
                }
            }
            return new RoutingResult(
                    top.rule().getDomain(),
                    top.rule().getPrimaryView(),
                    combinedSecondary,
                    top.score(),
                    false
            );
        }

        // Low confidence (safety net)
        return new RoutingResult(
                top.rule().getDomain(),
                top.rule().getPrimaryView(),
                parseJsonArray(top.rule().getSecondaryViews()),
                top.score(),
                true
        );
    }

    /**
     * Generate a clarification message for ambiguous questions.
     */
    public String generateClarificationMessage() {
        return "您的问题可能涉及以下方面，请确认：\n" +
                "1. 项目和客户信息\n" +
                "2. 报花业务（加花/换花/减花）\n" +
                "3. 租金和结算\n" +
                "4. 任务进度\n" +
                "5. 养护情况\n" +
                "6. 初摆进度";
    }

    private List<Nl2SqlRoutingRule> loadActiveRules() {
        long now = System.currentTimeMillis();
        if (cachedRules == null || (now - cacheTimestamp) > CACHE_TTL_MS) {
            cachedRules = routingRuleRepository.findByIsActiveTrueOrderByPriorityDesc();
            cacheTimestamp = now;
            log.debug("Refreshed routing rule cache, loaded {} rules", cachedRules.size());
        }
        return cachedRules;
    }

    private List<String> parseJsonArray(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse JSON array: {}", json, e);
            return Collections.emptyList();
        }
    }

    private record DomainScore(Nl2SqlRoutingRule rule, double score, int matchedCount, List<String> matchedKeywords) {}
}
