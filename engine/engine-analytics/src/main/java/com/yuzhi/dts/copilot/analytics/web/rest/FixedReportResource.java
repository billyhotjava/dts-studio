package com.yuzhi.dts.copilot.analytics.web.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.yuzhi.dts.copilot.analytics.domain.AnalyticsReportTemplate;
import com.yuzhi.dts.copilot.analytics.domain.AnalyticsUser;
import com.yuzhi.dts.copilot.analytics.repository.AnalyticsReportTemplateRepository;
import com.yuzhi.dts.copilot.analytics.service.AnalyticsSessionService;
import com.yuzhi.dts.copilot.analytics.service.report.ReportExecutionPlanService;
import com.yuzhi.dts.copilot.analytics.service.report.ReportExecutionPlanService.ReportExecutionPlan;
import com.yuzhi.dts.copilot.analytics.web.rest.errors.ApiError;
import com.yuzhi.dts.copilot.analytics.web.support.MetabaseAuth;
import com.yuzhi.dts.copilot.analytics.web.support.RequestContextUtils;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.MDC;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fixed-reports")
public class FixedReportResource {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int TEMPLATE_SCAN_LIMIT = 200;

    private final AnalyticsSessionService sessionService;
    private final AnalyticsReportTemplateRepository templateRepository;
    private final ReportExecutionPlanService planService;

    public FixedReportResource(
            AnalyticsSessionService sessionService,
            AnalyticsReportTemplateRepository templateRepository,
            ReportExecutionPlanService planService) {
        this.sessionService = sessionService;
        this.templateRepository = templateRepository;
        this.planService = planService;
    }

    @PostMapping(path = "/{templateCode}/run", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> run(
            @PathVariable("templateCode") String templateCode,
            @RequestBody(required = false) JsonNode body,
            HttpServletRequest request) {
        Optional<AnalyticsUser> user = MetabaseAuth.currentUser(sessionService, request);
        if (user.isEmpty()) {
            return buildApiError(
                    HttpStatus.UNAUTHORIZED,
                    "SEC_UNAUTHORIZED",
                    "Authentication required",
                    false,
                    request);
        }

        if (body != null && !body.isObject()) {
            return buildApiError(
                    HttpStatus.BAD_REQUEST,
                    "REQ_INVALID_ARGUMENT",
                    "Request body must be an object",
                    false,
                    request);
        }

        Optional<AnalyticsReportTemplate> template = resolveTemplate(templateCode);
        if (template.isEmpty()) {
            return buildApiError(
                    HttpStatus.NOT_FOUND,
                    "REPORT_TEMPLATE_NOT_FOUND",
                    "Fixed report template not found",
                    false,
                    request);
        }

        ResponseEntity<?> permissionDenied = enforcePermission(template.get(), user.get(), request);
        if (permissionDenied != null) {
            return permissionDenied;
        }

        JsonNode parametersNode = body == null ? null : body.path("parameters");
        if (parametersNode != null && !parametersNode.isMissingNode() && !parametersNode.isNull() && !parametersNode.isObject()) {
            return buildApiError(
                    HttpStatus.BAD_REQUEST,
                    "REQ_INVALID_ARGUMENT",
                    "parameters must be an object",
                    false,
                    request);
        }

        Set<String> allowedParameterNames = allowedParameterNames(template.get().getParameterSchemaJson());
        if (!allowedParameterNames.isEmpty() && parametersNode != null && parametersNode.isObject()) {
            List<String> unsupported = unsupportedParameterNames(parametersNode, allowedParameterNames);
            if (!unsupported.isEmpty()) {
                return buildApiError(
                        HttpStatus.BAD_REQUEST,
                        "REQ_INVALID_ARGUMENT",
                        "Unsupported parameter(s): " + String.join(", ", unsupported),
                        false,
                        request);
            }
        }

        ReportExecutionPlan plan = planService.planFor(template.get());
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("templateCode", safeText(template.get().getTemplateCode(), templateCode));
        response.put("templateName", template.get().getName());
        response.put("domain", template.get().getDomain());
        response.put("freshness", template.get().getRefreshPolicy());
        response.put("sourceType", template.get().getDataSourceType());
        response.put("targetObject", template.get().getTargetObject());
        response.put("route", plan.route().name());
        response.put("adapterKey", plan.adapterKey());
        response.put("rationale", plan.rationale());
        response.put("supported", plan.route() != ReportExecutionPlanService.Route.EXPLORATION);
        response.put("executionStatus", plan.route() == ReportExecutionPlanService.Route.EXPLORATION ? "PLANNED" : "READY");
        response.put("parameters", parametersNode == null || parametersNode.isMissingNode()
                ? Map.of()
                : OBJECT_MAPPER.convertValue(parametersNode, Map.class));
        return ResponseEntity.ok(response);
    }

    private Optional<AnalyticsReportTemplate> resolveTemplate(String templateCode) {
        String normalizedTemplateCode = trimToNull(templateCode);
        if (normalizedTemplateCode == null) {
            return Optional.empty();
        }

        Pageable page = PageRequest.of(0, TEMPLATE_SCAN_LIMIT);
        return templateRepository.findAllByArchivedFalseOrderByUpdatedAtDesc(page).stream()
                .filter(template -> matchesTemplateCode(template.getTemplateCode(), normalizedTemplateCode))
                .filter(AnalyticsReportTemplate::isPublished)
                .filter(template -> "certified".equalsIgnoreCase(trimToNull(template.getCertificationStatus())))
                .findFirst();
    }

    private ResponseEntity<?> enforcePermission(
            AnalyticsReportTemplate template, AnalyticsUser user, HttpServletRequest request) {
        JsonNode policy = parseJson(template.getPermissionPolicyJson());
        if (policy == null || policy.isNull()) {
            return null;
        }

        if (policy.path("superuserOnly").asBoolean(false) && !user.isSuperuser()) {
            return buildApiError(
                    HttpStatus.FORBIDDEN,
                    "SEC_FORBIDDEN",
                    "Fixed report permission denied",
                    false,
                    request);
        }

        JsonNode allowedUsernames = policy.get("allowedUsernames");
        if (allowedUsernames != null && allowedUsernames.isArray()) {
            boolean allowed = false;
            for (JsonNode node : allowedUsernames) {
                String candidate = trimToNull(node.asText(null));
                if (candidate != null && candidate.equalsIgnoreCase(user.getUsername())) {
                    allowed = true;
                    break;
                }
            }
            if (!allowed) {
                return buildApiError(
                        HttpStatus.FORBIDDEN,
                        "SEC_FORBIDDEN",
                        "Fixed report permission denied",
                        false,
                        request);
            }
        }

        return null;
    }

    private static List<String> unsupportedParameterNames(JsonNode parametersNode, Set<String> allowedParameterNames) {
        List<String> unsupported = new ArrayList<>();
        parametersNode.fieldNames().forEachRemaining(name -> {
            if (!allowedParameterNames.contains(name)) {
                unsupported.add(name);
            }
        });
        return unsupported;
    }

    private static Set<String> allowedParameterNames(String parameterSchemaJson) {
        JsonNode schema = parseJson(parameterSchemaJson);
        if (schema == null || schema.isNull()) {
            return Set.of();
        }
        JsonNode params = schema.get("params");
        if (params == null || !params.isArray()) {
            return Set.of();
        }

        Set<String> names = new LinkedHashSet<>();
        for (JsonNode param : params) {
            String name = trimToNull(param.path("name").asText(null));
            if (name == null && param.isTextual()) {
                name = trimToNull(param.asText());
            }
            if (name != null) {
                names.add(name);
            }
        }
        return names;
    }

    private static JsonNode parseJson(String value) {
        String text = trimToNull(value);
        if (text == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readTree(text);
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean matchesTemplateCode(String candidate, String expected) {
        String normalizedCandidate = trimToNull(candidate);
        return normalizedCandidate != null && normalizedCandidate.equalsIgnoreCase(expected);
    }

    private ResponseEntity<ApiError> buildApiError(
            HttpStatus status,
            String code,
            String message,
            boolean retryable,
            HttpServletRequest request) {
        String resolvedMessage = (message == null || message.isBlank()) ? status.getReasonPhrase() : message;
        String requestId = RequestContextUtils.resolveRequestId();
        if (requestId == null || requestId.isBlank()) {
            requestId = MDC.get("requestId");
        }
        ApiError payload = new ApiError(
                OffsetDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                code,
                retryable,
                resolvedMessage,
                request.getRequestURI(),
                requestId);
        return ResponseEntity.status(status)
                .body(payload);
    }

    private static String safeText(String value, String fallback) {
        String trimmed = trimToNull(value);
        if (trimmed != null) {
            return trimmed;
        }
        return fallback;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
