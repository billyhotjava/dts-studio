package com.yuzhi.dts.copilot.ai.web.rest;

import com.yuzhi.dts.copilot.ai.service.platform.CatalogStatus;
import com.yuzhi.dts.copilot.ai.service.platform.IndicatorCatalogStore;
import com.yuzhi.dts.copilot.ai.service.platform.IndicatorCatalogSyncService;
import com.yuzhi.dts.copilot.ai.service.platform.SyncResult;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.StringUtils;

@RestController
@RequestMapping("/api/copilot/platform-indicators")
public class PlatformIndicatorCatalogResource {

    private final IndicatorCatalogSyncService syncService;
    private final IndicatorCatalogStore store;
    private final String adminSecret;

    public PlatformIndicatorCatalogResource(
            IndicatorCatalogSyncService syncService,
            IndicatorCatalogStore store,
            @Value("${copilot.admin-secret:}") String adminSecret) {
        this.syncService = syncService;
        this.store = store;
        this.adminSecret = adminSecret;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(toStatusResponse(store.status()));
    }

    @PostMapping("/sync")
    public ResponseEntity<Map<String, Object>> sync(@RequestHeader("X-Admin-Secret") String secret) {
        ResponseEntity<Map<String, Object>> authCheck = checkAdminSecret(secret);
        if (authCheck != null) {
            return authCheck;
        }
        SyncResult result = syncService.refresh();
        Map<String, Object> response = toSyncResponse(result);
        response.putAll(toStatusResponse(store.status()));
        return ResponseEntity.ok(response);
    }

    private ResponseEntity<Map<String, Object>> checkAdminSecret(String secret) {
        if (!StringUtils.hasText(adminSecret)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Admin secret not configured"));
        }
        if (!adminSecret.equals(secret)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Invalid admin secret"));
        }
        return null;
    }

    private static Map<String, Object> toSyncResponse(SyncResult result) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", result.status().name());
        response.put("fetched", result.fetched());
        response.put("added", result.added());
        response.put("updated", result.updated());
        response.put("removed", result.removed());
        response.put("caliberChangedCodes", result.caliberChangedCodes());
        response.put("completedAt", result.completedAt());
        response.put("error", result.error());
        return response;
    }

    private static Map<String, Object> toStatusResponse(CatalogStatus status) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("catalogEntryCount", status.entryCount());
        response.put("entryCount", status.entryCount());
        response.put("lastSyncAt", status.lastSyncAt());
        response.put("stale", status.stale());
        response.put("lastStatus", status.lastStatus() == null ? null : status.lastStatus().name());
        response.put("error", status.lastError());
        return response;
    }
}
