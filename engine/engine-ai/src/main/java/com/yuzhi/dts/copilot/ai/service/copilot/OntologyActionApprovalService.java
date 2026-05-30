package com.yuzhi.dts.copilot.ai.service.copilot;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuzhi.dts.copilot.ai.security.CopilotUserContext;
import com.yuzhi.dts.copilot.ai.service.audit.AiAuditService;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class OntologyActionApprovalService {

    private static final String ACTION_TOOL_ID = "ontology.action.createDraft";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final OntologyService ontologyService;
    private final OntologyActionExecutor actionExecutor;
    private final ActionGuardService actionGuardService;
    private final AiAuditService auditService;

    public OntologyActionApprovalService(
            OntologyService ontologyService,
            OntologyActionExecutor actionExecutor,
            ActionGuardService actionGuardService,
            AiAuditService auditService) {
        this.ontologyService = ontologyService;
        this.actionExecutor = actionExecutor;
        this.actionGuardService = actionGuardService;
        this.auditService = auditService;
    }

    public ActionApprovalResult requestDraft(ActionApprovalRequest request) {
        ActionApprovalRequest safeRequest = request == null
                ? new ActionApprovalRequest(null, null, Map.of(), false, null, null)
                : request;
        Optional<SemanticPackService.OntologyAction> action = ontologyService.load(safeRequest.domain())
                .flatMap(model -> model.getAction(safeRequest.actionName()));
        if (action.isEmpty()) {
            return ActionApprovalResult.failure(false, "Action not found: " + safeRequest.actionName(), null);
        }
        SemanticPackService.OntologyAction selectedAction = action.get();
        Map<String, Object> objectAttributes = safeRequest.objectAttributes() == null
                ? Map.of()
                : safeRequest.objectAttributes();
        ActionApprovalCard card = buildCard(safeRequest.domain(), selectedAction, objectAttributes);
        if (!safeRequest.confirmed()) {
            return new ActionApprovalResult(
                    false,
                    true,
                    "该动作需要用户确认后才会创建草稿",
                    card,
                    null);
        }

        ActionGuardService.GuardDecision guard = actionGuardService.verify(
                selectedAction.guard(), safeRequest.userContext());
        if (!guard.allowed()) {
            if (selectedAction.audit()) {
                auditService.logActionExecution(buildAuditEvent(
                        safeRequest,
                        selectedAction,
                        objectAttributes,
                        false,
                        "{}",
                        guard.message()));
            }
            return new ActionApprovalResult(false, false, guard.message(), card, null);
        }

        OntologyActionExecutor.ActionDraftResult draftResult = actionExecutor.createDraft(
                safeRequest.domain(), selectedAction.name(), objectAttributes);
        if (selectedAction.audit()) {
            auditService.logActionExecution(buildAuditEvent(
                    safeRequest,
                    selectedAction,
                    objectAttributes,
                    draftResult.success(),
                    toJson(Map.of(
                            "message", draftResult.message(),
                            "responseBody", draftResult.responseBody(),
                            "draftEndpoint", draftResult.draftEndpoint())),
                    draftResult.success() ? null : draftResult.message()));
        }
        return new ActionApprovalResult(
                draftResult.success(),
                false,
                draftResult.message(),
                card,
                draftResult);
    }

    private ActionApprovalCard buildCard(
            String domain,
            SemanticPackService.OntologyAction action,
            Map<String, Object> objectAttributes) {
        List<MicroFormField> fields = action.params().stream()
                .map(param -> new MicroFormField(
                        param.name(),
                        param.name(),
                        fieldType(param.name()),
                        param.required(),
                        "来自 " + param.source(),
                        param.source()))
                .toList();
        MicroFormSchema microForm = new MicroFormSchema(
                action.name(),
                "确认后将通过 adminapi 创建草稿，正式提交仍需 adminweb 人工复核。",
                "HIGH",
                "该动作会创建业务草稿，请确认参数与影响范围。",
                fields);
        return new ActionApprovalCard(
                domain + ":" + action.name(),
                ACTION_TOOL_ID,
                objectAttributes,
                "该建议动作需要人工确认后执行。",
                action.name() + " / " + action.object(),
                "只创建草稿，不调用正式提交端点。",
                microForm);
    }

    private static String fieldType(String paramName) {
        if ("draftItemJson".equals(paramName)) {
            return "textarea";
        }
        if ("projectId".equals(paramName) || "badDebtType".equals(paramName)) {
            return "number";
        }
        return "text";
    }

    private static AiAuditService.ActionAuditEvent buildAuditEvent(
            ActionApprovalRequest request,
            SemanticPackService.OntologyAction action,
            Map<String, Object> input,
            boolean success,
            String result,
            String errorMessage) {
        CopilotUserContext userContext = request.userContext();
        return new AiAuditService.ActionAuditEvent(
                userContext == null ? null : userContext.userId(),
                request.sessionId(),
                action.name(),
                action.object(),
                action.guard(),
                toJson(input),
                result,
                success,
                errorMessage);
    }

    private static String toJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    public record ActionApprovalRequest(
            String domain,
            String actionName,
            Map<String, Object> objectAttributes,
            boolean confirmed,
            CopilotUserContext userContext,
            String sessionId) {
    }

    public record ActionApprovalResult(
            boolean success,
            boolean requiresApproval,
            String message,
            ActionApprovalCard card,
            OntologyActionExecutor.ActionDraftResult draftResult) {
        static ActionApprovalResult failure(boolean requiresApproval, String message, ActionApprovalCard card) {
            return new ActionApprovalResult(false, requiresApproval, message, card, null);
        }
    }

    public record ActionApprovalCard(
            String actionId,
            String toolId,
            Map<String, Object> params,
            String reason,
            String planSummary,
            String impactScope,
            MicroFormSchema microForm) {
        public ActionApprovalCard {
            params = params == null ? Map.of() : Collections.unmodifiableMap(params);
        }
    }

    public record MicroFormSchema(
            String title,
            String description,
            String riskLevel,
            String riskNote,
            List<MicroFormField> fields) {
        public MicroFormSchema {
            fields = fields == null ? List.of() : List.copyOf(fields);
        }
    }

    public record MicroFormField(
            String key,
            String label,
            String type,
            boolean required,
            String helpText,
            String source) {
    }
}
