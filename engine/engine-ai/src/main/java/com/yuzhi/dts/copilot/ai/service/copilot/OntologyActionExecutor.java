package com.yuzhi.dts.copilot.ai.service.copilot;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class OntologyActionExecutor {

    private final OntologyService ontologyService;
    private final AdminApiActionClient adminApiActionClient;
    private final ObjectMapper objectMapper;

    public OntologyActionExecutor(
            OntologyService ontologyService,
            AdminApiActionClient adminApiActionClient,
            ObjectMapper objectMapper) {
        this.ontologyService = ontologyService;
        this.adminApiActionClient = adminApiActionClient;
        this.objectMapper = objectMapper;
    }

    public ActionDraftResult createDraft(
            String domain,
            String actionName,
            Map<String, Object> objectAttributes) {
        Optional<SemanticPackService.OntologyAction> action = ontologyService.load(domain)
                .flatMap(model -> model.getAction(actionName));
        if (action.isEmpty()) {
            return ActionDraftResult.failure(actionName, null, null, "Action not found: " + actionName);
        }
        SemanticPackService.OntologyAction selectedAction = action.get();
        if (!"adminapi".equals(selectedAction.endpoint().service())) {
            return ActionDraftResult.failure(
                    selectedAction.name(),
                    selectedAction.endpoint().draft(),
                    selectedAction.endpoint().commit(),
                    "Unsupported action endpoint service: " + selectedAction.endpoint().service());
        }

        PayloadResult payload = assemblePayload(selectedAction, objectAttributes == null ? Map.of() : objectAttributes);
        if (!payload.success()) {
            return ActionDraftResult.failure(
                    selectedAction.name(),
                    selectedAction.endpoint().draft(),
                    selectedAction.endpoint().commit(),
                    payload.message());
        }

        AdminApiActionClient.AdminApiActionResponse response = adminApiActionClient.postDraft(
                selectedAction.endpoint().draft(),
                payload.payload());
        return new ActionDraftResult(
                response.success(),
                selectedAction.name(),
                selectedAction.endpoint().draft(),
                selectedAction.endpoint().commit(),
                payload.payload(),
                response.message(),
                response.body());
    }

    private PayloadResult assemblePayload(
            SemanticPackService.OntologyAction action,
            Map<String, Object> objectAttributes) {
        Map<String, Object> payload = new LinkedHashMap<>();
        for (SemanticPackService.OntologyActionParam param : action.params()) {
            Optional<Object> value = resolveParamValue(param.name(), param.source(), objectAttributes);
            if (value.isEmpty()) {
                if (param.required()) {
                    return PayloadResult.failure("Missing required action param: " + param.name());
                }
                continue;
            }
            payload.put(param.name(), normalizeValue(param.name(), value.get()));
        }
        return PayloadResult.success(payload);
    }

    private Optional<Object> resolveParamValue(String paramName, String source, Map<String, Object> objectAttributes) {
        if (objectAttributes.containsKey(paramName)) {
            return Optional.ofNullable(objectAttributes.get(paramName));
        }
        if (objectAttributes.containsKey(source)) {
            return Optional.ofNullable(objectAttributes.get(source));
        }
        int separator = source.lastIndexOf('.');
        if (separator > 0 && separator < source.length() - 1) {
            String fieldName = source.substring(separator + 1);
            if (objectAttributes.containsKey(fieldName)) {
                return Optional.ofNullable(objectAttributes.get(fieldName));
            }
        }
        return Optional.empty();
    }

    private Object normalizeValue(String paramName, Object value) {
        if (!"draftItemJson".equals(paramName) || value instanceof String) {
            return value;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize draftItemJson", e);
        }
    }

    private record PayloadResult(
            boolean success,
            Map<String, Object> payload,
            String message) {
        static PayloadResult success(Map<String, Object> payload) {
            return new PayloadResult(true, Collections.unmodifiableMap(payload), "ok");
        }

        static PayloadResult failure(String message) {
            return new PayloadResult(false, Map.of(), message);
        }
    }

    public record ActionDraftResult(
            boolean success,
            String actionName,
            String draftEndpoint,
            String commitEndpoint,
            Map<String, Object> payload,
            String message,
            Map<String, Object> responseBody) {
        public ActionDraftResult {
            payload = payload == null ? Map.of() : Collections.unmodifiableMap(payload);
            responseBody = responseBody == null ? Map.of() : Collections.unmodifiableMap(responseBody);
        }

        static ActionDraftResult failure(
                String actionName,
                String draftEndpoint,
                String commitEndpoint,
                String message) {
            return new ActionDraftResult(false, actionName, draftEndpoint, commitEndpoint, Map.of(), message, Map.of());
        }
    }
}
