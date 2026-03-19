package com.yuzhi.dts.copilot.analytics.web.rest;

import com.yuzhi.dts.copilot.analytics.service.CopilotQueryService;
import com.yuzhi.dts.copilot.analytics.service.DatasetQueryService;
import com.yuzhi.dts.copilot.analytics.service.ForbiddenQueryException;
import com.yuzhi.dts.copilot.analytics.web.rest.errors.ApiError;
import com.yuzhi.dts.copilot.analytics.web.support.RequestContextUtils;
import jakarta.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
public class CopilotQueryResource {

    private static final Logger log = LoggerFactory.getLogger(CopilotQueryResource.class);
    private static final String ERROR_CODE_HEADER = "X-Error-Code";
    private static final String ERROR_RETRYABLE_HEADER = "X-Error-Retryable";

    private final CopilotQueryService copilotQueryService;

    public CopilotQueryResource(CopilotQueryService copilotQueryService) {
        this.copilotQueryService = copilotQueryService;
    }

    public record CopilotQueryRequest(
            String sql,
            Long datasourceId,
            String source,        // "copilot-nl2sql" or "copilot-template"
            String routedDomain   // "project", "flowerbiz", etc.
    ) {}

    @PostMapping(
            path = "/copilot/execute",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> execute(
            @RequestBody CopilotQueryRequest body,
            HttpServletRequest request) {

        String userId = request.getHeader("X-DTS-User-Id");
        String userName = request.getHeader("X-DTS-User-Name");

        if (userId == null || userId.isBlank()) {
            return buildApiError(
                    HttpStatus.UNAUTHORIZED,
                    "SEC_UNAUTHORIZED",
                    "X-DTS-User-Id header is required",
                    false,
                    request);
        }

        if (body == null || body.sql() == null || body.sql().isBlank()) {
            return buildApiError(
                    HttpStatus.BAD_REQUEST,
                    "REQ_INVALID_ARGUMENT",
                    "sql is required",
                    false,
                    request);
        }

        if (body.datasourceId() == null || body.datasourceId() <= 0) {
            return buildApiError(
                    HttpStatus.BAD_REQUEST,
                    "REQ_INVALID_ARGUMENT",
                    "datasourceId is required and must be positive",
                    false,
                    request);
        }

        // Validate SQL against view whitelist
        try {
            copilotQueryService.validateSqlSources(body.sql());
        } catch (ForbiddenQueryException e) {
            log.warn("[copilot-query] forbidden query from user={} source={} domain={}: {}",
                    userId, body.source(), body.routedDomain(), e.getMessage());
            return buildApiError(
                    HttpStatus.FORBIDDEN,
                    "COPILOT_QUERY_FORBIDDEN",
                    e.getMessage(),
                    false,
                    request);
        }

        // Execute the validated query
        try {
            DatasetQueryService.DatasetResult result =
                    copilotQueryService.executeCopilotQuery(body.sql(), body.datasourceId(), userId);

            log.info("[copilot-query] success user={} userName={} source={} domain={} rows={}",
                    userId, userName, body.source(), body.routedDomain(),
                    result.rows().size());

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("rows", result.rows());
            data.put("cols", result.cols());
            data.put("results_timezone", result.resultsTimezone());
            data.put("results_metadata", Map.of("columns", result.resultsMetadataColumns()));

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("data", data);
            response.put("database_id", body.datasourceId());
            response.put("status", "completed");
            response.put("row_count", result.rows().size());
            response.put("source", body.source());
            response.put("routed_domain", body.routedDomain());

            return ResponseEntity.ok(response);

        } catch (SQLException e) {
            log.error("[copilot-query] execution failed user={} source={} domain={}: {}",
                    userId, body.source(), body.routedDomain(), e.getMessage());
            return buildApiError(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "QUERY_EXEC_FAILED",
                    "Error executing query: " + e.getMessage(),
                    false,
                    request);
        }
    }

    @ExceptionHandler(ForbiddenQueryException.class)
    public ResponseEntity<ApiError> handleForbiddenQuery(
            ForbiddenQueryException ex, HttpServletRequest request) {
        String requestId = RequestContextUtils.resolveRequestId();
        if (requestId == null || requestId.isBlank()) {
            requestId = MDC.get("requestId");
        }
        ApiError payload = new ApiError(
                OffsetDateTime.now(),
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                "COPILOT_QUERY_FORBIDDEN",
                false,
                ex.getMessage(),
                request.getRequestURI(),
                requestId);
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .header(ERROR_CODE_HEADER, "COPILOT_QUERY_FORBIDDEN")
                .header(ERROR_RETRYABLE_HEADER, "false")
                .body(payload);
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
                .header(ERROR_CODE_HEADER, code)
                .header(ERROR_RETRYABLE_HEADER, String.valueOf(retryable))
                .body(payload);
    }
}
