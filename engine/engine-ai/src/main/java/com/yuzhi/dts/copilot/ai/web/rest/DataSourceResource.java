package com.yuzhi.dts.copilot.ai.web.rest;

import com.yuzhi.dts.copilot.ai.domain.AiDataSource;
import com.yuzhi.dts.copilot.ai.service.datasource.AiDataSourceService;
import com.yuzhi.dts.copilot.ai.web.rest.dto.AiDataSourceUpsertRequest;
import com.yuzhi.dts.copilot.ai.web.rest.dto.ApiResponse;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/copilot/datasources")
public class DataSourceResource {

    private static final Logger log = LoggerFactory.getLogger(DataSourceResource.class);

    private final AiDataSourceService dataSourceService;
    private final String adminSecret;

    public DataSourceResource(
            AiDataSourceService dataSourceService,
            @Value("${copilot.admin-secret:}") String adminSecret) {
        this.dataSourceService = dataSourceService;
        this.adminSecret = adminSecret;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> list(
            @RequestHeader("X-Admin-Secret") String secret) {
        ResponseEntity<ApiResponse<List<Map<String, Object>>>> authCheck = checkAdminSecret(secret);
        if (authCheck != null) {
            return authCheck;
        }
        List<Map<String, Object>> items = dataSourceService.listDataSources().stream()
                .map(this::toSummary)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(items));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> create(
            @RequestHeader("X-Admin-Secret") String secret,
            @RequestBody AiDataSourceUpsertRequest request) {
        ResponseEntity<ApiResponse<Map<String, Object>>> authCheck = checkAdminSecret(secret);
        if (authCheck != null) {
            return authCheck;
        }
        try {
            AiDataSource created = dataSourceService.createDataSource(request);
            return ResponseEntity.ok(ApiResponse.ok(toDetail(created)));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        } catch (Exception ex) {
            log.error("Failed to create data source", ex);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to create data source"));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> get(
            @RequestHeader("X-Admin-Secret") String secret,
            @PathVariable String id) {
        ResponseEntity<ApiResponse<Map<String, Object>>> authCheck = checkAdminSecret(secret);
        if (authCheck != null) {
            return authCheck;
        }
        Long parsedId = parseId(id);
        if (parsedId == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Data source not found"));
        }
        return dataSourceService.getDataSource(parsedId)
                .map(source -> ResponseEntity.ok(ApiResponse.ok(toDetail(source))))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Data source not found")));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> update(
            @RequestHeader("X-Admin-Secret") String secret,
            @PathVariable Long id,
            @RequestBody AiDataSourceUpsertRequest request) {
        ResponseEntity<ApiResponse<Map<String, Object>>> authCheck = checkAdminSecret(secret);
        if (authCheck != null) {
            return authCheck;
        }
        try {
            AiDataSource updated = dataSourceService.updateDataSource(id, request);
            return ResponseEntity.ok(ApiResponse.ok(toDetail(updated)));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        } catch (Exception ex) {
            log.error("Failed to update data source {}", id, ex);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to update data source"));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @RequestHeader("X-Admin-Secret") String secret,
            @PathVariable Long id) {
        ResponseEntity<ApiResponse<Void>> authCheck = checkAdminSecret(secret);
        if (authCheck != null) {
            return authCheck;
        }
        try {
            dataSourceService.deleteDataSource(id);
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        }
    }

    private Map<String, Object> toSummary(AiDataSource source) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", source.getId());
        map.put("name", source.getName());
        map.put("type", source.getDbType());
        map.put("jdbcUrl", source.getJdbcUrl());
        map.put("description", source.getDescription());
        map.put("ownerDept", null);
        map.put("status", source.getStatus());
        map.put("driverVersion", null);
        map.put("lastUpdatedAt", stringifyInstant(source.getUpdatedAt()));
        return map;
    }

    private Map<String, Object> toDetail(AiDataSource source) {
        Map<String, Object> map = toSummary(source);
        map.put("username", source.getUsername());
        map.put("props", Map.of());
        map.put("secrets", source.getPassword() == null || source.getPassword().isBlank()
                ? Map.of()
                : Map.of("password", source.getPassword()));
        map.put("lastVerifiedAt", null);
        return map;
    }

    private Long parseId(String raw) {
        try {
            return Long.parseLong(raw);
        } catch (Exception ex) {
            return null;
        }
    }

    private String stringifyInstant(Instant instant) {
        return instant == null ? null : instant.toString();
    }

    private <T> ResponseEntity<ApiResponse<T>> checkAdminSecret(String secret) {
        if (adminSecret == null || adminSecret.isBlank()) {
            log.error("COPILOT_ADMIN_SECRET is not configured");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Admin secret not configured"));
        }
        if (!adminSecret.equals(secret)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Invalid admin secret"));
        }
        return null;
    }
}
