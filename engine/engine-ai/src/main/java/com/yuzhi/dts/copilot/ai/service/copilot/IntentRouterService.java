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
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

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
            "项目", "项目点", "合同", "任务", "收入", "费用", "正常", "停用", "执行", "维护");

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
        boolean singleDomainMatched = scores.size() == 1;
        boolean singleGenericHit = top.matchedCount() == 1
                && top.matchedKeywords().stream().allMatch(GENERIC_KEYWORDS::contains)
                && top.score() < HIGH_CONFIDENCE_THRESHOLD;
        boolean hasNonGenericSignal = scores.stream()
                .flatMap(score -> score.matchedKeywords().stream())
                .anyMatch(keyword -> !GENERIC_KEYWORDS.contains(keyword));

        // Special rule: if settlement domain matched, force settlement isolation
        if ("settlement".equals(top.rule().getDomain())
                && (!singleGenericHit || singleDomainMatched || hasNonGenericSignal)) {
            return new RoutingResult(
                    "settlement",
                    "v_monthly_settlement",
                    Collections.emptyList(),
                    top.score(),
                    false  // settlement match is always actionable
            );
        }

        // Determine effective confidence based on multiple signals
        boolean clearWinner = scores.size() > 1
                && top.matchedCount() > scores.get(1).matchedCount();
        boolean lowSignal = top.matchedCount() == 1 && top.score() < MEDIUM_CONFIDENCE_THRESHOLD;
        if ((singleGenericHit && singleDomainMatched)
                || ((singleGenericHit || lowSignal) && !singleDomainMatched && !clearWinner && !hasNonGenericSignal)) {
            return new RoutingResult(
                    top.rule().getDomain(),
                    top.rule().getPrimaryView(),
                    parseJsonArray(top.rule().getSecondaryViews()),
                    top.score(),
                    true
            );
        }

        // High confidence: only one domain matched, or clear winner with 2+ hits
        if (singleDomainMatched || (clearWinner && top.score() >= MEDIUM_CONFIDENCE_THRESHOLD)) {
            return new RoutingResult(
                    top.rule().getDomain(),
                    top.rule().getPrimaryView(),
                    parseJsonArray(top.rule().getSecondaryViews()),
                    top.score(),
                    false
            );
        }

        // Medium confidence: single keyword hit on one domain (still actionable)
        if (scores.size() > 1 && hasNonGenericSignal) {
            List<String> combinedSecondary = new ArrayList<>(parseJsonArray(top.rule().getSecondaryViews()));
            for (int i = 1; i < scores.size(); i++) {
                String view = scores.get(i).rule().getPrimaryView();
                if (view != null && !combinedSecondary.contains(view)) {
                    combinedSecondary.add(view);
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

        if (clearWinner || top.matchedCount() >= 1) {
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

    // =====================================================================
    // EL-05: DataLayer routing extension
    // =====================================================================

    public enum DataLayer { VIEW, MART }

    public record ExtendedRoutingResult(
            RoutingResult baseResult,
            DataLayer dataLayer,
            String martTable,
            boolean fallbackApplied,
            String fallbackReason
    ) {}

    private static final Set<String> MART_KEYWORDS = Set.of(
            "趋势", "变化", "对比", "环比", "同比",
            "近3月", "近半年", "近一年", "最近几个月",
            "月度", "每月", "各月", "按月",
            "增长", "下降", "波动", "走势"
    );

    private static final Map<String, String> DOMAIN_TO_MART = Map.of(
            "project", "mart_project_fulfillment_daily",
            "flowerbiz", "fact_field_operation_event",
            "green", "mart_project_fulfillment_daily",
            "settlement", "mart_project_fulfillment_daily",
            "task", "mart_project_fulfillment_daily",
            "curing", "mart_project_fulfillment_daily"
    );

    public ExtendedRoutingResult routeWithDataLayer(String userQuestion) {
        return routeWithDataLayer(userQuestion, martTable -> true);
    }

    public ExtendedRoutingResult routeWithDataLayer(String userQuestion, Map<String, Boolean> martHealthSnapshot) {
        return routeWithDataLayer(userQuestion,
                martTable -> martHealthSnapshot != null && Boolean.TRUE.equals(martHealthSnapshot.get(martTable)));
    }

    public ExtendedRoutingResult routeWithDataLayer(String userQuestion, Predicate<String> martAvailability) {
        RoutingResult base = route(userQuestion);
        if (base.needsClarification() || base.domain() == null) {
            return new ExtendedRoutingResult(base, DataLayer.VIEW, null, false, null);
        }
        boolean hasMartKeyword = userQuestion != null && MART_KEYWORDS.stream().anyMatch(userQuestion::contains);
        if (hasMartKeyword) {
            String martTable = DOMAIN_TO_MART.get(base.domain());
            if (martTable != null) {
                if (martAvailability.test(martTable)) {
                    return new ExtendedRoutingResult(base, DataLayer.MART, martTable, false, null);
                }
                return new ExtendedRoutingResult(
                        base,
                        DataLayer.VIEW,
                        null,
                        true,
                        "mart unavailable: " + martTable);
            }
        }
        return new ExtendedRoutingResult(base, DataLayer.VIEW, null, false, null);
    }
}
