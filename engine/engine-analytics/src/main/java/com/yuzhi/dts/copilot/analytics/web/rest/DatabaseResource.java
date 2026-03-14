package com.yuzhi.dts.copilot.analytics.web.rest;

import com.yuzhi.dts.copilot.analytics.domain.AnalyticsDatabase;
import com.yuzhi.dts.copilot.analytics.security.AnalyticsApiKeyAuthFilter;
import com.yuzhi.dts.copilot.analytics.security.ApiKeyAuthService;
import com.yuzhi.dts.copilot.analytics.service.DatabaseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST controller for analytics database management.
 */
@RestController
@RequestMapping("/api/databases")
public class DatabaseResource {

    private final DatabaseService databaseService;

    public DatabaseResource(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    @GetMapping
    public ResponseEntity<List<AnalyticsDatabase>> list() {
        return ResponseEntity.ok(databaseService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AnalyticsDatabase> get(@PathVariable Long id) {
        return databaseService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<AnalyticsDatabase> create(@RequestBody AnalyticsDatabase database) {
        AnalyticsDatabase created = databaseService.create(database);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AnalyticsDatabase> update(@PathVariable Long id,
                                                    @RequestBody AnalyticsDatabase database) {
        try {
            AnalyticsDatabase updated = databaseService.update(id, database);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        databaseService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/sync-metadata")
    public ResponseEntity<List<Map<String, Object>>> syncMetadata(Authentication authentication) {
        String apiKey = extractApiKey(authentication);
        List<Map<String, Object>> metadata = databaseService.syncMetadata(apiKey);
        return ResponseEntity.ok(metadata);
    }

    private String extractApiKey(Authentication authentication) {
        if (authentication instanceof AnalyticsApiKeyAuthFilter.AnalyticsAuthentication analyticsAuth) {
            return analyticsAuth.getAuthenticatedUser().apiKey();
        }
        return "";
    }
}
