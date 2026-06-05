package com.yuzhi.dts.copilot.ai.service.copilot;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SemanticDraftGovernanceSubmissionService {

    private static final String STATUS_LOCAL_STAGED = "LOCAL_STAGED";
    private static final String STATUS_DRAFT = "DRAFT";
    private static final Map<String, String> PLATFORM_DOMAIN_CODES = Map.of(
            "finance", "S10-FIN",
            "project", "S10-PM",
            "rental", "PRT");

    private final GovernanceDraftClient governanceDraftClient;

    public SemanticDraftGovernanceSubmissionService(GovernanceDraftClient governanceDraftClient) {
        this.governanceDraftClient = governanceDraftClient;
    }

    public GovernanceDraftSubmissionResult submitDraft(SemanticDraftService.SemanticDraft draft) {
        if (draft == null) {
            return notSubmitted("", "", "", "draft is required");
        }
        if (!STATUS_LOCAL_STAGED.equalsIgnoreCase(draft.status())) {
            return notSubmitted(draft.draftId(), "", "", "draft must be LOCAL_STAGED before governance submission");
        }
        GovernanceDraftSubmission submission = toSubmission(draft);
        if (!StringUtils.hasText(submission.targetPath())) {
            return notSubmitted(draft.draftId(), submission.targetType(), "", "unsupported draft type: " + draft.draftType());
        }
        try {
            return governanceDraftClient.submit(submission);
        } catch (RuntimeException ex) {
            return notSubmitted(draft.draftId(), submission.targetType(), submission.targetPath(), ex.getMessage());
        }
    }

    private GovernanceDraftSubmission toSubmission(SemanticDraftService.SemanticDraft draft) {
        return switch (draft.draftType()) {
            case "object" -> new GovernanceDraftSubmission(
                    draft.draftId(),
                    "SEMANTIC_OBJECT",
                    "/api/semantic/business-objects",
                    objectPayload(draft));
            case "indicator" -> new GovernanceDraftSubmission(
                    draft.draftId(),
                    "GOVERNANCE_INDICATOR",
                    "/api/governance/indicators",
                    indicatorPayload(draft));
            case "caliber-rule" -> new GovernanceDraftSubmission(
                    draft.draftId(),
                    "DATA_STANDARD_CALIBER_RULE",
                    "/api/modeling/standards",
                    caliberRulePayload(draft));
            default -> new GovernanceDraftSubmission(draft.draftId(), "", "", Map.of());
        };
    }

    private Map<String, Object> objectPayload(SemanticDraftService.SemanticDraft draft) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("code", text(draft.content(), "objectCode"));
        payload.put("name", text(draft.content(), "objectName"));
        payload.put("description", metadata(draft, text(draft.content(), "description")));
        payload.put("status", STATUS_DRAFT);
        return payload;
    }

    private Map<String, Object> indicatorPayload(SemanticDraftService.SemanticDraft draft) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("code", text(draft.content(), "indicatorCode"));
        payload.put("name", text(draft.content(), "indicatorName"));
        payload.put("definition", metadata(draft, text(draft.content(), "definition")));
        payload.put("expressionSql", text(draft.content(), "formula"));
        payload.put("domain", platformDomainCode(draft));
        payload.put("status", STATUS_DRAFT);
        payload.put("version", "draft");
        payload.put("versionNotes", metadata(draft, ""));
        payload.put("tags", "copilot,draft," + draft.domain());
        payload.put("llmGenerated", "automatic".equalsIgnoreCase(draft.source()));
        payload.put("llmSourceRef", metadata(draft, ""));
        payload.put("humanVerified", false);
        return payload;
    }

    private Map<String, Object> caliberRulePayload(SemanticDraftService.SemanticDraft draft) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("code", text(draft.content(), "ruleCode"));
        payload.put("name", text(draft.content(), "ruleName"));
        payload.put("domain", draft.domain());
        payload.put("scope", "CALIBER_RULE");
        payload.put("status", STATUS_DRAFT);
        payload.put("version", "draft");
        payload.put("versionStatus", STATUS_DRAFT);
        payload.put("description", metadata(draft, text(draft.content(), "guardrailText")));
        payload.put("changeSummary", metadata(draft, ""));
        payload.put("dataType", "RULE");
        payload.put("nullable", false);
        payload.put("tags", List.of("copilot", "draft", "caliber-rule"));
        return payload;
    }

    private static GovernanceDraftSubmissionResult notSubmitted(String draftId, String targetType, String targetPath, String error) {
        return new GovernanceDraftSubmissionResult(
                valueOrEmpty(draftId),
                valueOrEmpty(targetType),
                valueOrEmpty(targetPath),
                "",
                "",
                "NOT_SUBMITTED",
                false,
                valueOrEmpty(error));
    }

    private static String metadata(SemanticDraftService.SemanticDraft draft, String leadingText) {
        List<String> evidence = draft.evidence() == null ? List.of() : draft.evidence();
        StringBuilder text = new StringBuilder();
        if (StringUtils.hasText(leadingText)) {
            text.append(leadingText.trim()).append("\n\n");
        }
        text.append("source=copilot");
        text.append("; draftId=").append(draft.draftId());
        text.append("; draftSource=").append(draft.source());
        if (StringUtils.hasText(draft.triggerQuestion())) {
            text.append("; triggerQuestion=").append(draft.triggerQuestion());
        }
        if (!evidence.isEmpty()) {
            text.append("; evidence=").append(String.join(",", evidence));
        }
        return text.toString();
    }

    private static String text(Map<String, Object> content, String key) {
        Object value = content == null ? null : content.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private static String platformDomainCode(SemanticDraftService.SemanticDraft draft) {
        String explicit = text(draft.content(), "platformDomainCode");
        if (StringUtils.hasText(explicit)) {
            return explicit.trim();
        }
        String domain = valueOrEmpty(draft.domain()).trim();
        String mapped = PLATFORM_DOMAIN_CODES.get(domain.toLowerCase());
        return StringUtils.hasText(mapped) ? mapped : domain;
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    public interface GovernanceDraftClient {
        GovernanceDraftSubmissionResult submit(GovernanceDraftSubmission submission);
    }

    public record GovernanceDraftSubmission(
            String draftId,
            String targetType,
            String targetPath,
            Map<String, Object> payload) {
        public GovernanceDraftSubmission {
            draftId = valueOrEmpty(draftId);
            targetType = valueOrEmpty(targetType);
            targetPath = valueOrEmpty(targetPath);
            payload = payload == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(payload));
        }
    }

    public record GovernanceDraftSubmissionResult(
            String draftId,
            String targetType,
            String targetPath,
            String platformId,
            String platformStatus,
            String governanceStatus,
            boolean submitted,
            String error) {
        public GovernanceDraftSubmissionResult {
            draftId = valueOrEmpty(draftId);
            targetType = valueOrEmpty(targetType);
            targetPath = valueOrEmpty(targetPath);
            platformId = valueOrEmpty(platformId);
            platformStatus = valueOrEmpty(platformStatus);
            governanceStatus = valueOrEmpty(governanceStatus);
            error = valueOrEmpty(error);
        }
    }
}
