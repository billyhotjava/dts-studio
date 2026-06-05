package com.yuzhi.dts.copilot.ai.service.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yuzhi.dts.copilot.ai.domain.AiChatMessage;
import com.yuzhi.dts.copilot.ai.repository.AiChatMessageRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RouteTelemetryService {

    private static final int MAX_QUESTION_LENGTH = 300;
    private static final int MAX_SAMPLE_QUESTIONS = 3;

    private final AiChatMessageRepository messageRepository;
    private final ObjectMapper objectMapper;

    public RouteTelemetryService(AiChatMessageRepository messageRepository, ObjectMapper objectMapper) {
        this.messageRepository = messageRepository;
        this.objectMapper = objectMapper;
    }

    public void attachQuestion(AiChatMessage message, String userQuestion) {
        if (message == null || !StringUtils.hasText(userQuestion)) {
            return;
        }
        ObjectNode trace = parseTraceObject(message.getTrace());
        RouteFinalStep finalStep = resolveFinalStep(trace);
        ObjectNode telemetry = trace.withObject("/telemetry");
        telemetry.put("question", truncate(userQuestion.trim(), MAX_QUESTION_LENGTH));
        if (StringUtils.hasText(finalStep.tier())) {
            telemetry.put("finalTier", finalStep.tier());
            telemetry.put("weakPath", isWeakPath(finalStep.tier()));
        }
        if (StringUtils.hasText(finalStep.target())) {
            telemetry.put("finalTarget", finalStep.target());
        }
        putIfPresent(telemetry, "domain", message.getRoutedDomain());
        putIfPresent(telemetry, "responseKind", message.getResponseKind());
        putIfPresent(telemetry, "target", message.getTargetView());
        putIfPresent(telemetry, "dataSurface", message.getDataSurface());
        try {
            message.setTrace(objectMapper.writeValueAsString(trace));
        } catch (Exception ignored) {
            // Keep the original message persisted even if non-critical telemetry serialization fails.
        }
    }

    public RouteTelemetrySummary summarize(int days, int candidateLimit) {
        int safeDays = days <= 0 ? 7 : days;
        int safeLimit = candidateLimit <= 0 ? 10 : candidateLimit;
        Instant since = Instant.now().minus(safeDays, ChronoUnit.DAYS);
        List<AiChatMessage> messages =
                messageRepository.findByRoleAndTraceIsNotNullAndCreatedAtAfterOrderByCreatedAtDesc(
                        "assistant",
                        since);

        Map<String, Long> tierCounts = new LinkedHashMap<>();
        Map<String, CandidateAccumulator> candidates = new LinkedHashMap<>();

        for (AiChatMessage message : messages) {
            ObjectNode trace = parseTraceObject(message.getTrace());
            RouteFinalStep finalStep = resolveFinalStep(trace);
            String finalTier = firstText(trace.path("telemetry").path("finalTier"), finalStep.tier());
            if (!StringUtils.hasText(finalTier)) {
                continue;
            }
            tierCounts.merge(finalTier, 1L, Long::sum);
            boolean weakPath = trace.path("telemetry").path("weakPath").asBoolean(isWeakPath(finalTier));
            if (!weakPath) {
                continue;
            }
            String domain = firstText(trace.path("telemetry").path("domain"), message.getRoutedDomain());
            String responseKind = firstText(trace.path("telemetry").path("responseKind"), message.getResponseKind());
            String target = firstText(
                    trace.path("telemetry").path("target"),
                    firstText(trace.path("telemetry").path("finalTarget"), message.getTargetView()));
            String dataSurface = firstText(trace.path("telemetry").path("dataSurface"), message.getDataSurface());
            String key = String.join("|",
                    safeKey(finalTier),
                    safeKey(domain),
                    safeKey(responseKind),
                    safeKey(target),
                    safeKey(dataSurface));
            CandidateAccumulator accumulator = candidates.computeIfAbsent(
                    key,
                    ignored -> new CandidateAccumulator(finalTier, domain, responseKind, target, dataSurface));
            accumulator.count++;
            String question = trace.path("telemetry").path("question").asText(null);
            if (StringUtils.hasText(question) && accumulator.questionSamples.size() < MAX_SAMPLE_QUESTIONS
                    && !accumulator.questionSamples.contains(question)) {
                accumulator.questionSamples.add(question);
            }
        }

        List<MartCandidateSignal> martCandidateSignals = candidates.values().stream()
                .sorted(Comparator
                        .comparingLong(CandidateAccumulator::count).reversed()
                        .thenComparing(CandidateAccumulator::targetKey))
                .limit(safeLimit)
                .map(CandidateAccumulator::toSignal)
                .toList();
        return new RouteTelemetrySummary(
                safeDays,
                messages.size(),
                Map.copyOf(tierCounts),
                martCandidateSignals);
    }

    private ObjectNode parseTraceObject(String traceJson) {
        if (!StringUtils.hasText(traceJson)) {
            return objectMapper.createObjectNode();
        }
        try {
            JsonNode node = objectMapper.readTree(traceJson);
            return node != null && node.isObject() ? (ObjectNode) node : objectMapper.createObjectNode();
        } catch (Exception ignored) {
            return objectMapper.createObjectNode();
        }
    }

    private RouteFinalStep resolveFinalStep(JsonNode trace) {
        JsonNode routeTrace = trace.path("routeTrace");
        if (!routeTrace.isArray()) {
            return new RouteFinalStep(null, null);
        }
        RouteFinalStep fallback = new RouteFinalStep(null, null);
        for (JsonNode step : routeTrace) {
            String tier = step.path("tier").asText(null);
            String target = step.path("target").asText(null);
            if (StringUtils.hasText(tier)) {
                fallback = new RouteFinalStep(tier, target);
            }
            if ("HIT".equalsIgnoreCase(step.path("status").asText(""))) {
                return new RouteFinalStep(tier, target);
            }
        }
        return fallback;
    }

    private static boolean isWeakPath(String finalTier) {
        return "TIER_4_GUARDRAIL_FEDERATED".equals(finalTier)
                || "TIER_5_DIRECT_DETAIL".equals(finalTier);
    }

    private static void putIfPresent(ObjectNode node, String fieldName, String value) {
        if (StringUtils.hasText(value)) {
            node.put(fieldName, value);
        }
    }

    private static String firstText(JsonNode node, String fallback) {
        if (node != null && node.isTextual() && StringUtils.hasText(node.asText())) {
            return node.asText();
        }
        return fallback;
    }

    private static String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
    }

    private static String safeKey(String value) {
        return StringUtils.hasText(value) ? value : "";
    }

    public record RouteTelemetrySummary(
            int days,
            long totalMessages,
            Map<String, Long> tierCounts,
            List<MartCandidateSignal> martCandidateSignals) {
    }

    public record MartCandidateSignal(
            String finalTier,
            String domain,
            String responseKind,
            String target,
            String dataSurface,
            long count,
            List<String> questionSamples) {
    }

    private record RouteFinalStep(String tier, String target) {
    }

    private static final class CandidateAccumulator {
        private final String finalTier;
        private final String domain;
        private final String responseKind;
        private final String target;
        private final String dataSurface;
        private long count;
        private final List<String> questionSamples = new ArrayList<>();

        private CandidateAccumulator(
                String finalTier,
                String domain,
                String responseKind,
                String target,
                String dataSurface) {
            this.finalTier = finalTier;
            this.domain = domain;
            this.responseKind = responseKind;
            this.target = target;
            this.dataSurface = dataSurface;
        }

        private long count() {
            return count;
        }

        private String targetKey() {
            return safeKey(target);
        }

        private MartCandidateSignal toSignal() {
            return new MartCandidateSignal(
                    finalTier,
                    domain,
                    responseKind,
                    target,
                    dataSurface,
                    count,
                    List.copyOf(questionSamples));
        }
    }
}
