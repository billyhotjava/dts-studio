package com.yuzhi.dts.copilot.ai.service.copilot;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

@Service
public class SemanticDraftService {

    private static final AtomicLong DRAFT_SEQUENCE = new AtomicLong(1);
    private static final Map<String, List<String>> REQUIRED_KEYS = Map.of(
            "object", List.of("objectCode", "objectName", "sourceRefs"),
            "indicator", List.of("indicatorCode", "indicatorName", "formula", "objectCode"),
            "caliber-rule", List.of("ruleCode", "ruleName", "guardrailText"));

    private final List<SemanticDraft> localStaging = new ArrayList<>();

    public synchronized SemanticDraft createDraft(SemanticDraftRequest request) {
        SemanticDraftRequest safeRequest = request == null
                ? new SemanticDraftRequest("", "", Map.of(), "", List.of(), "")
                : request;
        String draftType = normalizeType(safeRequest.draftType());
        List<String> requiredContentKeys = REQUIRED_KEYS.getOrDefault(draftType, List.of());
        List<String> validationErrors = validationErrors(draftType, requiredContentKeys, safeRequest.content());
        SemanticDraft draft = new SemanticDraft(
                nextDraftId(),
                draftType,
                safeRequest.domain(),
                safeRequest.content(),
                safeRequest.triggerQuestion(),
                safeRequest.evidence(),
                normalizeSource(safeRequest.source()),
                validationErrors.isEmpty() ? "LOCAL_STAGED" : "REJECTED",
                "NOT_SUBMITTED",
                "SUBMIT_TO_GOVERNANCE_DRAFT",
                requiredContentKeys,
                validationErrors,
                false,
                false,
                Instant.now().toString(),
                "",
                "",
                "",
                "",
                "");
        if (validationErrors.isEmpty()) {
            localStaging.add(draft);
        }
        return draft;
    }

    public synchronized List<SemanticDraft> listDrafts() {
        return List.copyOf(localStaging);
    }

    public synchronized Optional<SemanticDraft> findDraft(String draftId) {
        String expectedId = textOrEmpty(draftId);
        return localStaging.stream()
                .filter(draft -> draft.draftId().equals(expectedId))
                .findFirst();
    }

    public synchronized SemanticDraft recordGovernanceSubmission(
            SemanticDraftGovernanceSubmissionService.GovernanceDraftSubmissionResult submission) {
        String draftId = submission == null ? "" : submission.draftId();
        for (int i = 0; i < localStaging.size(); i++) {
            SemanticDraft current = localStaging.get(i);
            if (!current.draftId().equals(draftId)) {
                continue;
            }
            if (submission == null || !submission.submitted()) {
                return current;
            }
            SemanticDraft updated = new SemanticDraft(
                    current.draftId(),
                    current.draftType(),
                    current.domain(),
                    current.content(),
                    current.triggerQuestion(),
                    current.evidence(),
                    current.source(),
                    current.status(),
                    submission.governanceStatus(),
                    "WAIT_FOR_GOVERNANCE_REVIEW",
                    current.requiredContentKeys(),
                    current.validationErrors(),
                    current.sotTouched(),
                    current.businessDatabaseTouched(),
                    current.createdAt(),
                    submission.targetType(),
                    submission.targetPath(),
                    submission.platformId(),
                    submission.platformStatus(),
                    Instant.now().toString());
            localStaging.set(i, updated);
            return updated;
        }
        throw new IllegalArgumentException("semantic draft not found: " + draftId);
    }

    private static List<String> validationErrors(String draftType, List<String> requiredContentKeys, Map<String, Object> content) {
        List<String> errors = new ArrayList<>();
        if (!REQUIRED_KEYS.containsKey(draftType)) {
            errors.add("unsupported draft type: " + draftType);
            return List.copyOf(errors);
        }
        for (String requiredKey : requiredContentKeys) {
            Object value = content.get(requiredKey);
            if (value == null || (value instanceof String text && text.isBlank())
                    || (value instanceof List<?> list && list.isEmpty())) {
                errors.add("missing required content key: " + requiredKey);
            }
        }
        return List.copyOf(errors);
    }

    private static String nextDraftId() {
        return "semantic-draft-" + DRAFT_SEQUENCE.getAndIncrement();
    }

    private static String normalizeType(String draftType) {
        return textOrEmpty(draftType).trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static String normalizeSource(String source) {
        String normalized = textOrEmpty(source).trim().toLowerCase(java.util.Locale.ROOT);
        return switch (normalized) {
            case "human", "automatic" -> normalized;
            default -> "human";
        };
    }

    private static String textOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private static <T> List<T> copyOrEmpty(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    public record SemanticDraftRequest(
            String draftType,
            String domain,
            Map<String, Object> content,
            String triggerQuestion,
            List<String> evidence,
            String source) {
        public SemanticDraftRequest {
            draftType = textOrEmpty(draftType);
            domain = textOrEmpty(domain);
            content = content == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(content));
            triggerQuestion = textOrEmpty(triggerQuestion);
            evidence = copyOrEmpty(evidence);
            source = normalizeSource(source);
        }
    }

    public record SemanticDraft(
            String draftId,
            String draftType,
            String domain,
            Map<String, Object> content,
            String triggerQuestion,
            List<String> evidence,
            String source,
            String status,
            String governanceStatus,
            String nextAction,
            List<String> requiredContentKeys,
            List<String> validationErrors,
            boolean sotTouched,
            boolean businessDatabaseTouched,
            String createdAt,
            String governanceTargetType,
            String governanceTargetPath,
            String platformDraftId,
            String platformDraftStatus,
            String governanceSubmittedAt) {
        public SemanticDraft {
            draftId = textOrEmpty(draftId);
            draftType = textOrEmpty(draftType);
            domain = textOrEmpty(domain);
            content = content == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(content));
            triggerQuestion = textOrEmpty(triggerQuestion);
            evidence = copyOrEmpty(evidence);
            source = normalizeSource(source);
            status = textOrEmpty(status);
            governanceStatus = textOrEmpty(governanceStatus);
            nextAction = textOrEmpty(nextAction);
            requiredContentKeys = copyOrEmpty(requiredContentKeys);
            validationErrors = copyOrEmpty(validationErrors);
            createdAt = textOrEmpty(createdAt);
            governanceTargetType = textOrEmpty(governanceTargetType);
            governanceTargetPath = textOrEmpty(governanceTargetPath);
            platformDraftId = textOrEmpty(platformDraftId);
            platformDraftStatus = textOrEmpty(platformDraftStatus);
            governanceSubmittedAt = textOrEmpty(governanceSubmittedAt);
        }
    }
}
